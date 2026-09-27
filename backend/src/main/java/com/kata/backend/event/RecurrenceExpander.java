package com.kata.backend.event;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Expands a (possibly recurring) event into concrete occurrence start times.
 * <p>
 * Occurrences are computed in the event's time zone so that "every Monday 19:00" stays at 19:00 local time.
 * Monthly/yearly series anchored on a day that a month lacks (e.g. the 31st, Feb 29) fall on that month's
 * last day. Weekly series with explicit days use Monday-based weeks for the interval; the start is always an
 * occurrence (and counts toward {@code count}) even when its weekday is not one of the listed days.
 */
public final class RecurrenceExpander {

    /** Guards against runaway loops (e.g. a daily series queried decades into the future). */
    static final int MAX_STEPS = 100_000;

    private RecurrenceExpander() {
    }

    /**
     * Start instants of occurrences overlapping [from, to), in chronological order.
     * An occurrence overlaps when it starts before {@code to} and ends at or after {@code from}.
     */
    public static List<Instant> occurrences(Instant start, Duration duration, ZoneId zone, Recurrence rule,
                                            Instant from, Instant to) {
        List<Instant> result = new ArrayList<>();
        // Anything starting before this ends before the window, so those periods need not be generated
        generate(start, zone, rule, from.minus(duration), occurrence -> {
            if (!occurrence.isBefore(to)) {
                return false; // generated in order, nothing later can match
            }
            if (!occurrence.plus(duration).isBefore(from)) {
                result.add(occurrence);
            }
            return true;
        });
        return result;
    }

    /** End of the last occurrence, or {@code null} when the series never ends. */
    public static Instant lastEnd(Instant start, Duration duration, ZoneId zone, Recurrence rule) {
        if (rule != null && rule.isInfinite()) {
            return null;
        }
        Instant[] last = {start};
        generate(start, zone, rule, null, occurrence -> {
            last[0] = occurrence;
            return true;
        });
        return last[0].plus(duration);
    }

    /**
     * Emits occurrence starts in order until the consumer returns false or the series ends.
     * When {@code skipBefore} is given, periods that end before it may be skipped without being generated
     * (so a daily series started years ago costs no more than a new one). A series with a {@code count} is
     * always generated from the start, since earlier occurrences decide where it ends; count is at most 500.
     */
    private static void generate(Instant start, ZoneId zone, Recurrence rule, Instant skipBefore,
                                 Predicate<Instant> consumer) {
        if (rule == null) {
            consumer.test(start);
            return;
        }
        ZonedDateTime first = start.atZone(zone);
        int firstStep = skipBefore == null || rule.getCount() != null ? 0 : stepsBefore(first, rule, skipBefore);
        int emitted = 0;
        for (int step = firstStep; step < firstStep + MAX_STEPS; step++) {
            for (ZonedDateTime candidate : candidates(first, rule, step)) {
                if (candidate.isBefore(first)) {
                    continue; // weekly days earlier in the first week
                }
                if (rule.getUntil() != null && candidate.toLocalDate().isAfter(rule.getUntil())) {
                    return;
                }
                if (rule.getCount() != null && emitted >= rule.getCount()) {
                    return;
                }
                emitted++;
                if (!consumer.test(candidate.toInstant())) {
                    return;
                }
            }
        }
    }

    /**
     * A step index whose candidates all start before {@code target} or at most one period later: every period
     * before it lies entirely before {@code target}. Errs low by one period to stay clear of DST and month-end
     * edge cases; generating one extra period is harmless.
     */
    static int stepsBefore(ZonedDateTime first, Recurrence rule, Instant target) {
        LocalDate from = first.toLocalDate();
        LocalDate to = target.atZone(first.getZone()).toLocalDate();
        if (!to.isAfter(from)) {
            return 0;
        }
        long units = switch (rule.getFrequency()) {
            case DAILY -> ChronoUnit.DAYS.between(from, to);
            // Weekly periods are Monday-based weeks (see candidates)
            case WEEKLY -> ChronoUnit.WEEKS.between(
                    from.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                    to.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
            case MONTHLY -> ChronoUnit.MONTHS.between(YearMonth.from(from), YearMonth.from(to));
            case YEARLY -> to.getYear() - from.getYear();
        };
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE / 2, units / rule.intervalOrDefault() - 1));
    }

    /** Candidate starts for the {@code step}-th period, in order. */
    private static List<ZonedDateTime> candidates(ZonedDateTime first, Recurrence rule, int step) {
        long n = (long) step * rule.intervalOrDefault();
        return switch (rule.getFrequency()) {
            case DAILY -> List.of(first.plusDays(n));
            // plusMonths/plusYears from the original start clamp to month end without drifting
            case MONTHLY -> List.of(first.plusMonths(n));
            case YEARLY -> List.of(first.plusYears(n));
            case WEEKLY -> {
                var days = rule.daysOfWeek();
                if (days.isEmpty()) {
                    yield List.of(first.plusWeeks(n));
                }
                LocalDate weekStart = first.toLocalDate()
                        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                        .plusWeeks(n);
                LocalTime time = first.toLocalTime();
                List<ZonedDateTime> week = new ArrayList<>(days.stream() // EnumSet iterates Monday..Sunday
                        .map(d -> ZonedDateTime.of(weekStart.plusDays(d.getValue() - 1L), time, first.getZone()))
                        .toList());
                if (step == 0 && !days.contains(first.getDayOfWeek())) {
                    // As in iCalendar, the start itself is always the first occurrence, even on an unlisted day
                    week.add(first);
                    week.sort(null);
                }
                yield week;
            }
        };
    }
}
