package com.kata.backend.pettycash;

import com.kata.backend.community.Community;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A community's petty cash (imprest fund). {@code amount} is the target level each period is replenished to;
 * the balance itself is derived from period deposits minus expenses and may go negative.
 */
@Entity
@Table(name = "petty_cash_funds")
@Getter
@Setter
@NoArgsConstructor
public class PettyCashFund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "community_id", unique = true)
    private Community community;

    /** Fund level in NT$ (whole dollars). */
    @Column(nullable = false)
    private long amount;
}
