package com.kata.backend.event;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * How an event repeats. Stored inline on the event; a null embedded value means a one-off event.
 * At most one of {@code until} / {@code count} is set; neither means the series repeats forever.
 */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Recurrence {

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_frequency", length = 10)
    private Frequency frequency;

    /** Repeat every N days/weeks/months/years (≥ 1). */
    @Column(name = "recurrence_interval")
    private Integer interval;

    /** WEEKLY only: comma-separated {@link DayOfWeek} names; empty means the start date's weekday. */
    @Column(name = "recurrence_days", length = 80)
    private String days;

    /** Last date (inclusive, in the event's time zone) an occurrence may start on. */
    @Column(name = "recurrence_until")
    private LocalDate until;

    /** Total number of occurrences. */
    @Column(name = "recurrence_count")
    private Integer count;

    public int intervalOrDefault() {
        return interval == null ? 1 : interval;
    }

    public Set<DayOfWeek> daysOfWeek() {
        if (days == null || days.isBlank()) {
            return EnumSet.noneOf(DayOfWeek.class);
        }
        return Arrays.stream(days.split(","))
                .map(String::trim)
                .map(DayOfWeek::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));
    }

    public static String joinDays(Set<DayOfWeek> days) {
        if (days == null || days.isEmpty()) {
            return null;
        }
        return EnumSet.copyOf(days).stream().map(DayOfWeek::name).collect(Collectors.joining(","));
    }

    public boolean isInfinite() {
        return until == null && count == null;
    }
}
