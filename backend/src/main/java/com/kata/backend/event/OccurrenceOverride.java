package com.kata.backend.event;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;

/**
 * A single occurrence of a recurring series that was changed on its own ("edit only this one").
 * <p>
 * Identified by {@code originalStart} — the time the series would have placed it. Times are always stored;
 * title/description/location are null when they follow the series.
 */
@Entity
@Table(name = "calendar_event_overrides",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "original_start"}),
        indexes = @Index(columnList = "start_at"))
@Getter
@Setter
@NoArgsConstructor
public class OccurrenceOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id")
    private CalendarEvent event;

    @Column(name = "original_start", nullable = false)
    private Instant originalStart;

    @Column(nullable = false)
    private Instant startAt;

    private Instant endAt;

    @Column(length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(length = 200)
    private String location;

    OccurrenceOverride(CalendarEvent event, Instant originalStart) {
        this.event = event;
        this.originalStart = originalStart;
    }

    public Duration duration() {
        return endAt == null ? Duration.ZERO : Duration.between(startAt, endAt);
    }
}
