package com.kata.backend.pettycash;

import com.kata.backend.ApiTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PettyCashTests extends ApiTestSupport {

    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'};

    @Autowired
    PettyCashService pettyCash;

    @Autowired
    TransactionTemplate transactions;

    String suffix;
    String admin;
    String mgr;
    String committee;
    String resident;
    Created a;
    Created b;

    @BeforeEach
    void setUp() throws Exception {
        suffix = String.valueOf(System.nanoTime()).substring(8);
        admin = adminToken();
        a = createCommunity(admin, "零用金A" + suffix);
        b = createCommunity(admin, "零用金B" + suffix);
        register("pmgr" + suffix, null);
        makeManager(admin, a, "pmgr" + suffix);
        String committeeId = register("pcom" + suffix, a.inviteCode());
        register("pres" + suffix, a.inviteCode());
        mgr = login("pmgr" + suffix);
        committee = login("pcom" + suffix);
        resident = login("pres" + suffix);

        // The manager appoints the committee member through the regular member-role endpoint
        call(mgr, put("/api/communities/{id}/members/{uid}/role", a.id(), committeeId), "{\"role\":\"COMMITTEE\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("COMMITTEE"));
    }

    @Test
    void imprestCycleAllowsOverspendingAndReplenishesBackToTheFundLevel() throws Exception {
        // Before setup: nothing to spend from
        expense(committee, 100, "文具").andExpect(status().isBadRequest());
        call(committee, get("/api/communities/{id}/petty-cash", a.id()), null)
                .andExpect(jsonPath("$.fundAmount").doesNotExist())
                .andExpect(jsonPath("$.canRecord").value(false));

        call(mgr, put("/api/communities/{id}/petty-cash/fund", a.id()), "{\"amount\":5000}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(5000))
                .andExpect(jsonPath("$.currentPeriod.number").value(1))
                .andExpect(jsonPath("$.currentPeriod.deposit").value(5000));

        expense(committee, 1200, "文具").andExpect(status().isCreated());
        expense(committee, 4500, "修繕中庭燈具").andExpect(status().isCreated());

        // Overspent: the balance goes negative, and the next deposit will cover it
        call(committee, get("/api/communities/{id}/petty-cash", a.id()), null)
                .andExpect(jsonPath("$.balance").value(-700))
                .andExpect(jsonPath("$.nextDeposit").value(5700))
                .andExpect(jsonPath("$.currentPeriod.expenseTotal").value(5700))
                .andExpect(jsonPath("$.currentPeriod.expenseCount").value(2))
                .andExpect(jsonPath("$.canRecord").value(true))
                .andExpect(jsonPath("$.canManage").value(false));

        call(mgr, post("/api/communities/{id}/petty-cash/replenish", a.id()), "{\"note\":\"10月撥補\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(5000))
                .andExpect(jsonPath("$.currentPeriod.number").value(2))
                .andExpect(jsonPath("$.currentPeriod.deposit").value(5700))
                .andExpect(jsonPath("$.currentPeriod.note").value("10月撥補"))
                // History, newest first: period 1 closed at -700
                .andExpect(jsonPath("$.periods[1].number").value(1))
                .andExpect(jsonPath("$.periods[1].open").value(false))
                .andExpect(jsonPath("$.periods[1].endingBalance").value(-700))
                .andExpect(jsonPath("$.periods[1].closedBy").value("pmgr" + suffix));

        // New expenses go to period 2; period 1 keeps its two
        expense(committee, 300, "清潔用品").andExpect(jsonPath("$.periodNumber").value(2));
        call(mgr, get("/api/communities/{id}/petty-cash/expenses", a.id()).param("period", "1"), null)
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].canEdit").value(false));
        call(committee, get("/api/communities/{id}/petty-cash/expenses", a.id()), null)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].canEdit").value(true));
    }

    @Test
    void changingTheFundLevelTakesEffectAtTheNextReplenishment() throws Exception {
        call(mgr, put("/api/communities/{id}/petty-cash/fund", a.id()), "{\"amount\":5000}");
        expense(committee, 1000, "茶水");
        call(mgr, put("/api/communities/{id}/petty-cash/fund", a.id()), "{\"amount\":8000}")
                .andExpect(jsonPath("$.fundAmount").value(8000))
                .andExpect(jsonPath("$.balance").value(4000))
                .andExpect(jsonPath("$.nextDeposit").value(4000));
        call(mgr, post("/api/communities/{id}/petty-cash/replenish", a.id()), null)
                .andExpect(jsonPath("$.balance").value(8000));
    }

    @Test
    void anEmptyPeriodNeedsNoReplenishment() throws Exception {
        call(mgr, put("/api/communities/{id}/petty-cash/fund", a.id()), "{\"amount\":3000}");
        call(mgr, post("/api/communities/{id}/petty-cash/replenish", a.id()), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("本期沒有任何支出，餘額也等於額度，不需要結算撥補"));

        // A new fund level still has to be topped up (or paid back), even without expenses
        call(mgr, put("/api/communities/{id}/petty-cash/fund", a.id()), "{\"amount\":3500}");
        call(mgr, post("/api/communities/{id}/petty-cash/replenish", a.id()), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPeriod.deposit").value(500));
    }

    @Test
    void closedPeriodsAreLocked() throws Exception {
        call(mgr, put("/api/communities/{id}/petty-cash/fund", a.id()), "{\"amount\":3000}");
        String id = idOf(expense(committee, 500, "郵資"));
        call(mgr, post("/api/communities/{id}/petty-cash/replenish", a.id()), null);

        call(committee, put("/api/petty-cash/expenses/{id}", id), expenseJson(600, "郵資"))
                .andExpect(status().isConflict());
        call(committee, delete("/api/petty-cash/expenses/{id}", id), null).andExpect(status().isConflict());
    }

    @Test
    void everyChangeIsKeptInTheAuditTrailIncludingDeletedExpenses() throws Exception {
        call(mgr, put("/api/communities/{id}/petty-cash/fund", a.id()), "{\"amount\":3000}");
        String keep = idOf(expense(committee, 1200, "文具"));
        String gone = idOf(expense(committee, 80, "郵資"));

        // Someone else (here the platform admin) changes the committee member's expense
        call(admin, put("/api/petty-cash/expenses/{id}", keep), """
                {"spentOn":"%s","amount":1500,"purpose":"影印紙","payee":"五金行","receiptNo":"AB-12345678"}"""
                .formatted(LocalDate.now()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordedBy").value("pcom" + suffix))
                .andExpect(jsonPath("$.updatedBy").value("admin"));
        // Saving without changes is not an edit
        call(committee, put("/api/petty-cash/expenses/{id}", gone), expenseJson(80, "郵資"))
                .andExpect(jsonPath("$.updatedBy").doesNotExist());
        call(committee, delete("/api/petty-cash/expenses/{id}", gone), null).andExpect(status().isNoContent());
        call(mgr, put("/api/communities/{id}/petty-cash/fund", a.id()), "{\"amount\":4000}");

        call(mgr, get("/api/communities/{id}/petty-cash/log", a.id()), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].action").value("FUND_SET"))
                .andExpect(jsonPath("$[0].changes[0]").value("額度：NT$ 3,000 → NT$ 4,000"))
                .andExpect(jsonPath("$[1].action").value("EXPENSE_DELETED"))
                .andExpect(jsonPath("$[1].expenseId").value(Integer.valueOf(gone)))
                .andExpect(jsonPath("$[1].subject").value("郵資 NT$ 80"))
                .andExpect(jsonPath("$[1].actor").value("pcom" + suffix))
                .andExpect(jsonPath("$[2].action").value("EXPENSE_UPDATED"))
                .andExpect(jsonPath("$[2].actor").value("admin"))
                .andExpect(jsonPath("$[2].subject").value("文具 NT$ 1,200"))
                .andExpect(jsonPath("$[2].changes[0]").value("金額：NT$ 1,200 → NT$ 1,500"))
                .andExpect(jsonPath("$[2].changes[1]").value("用途：文具 → 影印紙"))
                .andExpect(jsonPath("$[3].action").value("EXPENSE_CREATED"))
                .andExpect(jsonPath("$[5].action").value("FUND_SET"));

        // The replenishment is logged on the period it closed; the new period starts empty
        call(mgr, post("/api/communities/{id}/petty-cash/replenish", a.id()), "{\"note\":\"十月\"}");
        call(mgr, get("/api/communities/{id}/petty-cash/log?period=1", a.id()), null)
                .andExpect(jsonPath("$[0].action").value("REPLENISHED"))
                .andExpect(jsonPath("$[0].subject").value("結算第 1 期並撥補 NT$ 2,500"))
                .andExpect(jsonPath("$[0].changes[0]").value("備註：十月"));
        call(mgr, get("/api/communities/{id}/petty-cash/log", a.id()), null)
                .andExpect(jsonPath("$.length()").value(0));

        call(resident, get("/api/communities/{id}/petty-cash/log", a.id()), null).andExpect(status().isForbidden());
    }

    @Test
    void editingAnExpenseWaitsForAConcurrentReplenishmentAndThenSeesThePeriodClosed() throws Exception {
        call(mgr, put("/api/communities/{id}/petty-cash/fund", a.id()), "{\"amount\":3000}");
        String id = idOf(expense(committee, 500, "郵資"));

        CountDownLatch replenished = new CountDownLatch(1);
        CountDownLatch commit = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            // The manager's replenishment has closed period 1 but not yet committed
            Future<?> manager = pool.submit(() -> transactions.executeWithoutResult(tx -> {
                pettyCash.replenish(Long.valueOf(a.id()), "pmgr" + suffix, null);
                replenished.countDown();
                await(commit);
            }));
            assertThat(replenished.await(5, TimeUnit.SECONDS)).isTrue();

            Future<Integer> edit = pool.submit(() -> call(committee, put("/api/petty-cash/expenses/{id}", id),
                    expenseJson(600, "郵資")).andReturn().getResponse().getStatus());
            Thread.sleep(300);
            assertThat(edit.isDone()).as("the edit must wait for the fund lock").isFalse();

            commit.countDown();
            manager.get(5, TimeUnit.SECONDS);
            assertThat(edit.get(5, TimeUnit.SECONDS)).isEqualTo(409);
        } finally {
            commit.countDown();
            pool.shutdownNow();
        }
        call(committee, get("/api/communities/{id}/petty-cash/expenses?period=1", a.id()), null)
                .andExpect(jsonPath("$[0].amount").value(500));
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void rolesAreSeparated() throws Exception {
        // Only managers set the level / replenish; only committee members record expenses
        call(committee, put("/api/communities/{id}/petty-cash/fund", a.id()), "{\"amount\":5000}")
                .andExpect(status().isForbidden());
        call(mgr, put("/api/communities/{id}/petty-cash/fund", a.id()), "{\"amount\":5000}").andExpect(status().isOk());
        call(committee, post("/api/communities/{id}/petty-cash/replenish", a.id()), null)
                .andExpect(status().isForbidden());
        expense(mgr, 100, "x").andExpect(status().isForbidden());

        // Residents see nothing at all
        call(resident, get("/api/communities/{id}/petty-cash", a.id()), null).andExpect(status().isForbidden());
        call(resident, get("/api/communities/{id}/petty-cash/expenses", a.id()), null).andExpect(status().isForbidden());
        expense(resident, 100, "x").andExpect(status().isForbidden());

        // Committee of A has no say in community B
        call(committee, get("/api/communities/{id}/petty-cash", b.id()), null).andExpect(status().isNotFound());

        // Platform admins can do everything
        expense(admin, 100, "平台代登").andExpect(status().isCreated());

        // Validation
        call(committee, post("/api/communities/{id}/petty-cash/expenses", a.id()), """
                {"spentOn":"2026-10-01","amount":0,"purpose":""}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists())
                .andExpect(jsonPath("$.errors.purpose").exists());
        call(mgr, put("/api/communities/{id}/petty-cash/fund", a.id()), "{\"amount\":-1}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void receiptFilesCanBeAttachedViewedAndRemoved() throws Exception {
        call(mgr, put("/api/communities/{id}/petty-cash/fund", a.id()), "{\"amount\":5000}");
        String id = idOf(expense(committee, 350, "影印"));

        mvc.perform(multipart("/api/petty-cash/expenses/{id}/attachment", id)
                        .file(new MockMultipartFile("file", "發票 001.png", "image/png", PNG))
                        .header("Authorization", "Bearer " + committee))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attachment.name").value("發票 001.png"))
                .andExpect(jsonPath("$.attachment.contentType").value("image/png"));

        byte[] downloaded = call(mgr, get("/api/petty-cash/expenses/{id}/attachment", id), null)
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Security-Policy", org.hamcrest.Matchers.startsWith("default-src 'none'")))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(downloaded).isEqualTo(PNG);
        call(resident, get("/api/petty-cash/expenses/{id}/attachment", id), null).andExpect(status().isForbidden());

        // The real type is checked, not the declared one
        mvc.perform(multipart("/api/petty-cash/expenses/{id}/attachment", id)
                        .file(new MockMultipartFile("file", "evil.png", "image/png", "<script>".getBytes()))
                        .header("Authorization", "Bearer " + committee))
                .andExpect(status().isBadRequest());

        call(committee, delete("/api/petty-cash/expenses/{id}/attachment", id), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attachment").doesNotExist());
        call(mgr, get("/api/petty-cash/expenses/{id}/attachment", id), null).andExpect(status().isNotFound());
    }

    @Test
    void sniffsAllowedFileTypes() {
        assertThat(ReceiptStorage.sniff(PNG)).isEqualTo("image/png");
        assertThat(ReceiptStorage.sniff(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0})).isEqualTo("image/jpeg");
        assertThat(ReceiptStorage.sniff("%PDF-1.7".getBytes())).isEqualTo("application/pdf");
        assertThat(ReceiptStorage.sniff("<html>".getBytes())).isNull();
        assertThat(ReceiptStorage.sniff(new byte[0])).isNull();
    }

    private org.springframework.test.web.servlet.ResultActions expense(String token, long amount, String purpose)
            throws Exception {
        return call(token, post("/api/communities/{id}/petty-cash/expenses", a.id()), expenseJson(amount, purpose));
    }

    private static String expenseJson(long amount, String purpose) {
        return """
                {"spentOn":"%s","amount":%d,"purpose":"%s","payee":"五金行","receiptNo":"AB-12345678"}"""
                .formatted(LocalDate.now(), amount, purpose);
    }
}
