package com.kata.backend.pettycash;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class PettyCashDtos {

    private PettyCashDtos() {
    }

    public static final long MAX_AMOUNT = 10_000_000;

    public record FundRequest(
            @NotNull(message = "請輸入額度")
            @Min(value = 1, message = "額度需大於 0") @Max(value = MAX_AMOUNT, message = "額度過大")
            Long amount
    ) {
    }

    public record ReplenishRequest(@Size(max = 500, message = "備註過長") String note) {
    }

    public record ExpenseRequest(
            @NotNull(message = "請選擇日期") LocalDate spentOn,
            @NotNull(message = "請輸入金額")
            @Min(value = 1, message = "金額需大於 0") @Max(value = MAX_AMOUNT, message = "金額過大")
            Long amount,
            @NotBlank(message = "請輸入用途") @Size(max = 200, message = "用途過長") String purpose,
            @Size(max = 100, message = "付款對象過長") String payee,
            @Size(max = 50, message = "單據號碼過長") String receiptNo,
            @Size(max = 500, message = "備註過長") String note
    ) {
    }

    /**
     * One accounting period. {@code endingBalance} is the balance after this period's expenses
     * (for the open period: the current balance).
     */
    public record PeriodSummary(int number, boolean open, long deposit, String note, long expenseTotal,
                                long expenseCount, long endingBalance, Instant startedAt, String startedBy,
                                Instant closedAt, String closedBy) {
    }

    /**
     * {@code fundAmount} is null until a manager sets it up. {@code nextDeposit} is what "replenish" would put in now.
     */
    public record Summary(Long communityId, String communityName, Long fundAmount, long balance, Long nextDeposit,
                          PeriodSummary currentPeriod, List<PeriodSummary> periods,
                          boolean canRecord, boolean canManage) {
    }

    public record Attachment(String name, String contentType, long size) {
    }

    public record ExpenseResponse(Long id, int periodNumber, LocalDate spentOn, long amount, String purpose,
                                  String payee, String receiptNo, String note, Attachment attachment,
                                  String recordedBy, Instant createdAt, String updatedBy, Instant updatedAt,
                                  boolean canEdit) {

        static ExpenseResponse from(PettyCashExpense e, boolean canEdit) {
            Attachment attachment = e.hasAttachment()
                    ? new Attachment(e.getAttachmentName(), e.getAttachmentType(), e.getAttachmentSize())
                    : null;
            return new ExpenseResponse(e.getId(), e.getPeriod().getNumber(), e.getSpentOn(), e.getAmount(),
                    e.getPurpose(), e.getPayee(), e.getReceiptNo(), e.getNote(), attachment,
                    e.getRecordedBy().getUsername(), e.getCreatedAt(),
                    e.getUpdatedBy() == null ? null : e.getUpdatedBy().getUsername(),
                    e.getUpdatedBy() == null ? null : e.getUpdatedAt(), canEdit);
        }
    }

    /** One audit trail entry; {@code changes} lists "field：before → after" lines. */
    public record LogEntry(Long id, PettyCashLog.Action action, Long expenseId, String subject, List<String> changes,
                           String actor, Instant at) {

        static LogEntry from(PettyCashLog l) {
            List<String> changes = l.getChanges() == null ? List.of() : List.of(l.getChanges().split("\n"));
            return new LogEntry(l.getId(), l.getAction(), l.getExpenseId(), l.getSubject(), changes,
                    l.getActor().getUsername(), l.getAt());
        }
    }
}
