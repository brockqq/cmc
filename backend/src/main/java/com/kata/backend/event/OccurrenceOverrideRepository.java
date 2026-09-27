package com.kata.backend.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface OccurrenceOverrideRepository extends JpaRepository<OccurrenceOverride, Long> {

    List<OccurrenceOverride> findByEventIdIn(Collection<Long> eventIds);

    /** Modified occurrences whose (new) times overlap [from, to), wherever the series itself lies. */
    @Query("select o from OccurrenceOverride o join fetch o.event e join fetch e.community c join fetch e.createdBy"
            + " where c.id in :communityIds and o.startAt < :to and coalesce(o.endAt, o.startAt) >= :from")
    List<OccurrenceOverride> findOverlapping(Collection<Long> communityIds, Instant from, Instant to);
}
