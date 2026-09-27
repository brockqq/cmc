package com.kata.backend.pettycash;

import com.kata.backend.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Append-only audit trail of everything that changes a community's petty cash. Entries are never updated or
 * deleted; {@code expenseId} is a plain column (no foreign key) so the history of a deleted expense remains.
 */
@Entity
@Table(name = "petty_cash_logs", indexes = @Index(columnList = "period_id"))
@Getter
@Setter
@NoArgsConstructor
public class PettyCashLog {

    public enum Action {
        EXPENSE_CREATED, EXPENSE_UPDATED, EXPENSE_DELETED,
        ATTACHMENT_ADDED, ATTACHMENT_REPLACED, ATTACHMENT_REMOVED,
        FUND_SET, REPLENISHED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The period the change belongs to (for a replenishment: the period it closed). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "period_id", updatable = false)
    private PettyCashPeriod period;

    @Column(updatable = false)
    private Long expenseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private Action action;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", updatable = false)
    private User actor;

    @Column(nullable = false, updatable = false)
    private Instant at;

    /** What it is about, e.g. "文具 NT$ 1,200". */
    @Column(nullable = false, length = 300, updatable = false)
    private String subject;

    /** Field-by-field changes ("金額：1,200 → 1,500"), one per line; null when there is nothing to list. */
    @Column(length = 4000, updatable = false)
    private String changes;

    @PrePersist
    void onCreate() {
        at = Instant.now();
    }
}
