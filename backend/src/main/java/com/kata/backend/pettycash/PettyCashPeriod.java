package com.kata.backend.pettycash;

import com.kata.backend.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * One accounting period of a fund. It opens with a {@code deposit} (the replenishment that brings the balance
 * back to the fund level) and is closed by the next replenishment. Only the open period accepts changes.
 */
@Entity
@Table(name = "petty_cash_periods", uniqueConstraints = @UniqueConstraint(columnNames = {"fund_id", "number"}))
@Getter
@Setter
@NoArgsConstructor
public class PettyCashPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fund_id")
    private PettyCashFund fund;

    /** 1, 2, 3 … within the fund. */
    @Column(nullable = false)
    private int number;

    /** Money put in when the period opened (negative = excess returned after the fund level was lowered). */
    @Column(nullable = false)
    private long deposit;

    @Column(length = 500)
    private String note;

    @Column(nullable = false)
    private Instant startedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "started_by")
    private User startedBy;

    private Instant closedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by")
    private User closedBy;

    public boolean isOpen() {
        return closedAt == null;
    }
}
