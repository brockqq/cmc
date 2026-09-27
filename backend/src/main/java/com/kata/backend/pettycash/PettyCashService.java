package com.kata.backend.pettycash;

import com.kata.backend.common.ApiException;
import com.kata.backend.community.Community;
import com.kata.backend.community.CommunityAccess;
import com.kata.backend.community.CommunityRole;
import com.kata.backend.pettycash.PettyCashDtos.*;
import com.kata.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Petty cash (零用金) per community, run as an imprest fund:
 * <ul>
 *   <li>a manager sets the fund level; the first period opens with that amount</li>
 *   <li>committee members (行政委員) record what they pay out; the balance may go negative</li>
 *   <li>a manager closes the period by replenishing: deposit = fund level − balance,
 *       so the next period starts at the fund level again (overspending is made good)</li>
 * </ul>
 * Only the open period can be changed. Managers, committee members and platform admins can view.
 * Every change is written to an append-only audit trail ({@link PettyCashLog}).
 */
@Service
@RequiredArgsConstructor
public class PettyCashService {

    private static final CommunityRole[] VIEWERS = {CommunityRole.MANAGER, CommunityRole.COMMITTEE};

    private final PettyCashFundRepository fundRepository;
    private final PettyCashPeriodRepository periodRepository;
    private final PettyCashExpenseRepository expenseRepository;
    private final PettyCashLogRepository logRepository;
    private final CommunityAccess access;
    private final ReceiptStorage storage;

    // ---- Reading ----

    @Transactional(readOnly = true)
    public Summary summary(Long communityId, String username) {
        CommunityAccess.Context ctx = requireViewer(communityId, username);
        return fundRepository.findByCommunityId(communityId)
                .map(fund -> summarize(ctx, fund))
                .orElseGet(() -> new Summary(communityId, ctx.community().getName(), null, 0, null, null,
                        List.of(), false, canManage(ctx)));
    }

    /** Expenses of one period (default: the open one). */
    @Transactional(readOnly = true)
    public List<ExpenseResponse> expenses(Long communityId, String username, Integer periodNumber) {
        CommunityAccess.Context ctx = requireViewer(communityId, username);
        PettyCashFund fund = fundRepository.findByCommunityId(communityId).orElse(null);
        if (fund == null) {
            return List.of();
        }
        PettyCashPeriod period = findPeriod(fund, periodNumber);
        boolean editable = period.isOpen() && canRecord(ctx);
        return expenseRepository.findByPeriod(period.getId()).stream()
                .map(e -> ExpenseResponse.from(e, editable))
                .toList();
    }

    /** Audit trail of one period (default: the open one), newest first. */
    @Transactional(readOnly = true)
    public List<LogEntry> log(Long communityId, String username, Integer periodNumber) {
        requireViewer(communityId, username);
        PettyCashFund fund = fundRepository.findByCommunityId(communityId).orElse(null);
        if (fund == null) {
            return List.of();
        }
        return logRepository.findByPeriodNewestFirst(findPeriod(fund, periodNumber).getId()).stream()
                .map(LogEntry::from)
                .toList();
    }

    public record Download(Resource resource, String contentType, String fileName) {
    }

    @Transactional(readOnly = true)
    public Download attachment(Long expenseId, String username) {
        PettyCashExpense e = findExpense(expenseId);
        requireViewer(communityOf(e).getId(), username);
        if (!e.hasAttachment()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "這筆支出沒有單據檔案");
        }
        return new Download(storage.load(e.getAttachmentKey()), e.getAttachmentType(), e.getAttachmentName());
    }

    // ---- Managers: fund level and replenishment ----

    /** Sets the fund level. The first time, this opens period 1 funded with that amount. */
    @Transactional
    public Summary setFundAmount(Long communityId, String username, long amount) {
        CommunityAccess.Context ctx = requireManager(communityId, username);
        PettyCashFund fund = fundRepository.lockByCommunityId(communityId).orElse(null);
        if (fund == null) {
            fund = new PettyCashFund();
            fund.setCommunity(ctx.community());
            fund.setAmount(amount);
            fundRepository.save(fund);
            PettyCashPeriod first = openPeriod(fund, 1, amount, "初始撥補", ctx.user());
            log(first, null, PettyCashLog.Action.FUND_SET, ctx.user(), "設定每期額度 " + money(amount), null);
        } else if (fund.getAmount() != amount) {
            // Takes effect at the next replenishment
            log(openPeriod(fund), null, PettyCashLog.Action.FUND_SET, ctx.user(), "調整每期額度",
                    "額度：" + money(fund.getAmount()) + " → " + money(amount));
            fund.setAmount(amount);
        }
        return summarize(ctx, fund);
    }

    /** Closes the open period and starts the next one topped back up to the fund level. */
    @Transactional
    public Summary replenish(Long communityId, String username, String note) {
        CommunityAccess.Context ctx = requireManager(communityId, username);
        PettyCashFund fund = lockFund(communityId);
        PettyCashPeriod current = openPeriod(fund);
        long deposit = fund.getAmount() - balance(fund);
        // Nothing to settle: no expenses and nothing to top up (a changed fund level alone still allows it)
        if (deposit == 0 && !expenseRepository.existsByPeriodId(current.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "本期沒有任何支出，餘額也等於額度，不需要結算撥補");
        }
        current.setClosedAt(Instant.now());
        current.setClosedBy(ctx.user());
        periodRepository.saveAndFlush(current); // close before opening the next (one open period per fund)
        openPeriod(fund, current.getNumber() + 1, deposit, blankToNull(note), ctx.user());
        log(current, null, PettyCashLog.Action.REPLENISHED, ctx.user(),
                "結算第 " + current.getNumber() + " 期並撥補 " + money(deposit),
                blankToNull(note) == null ? null : "備註：" + note.trim());
        return summarize(ctx, fund);
    }

    // ---- Committee: expenses ----

    @Transactional
    public ExpenseResponse createExpense(Long communityId, String username, ExpenseRequest request) {
        CommunityAccess.Context ctx = requireRecorder(communityId, username);
        PettyCashFund fund = lockFund(communityId);
        PettyCashExpense e = new PettyCashExpense();
        e.setPeriod(openPeriod(fund));
        e.setRecordedBy(ctx.user());
        apply(e, request);
        expenseRepository.save(e);
        log(e.getPeriod(), e.getId(), PettyCashLog.Action.EXPENSE_CREATED, ctx.user(), subjectOf(e), details(e));
        return ExpenseResponse.from(e, true);
    }

    @Transactional
    public ExpenseResponse updateExpense(Long expenseId, String username, ExpenseRequest request) {
        Editing edit = findEditableExpense(expenseId, username);
        PettyCashExpense e = edit.expense();
        List<String> before = fieldValues(e);
        String subject = subjectOf(e);
        apply(e, request);
        String changes = diff(before, fieldValues(e));
        if (changes != null) {
            e.setUpdatedBy(edit.user());
            log(e.getPeriod(), e.getId(), PettyCashLog.Action.EXPENSE_UPDATED, edit.user(), subject, changes);
        }
        expenseRepository.flush();
        return ExpenseResponse.from(e, true);
    }

    @Transactional
    public void deleteExpense(Long expenseId, String username) {
        Editing edit = findEditableExpense(expenseId, username);
        PettyCashExpense e = edit.expense();
        String key = e.getAttachmentKey();
        log(e.getPeriod(), e.getId(), PettyCashLog.Action.EXPENSE_DELETED, edit.user(), subjectOf(e), details(e));
        expenseRepository.delete(e);
        deleteFileAfterCommit(key);
    }

    @Transactional
    public ExpenseResponse attach(Long expenseId, String username, MultipartFile file) {
        Editing edit = findEditableExpense(expenseId, username);
        PettyCashExpense e = edit.expense();
        ReceiptStorage.Stored stored = storage.store(file);
        String previous = e.getAttachmentKey();
        String previousName = e.getAttachmentName();
        e.setAttachmentKey(stored.key());
        e.setAttachmentType(stored.contentType());
        e.setAttachmentSize(stored.size());
        e.setAttachmentName(safeFileName(file.getOriginalFilename()));
        e.setUpdatedBy(edit.user());
        log(e.getPeriod(), e.getId(),
                previous == null ? PettyCashLog.Action.ATTACHMENT_ADDED : PettyCashLog.Action.ATTACHMENT_REPLACED,
                edit.user(), subjectOf(e),
                "單據檔案：" + (previousName == null ? EMPTY : previousName) + " → " + e.getAttachmentName());
        deleteFileAfterCommit(previous);
        // If this transaction fails, the newly written file must not be left behind
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    storage.delete(stored.key());
                }
            }
        });
        expenseRepository.flush();
        return ExpenseResponse.from(e, true);
    }

    @Transactional
    public ExpenseResponse removeAttachment(Long expenseId, String username) {
        Editing edit = findEditableExpense(expenseId, username);
        PettyCashExpense e = edit.expense();
        if (!e.hasAttachment()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "這筆支出沒有單據檔案");
        }
        log(e.getPeriod(), e.getId(), PettyCashLog.Action.ATTACHMENT_REMOVED, edit.user(), subjectOf(e),
                "單據檔案：" + e.getAttachmentName() + " → " + EMPTY);
        deleteFileAfterCommit(e.getAttachmentKey());
        e.clearAttachment();
        e.setUpdatedBy(edit.user());
        expenseRepository.flush();
        return ExpenseResponse.from(e, true);
    }

    // ---- Helpers ----

    private Summary summarize(CommunityAccess.Context ctx, PettyCashFund fund) {
        List<PettyCashPeriod> periods = periodRepository.findByFund(fund.getId());
        Map<Long, Object[]> totals = expenseRepository.totalsByPeriod(fund.getId()).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> row));

        List<PeriodSummary> summaries = new ArrayList<>();
        long running = 0;
        for (PettyCashPeriod p : periods) { // oldest first, to carry the running balance
            Object[] t = totals.get(p.getId());
            long spent = t == null ? 0 : ((Number) t[1]).longValue();
            long count = t == null ? 0 : ((Number) t[2]).longValue();
            running += p.getDeposit() - spent;
            summaries.add(new PeriodSummary(p.getNumber(), p.isOpen(), p.getDeposit(), p.getNote(), spent, count,
                    running, p.getStartedAt(), p.getStartedBy().getUsername(), p.getClosedAt(),
                    p.getClosedBy() == null ? null : p.getClosedBy().getUsername()));
        }
        PeriodSummary current = summaries.isEmpty() ? null : summaries.getLast();
        long balance = running;
        List<PeriodSummary> newestFirst = new ArrayList<>(summaries.reversed());
        return new Summary(ctx.community().getId(), ctx.community().getName(), fund.getAmount(), balance,
                fund.getAmount() - balance, current, newestFirst, canRecord(ctx), canManage(ctx));
    }

    private long balance(PettyCashFund fund) {
        long deposits = periodRepository.findByFund(fund.getId()).stream().mapToLong(PettyCashPeriod::getDeposit).sum();
        long spent = expenseRepository.totalsByPeriod(fund.getId()).stream()
                .mapToLong(row -> ((Number) row[1]).longValue()).sum();
        return deposits - spent;
    }

    private PettyCashPeriod openPeriod(PettyCashFund fund, int number, long deposit, String note, User by) {
        PettyCashPeriod p = new PettyCashPeriod();
        p.setFund(fund);
        p.setNumber(number);
        p.setDeposit(deposit);
        p.setNote(note);
        p.setStartedAt(Instant.now());
        p.setStartedBy(by);
        return periodRepository.save(p);
    }

    private PettyCashPeriod findPeriod(PettyCashFund fund, Integer number) {
        return number == null
                ? openPeriod(fund)
                : periodRepository.findByFundIdAndNumber(fund.getId(), number)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "找不到第 " + number + " 期"));
    }

    private PettyCashPeriod openPeriod(PettyCashFund fund) {
        return periodRepository.findByFundIdAndClosedAtIsNull(fund.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "目前沒有進行中的期別"));
    }

    private PettyCashFund lockFund(Long communityId) {
        return fundRepository.lockByCommunityId(communityId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "尚未設定零用金額度，請社區管理員先設定"));
    }

    private PettyCashExpense findExpense(Long expenseId) {
        return expenseRepository.findWithCommunity(expenseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "找不到此筆支出"));
    }

    /**
     * Loads an expense for changing, holding the fund lock so that a concurrent replenishment cannot close its
     * period in between. The expense is read only after the lock is taken, so its period state is current.
     */
    private Editing findEditableExpense(Long expenseId, String username) {
        Long communityId = expenseRepository.findCommunityId(expenseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "找不到此筆支出"));
        User user = requireRecorder(communityId, username).user();
        lockFund(communityId);
        PettyCashExpense e = findExpense(expenseId);
        if (!e.getPeriod().isOpen()) {
            throw new ApiException(HttpStatus.CONFLICT, "第 " + e.getPeriod().getNumber() + " 期已結算，無法修改");
        }
        return new Editing(e, user);
    }

    private record Editing(PettyCashExpense expense, User user) {
    }

    // ---- Audit trail ----

    private static final String EMPTY = "（無）";
    private static final List<String> FIELD_LABELS = List.of("日期", "金額", "用途", "付款對象", "單據號碼", "備註");

    private void log(PettyCashPeriod period, Long expenseId, PettyCashLog.Action action, User actor,
                     String subject, String changes) {
        PettyCashLog l = new PettyCashLog();
        l.setPeriod(period);
        l.setExpenseId(expenseId);
        l.setAction(action);
        l.setActor(actor);
        l.setSubject(truncate(subject, 300));
        l.setChanges(changes == null ? null : truncate(changes, 4000));
        logRepository.save(l);
    }

    private static String subjectOf(PettyCashExpense e) {
        return e.getPurpose() + " " + money(e.getAmount());
    }

    /** Display values in {@link #FIELD_LABELS} order (null = empty). */
    private static List<String> fieldValues(PettyCashExpense e) {
        return Arrays.asList(e.getSpentOn().toString(), money(e.getAmount()), e.getPurpose(),
                e.getPayee(), e.getReceiptNo(), e.getNote());
    }

    /** Every filled-in field, for a created or deleted expense. */
    private static String details(PettyCashExpense e) {
        List<String> values = fieldValues(e);
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i) != null) {
                lines.add(FIELD_LABELS.get(i) + "：" + oneLine(values.get(i)));
            }
        }
        if (e.hasAttachment()) {
            lines.add("單據檔案：" + e.getAttachmentName());
        }
        return String.join("\n", lines);
    }

    /** "field：before → after" for each changed field, or null when nothing changed. */
    private static String diff(List<String> before, List<String> after) {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < before.size(); i++) {
            if (!Objects.equals(before.get(i), after.get(i))) {
                lines.add(FIELD_LABELS.get(i) + "：" + oneLine(before.get(i)) + " → " + oneLine(after.get(i)));
            }
        }
        return lines.isEmpty() ? null : String.join("\n", lines);
    }

    /** Entries are stored one change per line, so values must not contain line breaks. */
    private static String oneLine(String s) {
        return s == null ? EMPTY : s.replaceAll("[\\r\\n]+", " ");
    }

    static String money(long amount) {
        return (amount < 0 ? "-" : "") + "NT$ " + String.format("%,d", Math.abs(amount));
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private static Community communityOf(PettyCashExpense e) {
        return e.getPeriod().getFund().getCommunity();
    }

    private CommunityAccess.Context requireViewer(Long communityId, String username) {
        return access.requireAnyRole(communityId, username, "只有社區管理員與行政委員可以查看零用金", VIEWERS);
    }

    private CommunityAccess.Context requireManager(Long communityId, String username) {
        return access.requireAnyRole(communityId, username, "只有社區管理員可以設定額度與撥補", CommunityRole.MANAGER);
    }

    private CommunityAccess.Context requireRecorder(Long communityId, String username) {
        return access.requireAnyRole(communityId, username, "只有行政委員可以登錄支出", CommunityRole.COMMITTEE);
    }

    private static boolean canManage(CommunityAccess.Context ctx) {
        return ctx.isPlatformAdmin() || ctx.role() == CommunityRole.MANAGER;
    }

    private static boolean canRecord(CommunityAccess.Context ctx) {
        return ctx.isPlatformAdmin() || ctx.role() == CommunityRole.COMMITTEE;
    }

    private void deleteFileAfterCommit(String key) {
        if (key == null) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                storage.delete(key);
            }
        });
    }

    private static void apply(PettyCashExpense e, ExpenseRequest r) {
        e.setSpentOn(r.spentOn());
        e.setAmount(r.amount());
        e.setPurpose(r.purpose().trim());
        e.setPayee(blankToNull(r.payee()));
        e.setReceiptNo(blankToNull(r.receiptNo()));
        e.setNote(blankToNull(r.note()));
    }

    /** Keeps only the base name, for display and Content-Disposition. */
    private static String safeFileName(String original) {
        if (original == null || original.isBlank()) {
            return "receipt";
        }
        String name = original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\r\\n\"]", "_");
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
