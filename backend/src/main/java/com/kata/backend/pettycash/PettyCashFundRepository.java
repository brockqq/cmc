package com.kata.backend.pettycash;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PettyCashFundRepository extends JpaRepository<PettyCashFund, Long> {

    Optional<PettyCashFund> findByCommunityId(Long communityId);

    /** Serializes balance-changing operations (recording, replenishing) per fund. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from PettyCashFund f where f.community.id = :communityId")
    Optional<PettyCashFund> lockByCommunityId(Long communityId);
}
