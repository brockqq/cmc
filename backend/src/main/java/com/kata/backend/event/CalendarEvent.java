package com.kata.backend.event;

import com.kata.backend.community.Community;
import com.kata.backend.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * A one-off event or a recurring series. For a series, {@code startAt}/{@code endAt} describe the first
 * occurrence; later occurrences are computed by {@link RecurrenceExpander}.
 */
@Entity
@Table(name = "calendar_events", indexes = {
        @Index(columnList = "community_id, start_at"),
        @Index(columnList = "last_end_at")})
@Getter
@Setter
@NoArgsConstructor
public class CalendarEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "community_id")
    private Community community;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(length = 200)
    private String location;

    @Column(nullable = false)
    private Instant startAt;

    /** Optional; a missing end means a point-in-time event. */
    private Instant endAt;

    /** IANA zone the recurrence is computed in, e.g. "Asia/Taipei". */
    @Column(nullable = false, length = 40)
    private String timeZone;

    /** Null for one-off events. */
    @Embedded
    private Recurrence recurrence;

    /**
     * End of the last occurrence (derived, used to query by date range); null when the series never ends.
     */
    private Instant lastEndAt;

    /** Start instants of occurrences cancelled individually. */
    @ElementCollection
    @CollectionTable(name = "calendar_event_exclusions", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "occurrence_start", nullable = false)
    private Set<Instant> exclusions = new HashSet<>();

    /** Occurrences edited individually. */
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OccurrenceOverride> overrides = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Bumped by every change to the series or any of its occurrences, so that an edit based on what the user saw
     * earlier can be refused when someone else changed the event in between (clients send it back).
     */
    @Version
    private long version;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    /**
     * Marks the event changed. Needed for changes that only touch occurrence overrides (a separate table), so
     * that {@link #version} still moves on.
     */
    public void touch() {
        updatedAt = Instant.now();
    }

    public boolean isRecurring() {
        return recurrence != null;
    }

    public Duration duration() {
        return endAt == null ? Duration.ZERO : Duration.between(startAt, endAt);
    }

    public ZoneId zone() {
        return ZoneId.of(timeZone);
    }

    /** Must be called whenever times or recurrence change. */
    public void refreshLastEnd() {
        lastEndAt = RecurrenceExpander.lastEnd(startAt, duration(), zone(), recurrence);
    }

    /** Whether the series places an occurrence starting exactly at {@code start}. */
    public boolean hasOccurrenceAt(Instant start) {
        return RecurrenceExpander.occurrences(startAt, Duration.ZERO, zone(), recurrence, start, start.plusMillis(1))
                .contains(start);
    }

    public Optional<OccurrenceOverride> findOverride(Instant originalStart) {
        return overrides.stream().filter(o -> o.getOriginalStart().equals(originalStart)).findFirst();
    }

    /** Returns the existing override for that occurrence, or attaches a new one. */
    public OccurrenceOverride overrideFor(Instant originalStart) {
        return findOverride(originalStart).orElseGet(() -> {
            OccurrenceOverride o = new OccurrenceOverride(this, originalStart);
            overrides.add(o);
            return o;
        });
    }

    /**
     * After the series was moved to another time of day on the same start date (rule unchanged), moves each
     * cancellation and per-occurrence edit to the same date at the new time, so a cancelled holiday stays
     * cancelled. Individually edited occurrences whose times still followed their slot move along with it;
     * ones given their own times keep them.
     *
     * @param oldEnd the end of an occurrence starting at {@code slot} before the change (null: no end)
     */
    public void moveExceptionsToTimeOfDay(LocalTime time, Function<Instant, Instant> oldEnd) {
        ZoneId z = zone();
        Function<Instant, Instant> move = slot -> ZonedDateTime.of(slot.atZone(z).toLocalDate(), time, z).toInstant();
        Set<Instant> moved = new HashSet<>();
        exclusions.forEach(s -> moved.add(move.apply(s)));
        exclusions.clear();
        exclusions.addAll(moved);
        for (OccurrenceOverride o : overrides) {
            Instant slot = o.getOriginalStart();
            Instant newSlot = move.apply(slot);
            if (o.getStartAt().equals(slot) && Objects.equals(o.getEndAt(), oldEnd.apply(slot))) {
                o.setStartAt(newSlot);
                o.setEndAt(endAt == null ? null : newSlot.plus(duration()));
            }
            o.setOriginalStart(newSlot);
        }
    }

    /**
     * Drops cancellations and per-occurrence edits that no longer match an occurrence
     * (after the series' times or recurrence changed).
     */
    public void pruneStaleExceptions() {
        if (recurrence == null) {
            exclusions.clear();
            overrides.clear();
            return;
        }
        exclusions.removeIf(s -> !hasOccurrenceAt(s));
        overrides.removeIf(o -> !hasOccurrenceAt(o.getOriginalStart()));
    }
}
