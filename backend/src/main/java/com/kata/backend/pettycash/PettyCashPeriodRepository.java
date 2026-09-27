package com.kata.backend.pettycash;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PettyCashPeriodRepository extends JpaRepository<PettyCashPeriod, Long> {

    @Query("select p from PettyCashPeriod p join fetch p.startedBy left join fetch p.closedBy"
            + " where p.fund.id = :fundId order by p.number")
    List<PettyCashPeriod> findByFund(Long fundId);

    Optional<PettyCashPeriod> findByFundIdAndClosedAtIsNull(Long fundId);

    Optional<PettyCashPeriod> findByFundIdAndNumber(Long fundId, int number);
}
