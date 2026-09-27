package com.kata.backend.event;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    /**
     * Loads an event for changing, holding a row lock until the transaction ends: edits of the series and of
     * its single occurrences run one after another, each seeing the other's result (no "ghost" occurrences).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from CalendarEvent e where e.id = :id")
    Optional<CalendarEvent> lockById(Long id);

    /**
     * Events/series in the given communities that may have an occurrence overlapping [from, to).
     * Candidates only — recurring series still need expanding.
     */
    @Query("select distinct e from CalendarEvent e join fetch e.community c join fetch e.createdBy"
            + " left join fetch e.exclusions"
            + " where c.id in :communityIds and e.startAt < :to and (e.lastEndAt is null or e.lastEndAt >= :from)")
    List<CalendarEvent> findCandidates(Collection<Long> communityIds, Instant from, Instant to);
}
