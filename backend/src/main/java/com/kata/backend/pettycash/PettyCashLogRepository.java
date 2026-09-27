package com.kata.backend.pettycash;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PettyCashLogRepository extends JpaRepository<PettyCashLog, Long> {

    @Query("select l from PettyCashLog l join fetch l.actor where l.period.id = :periodId order by l.id desc")
    List<PettyCashLog> findByPeriodNewestFirst(Long periodId);
}
