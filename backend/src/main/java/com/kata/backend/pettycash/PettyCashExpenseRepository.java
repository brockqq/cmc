package com.kata.backend.pettycash;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PettyCashExpenseRepository extends JpaRepository<PettyCashExpense, Long> {

    @Query("select e from PettyCashExpense e join fetch e.recordedBy left join fetch e.updatedBy"
            + " where e.period.id = :periodId order by e.spentOn, e.id")
    List<PettyCashExpense> findByPeriod(Long periodId);

    /** [periodId, sum(amount), count] for every period of the fund that has expenses. */
    @Query("select e.period.id, sum(e.amount), count(e) from PettyCashExpense e"
            + " where e.period.fund.id = :fundId group by e.period.id")
    List<Object[]> totalsByPeriod(Long fundId);

    boolean existsByPeriodId(Long periodId);

    @Query("select e.period.fund.community.id from PettyCashExpense e where e.id = :id")
    Optional<Long> findCommunityId(Long id);

    @Query("select e from PettyCashExpense e join fetch e.period p join fetch p.fund f join fetch f.community"
            + " where e.id = :id")
    Optional<PettyCashExpense> findWithCommunity(Long id);
}
