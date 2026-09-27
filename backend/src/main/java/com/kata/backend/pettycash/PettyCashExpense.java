package com.kata.backend.pettycash;

import com.kata.backend.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/** A payment made from petty cash, recorded by a committee member. */
@Entity
@Table(name = "petty_cash_expenses", indexes = @Index(columnList = "period_id"))
@Getter
@Setter
@NoArgsConstructor
public class PettyCashExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "period_id")
    private PettyCashPeriod period;

    /** Date on the receipt / date paid. */
    @Column(nullable = false)
    private LocalDate spentOn;

    /** NT$ (whole dollars), always positive. */
    @Column(nullable = false)
    private long amount;

    @Column(nullable = false, length = 200)
    private String purpose;

    @Column(length = 100)
    private String payee;

    @Column(length = 50)
    private String receiptNo;

    @Column(length = 500)
    private String note;

    /** Optional scanned receipt; {@code attachmentKey} is the stored file name. */
    @Column(length = 100)
    private String attachmentKey;

    @Column(length = 255)
    private String attachmentName;

    @Column(length = 100)
    private String attachmentType;

    private Long attachmentSize;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recorded_by")
    private User recordedBy;

    /** Who last changed the expense after it was recorded; null if never changed. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean hasAttachment() {
        return attachmentKey != null;
    }

    public void clearAttachment() {
        attachmentKey = null;
        attachmentName = null;
        attachmentType = null;
        attachmentSize = null;
    }
}
