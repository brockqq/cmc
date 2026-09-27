package com.kata.backend.event;

import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecurrenceExpanderTest {

    static final ZoneId TAIPEI = ZoneId.of("Asia/Taipei");
    static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

    static Instant at(ZoneId zone, String localDateTime) {
        return LocalDateTime.parse(localDateTime).atZone(zone).toInstant();
    }

    static List<LocalDateTime> local(List<Instant> instants, ZoneId zone) {
        return instants.stream().map(i -> LocalDateTime.ofInstant(i, zone)).toList();
    }

    static Recurrence rule(Frequency f, int interval, String days, LocalDate until, Integer count) {
        return new Recurrence(f, interval, days, until, count);
    }

    static final Instant FAR_PAST = Instant.parse("2000-01-01T00:00:00Z");
    static final Instant FAR_FUTURE = Instant.parse("2100-01-01T00:00:00Z");

    @Test
    void oneOffEventOverlapsWhenItsSpanTouchesTheWindow() {
        Instant start = at(TAIPEI, "2026-10-01T10:00");
        Duration twoHours = Duration.ofHours(2);
        assertThat(RecurrenceExpander.occurrences(start, twoHours, TAIPEI, null,
                at(TAIPEI, "2026-10-01T11:00"), at(TAIPEI, "2026-10-02T00:00"))).containsExactly(start);
        assertThat(RecurrenceExpander.occurrences(start, twoHours, TAIPEI, null,
                at(TAIPEI, "2026-10-01T12:01"), at(TAIPEI, "2026-10-02T00:00"))).isEmpty();
        assertThat(RecurrenceExpander.lastEnd(start, twoHours, TAIPEI, null)).isEqualTo(start.plus(twoHours));
    }

    @Test
    void dailyEveryTwoDaysUntilDateInclusive() {
        Instant start = at(TAIPEI, "2026-10-01T08:00");
        Recurrence r = rule(Frequency.DAILY, 2, null, LocalDate.parse("2026-10-07"), null);
        assertThat(local(RecurrenceExpander.occurrences(start, Duration.ZERO, TAIPEI, r, FAR_PAST, FAR_FUTURE), TAIPEI))
                .containsExactly(
                        LocalDateTime.parse("2026-10-01T08:00"),
                        LocalDateTime.parse("2026-10-03T08:00"),
                        LocalDateTime.parse("2026-10-05T08:00"),
                        LocalDateTime.parse("2026-10-07T08:00"));
        assertThat(RecurrenceExpander.lastEnd(start, Duration.ZERO, TAIPEI, r)).isEqualTo(at(TAIPEI, "2026-10-07T08:00"));
    }

    @Test
    void weeklyOnSeveralDaysWithCount() {
        // 2026-10-01 is a Thursday; recycling on Tue/Thu/Sat, 5 times
        Instant start = at(TAIPEI, "2026-10-01T19:00");
        Recurrence r = rule(Frequency.WEEKLY, 1, "TUESDAY,THURSDAY,SATURDAY", null, 5);
        assertThat(local(RecurrenceExpander.occurrences(start, Duration.ZERO, TAIPEI, r, FAR_PAST, FAR_FUTURE), TAIPEI))
                .containsExactly(
                        LocalDateTime.parse("2026-10-01T19:00"),
                        LocalDateTime.parse("2026-10-03T19:00"),
                        LocalDateTime.parse("2026-10-06T19:00"),
                        LocalDateTime.parse("2026-10-08T19:00"),
                        LocalDateTime.parse("2026-10-10T19:00"));
    }

    @Test
    void weeklyStartOnAnUnlistedDayIsStillTheFirstOccurrence() {
        // 2026-09-30 is a Wednesday; the series repeats on Mondays only, 3 times in total
        Instant start = at(TAIPEI, "2026-09-30T19:00");
        Recurrence r = rule(Frequency.WEEKLY, 1, "MONDAY", null, 3);
        assertThat(local(RecurrenceExpander.occurrences(start, Duration.ZERO, TAIPEI, r, FAR_PAST, FAR_FUTURE), TAIPEI))
                .containsExactly(
                        LocalDateTime.parse("2026-09-30T19:00"),
                        LocalDateTime.parse("2026-10-05T19:00"),
                        LocalDateTime.parse("2026-10-12T19:00"));
    }

    @Test
    void skippingAheadGivesTheSameOccurrencesAsExpandingFromTheStart() {
        Instant start = at(NEW_YORK, "2000-01-31T23:30"); // month end, late evening, a DST zone
        Duration duration = Duration.ofHours(3); // crosses midnight
        List<Recurrence> rules = List.of(
                rule(Frequency.DAILY, 1, null, null, null),
                rule(Frequency.DAILY, 3, null, null, null),
                rule(Frequency.WEEKLY, 1, null, null, null),
                rule(Frequency.WEEKLY, 2, "MONDAY,WEDNESDAY,SUNDAY", null, null),
                rule(Frequency.WEEKLY, 3, "TUESDAY", LocalDate.parse("2009-06-30"), null),
                rule(Frequency.MONTHLY, 1, null, null, null),
                rule(Frequency.MONTHLY, 5, null, null, null),
                rule(Frequency.YEARLY, 1, null, null, null),
                rule(Frequency.YEARLY, 4, null, LocalDate.parse("2080-01-01"), null));
        List<Instant> windowStarts = List.of(
                at(NEW_YORK, "2000-01-31T23:00"), at(NEW_YORK, "2000-02-01T01:00"), at(NEW_YORK, "2003-03-09T00:00"),
                at(NEW_YORK, "2008-11-02T01:30"), at(NEW_YORK, "2026-02-28T12:00"), at(NEW_YORK, "2099-12-01T00:00"));
        for (Recurrence r : rules) {
            for (Instant from : windowStarts) {
                for (Duration length : List.of(Duration.ofHours(1), Duration.ofDays(35), Duration.ofDays(400))) {
                    Instant to = from.plus(length);
                    // FAR_PAST lies before the start, so this expands every period from the first one
                    List<Instant> expected = RecurrenceExpander.occurrences(start, duration, NEW_YORK, r, FAR_PAST, to)
                            .stream().filter(o -> !o.plus(duration).isBefore(from)).toList();
                    assertThat(RecurrenceExpander.occurrences(start, duration, NEW_YORK, r, from, to))
                            .as("%s %s from %s", r.getFrequency(), r.intervalOrDefault(), from)
                            .isEqualTo(expected);
                }
            }
        }
    }

    @Test
    void skipsStraightToTheWindowInsteadOfWalkingFromTheStart() {
        ZonedDateTime first = at(TAIPEI, "2000-01-01T08:00").atZone(TAIPEI);
        Instant target = at(TAIPEI, "2099-01-01T00:00");
        int step = RecurrenceExpander.stepsBefore(first, rule(Frequency.DAILY, 1, null, null, null), target);
        // Roughly 36,000 days are skipped; only the periods around the window are generated
        assertThat(step).isBetween(36_100, 36_160);
        assertThat(first.plusDays(step).toInstant()).isBefore(target);
        assertThat(RecurrenceExpander.stepsBefore(first, rule(Frequency.DAILY, 1, null, null, null),
                at(TAIPEI, "1999-01-01T00:00"))).isZero();
    }

    @Test
    void biweeklyWithoutDaysUsesStartWeekday() {
        Instant start = at(TAIPEI, "2026-10-05T09:00"); // Monday
        Recurrence r = rule(Frequency.WEEKLY, 2, null, null, 3);
        assertThat(local(RecurrenceExpander.occurrences(start, Duration.ZERO, TAIPEI, r, FAR_PAST, FAR_FUTURE), TAIPEI))
                .containsExactly(
                        LocalDateTime.parse("2026-10-05T09:00"),
                        LocalDateTime.parse("2026-10-19T09:00"),
                        LocalDateTime.parse("2026-11-02T09:00"));
    }

    @Test
    void monthlyOn31stFallsOnLastDayWithoutDrifting() {
        Instant start = at(TAIPEI, "2026-01-31T10:00");
        Recurrence r = rule(Frequency.MONTHLY, 1, null, null, 4);
        assertThat(local(RecurrenceExpander.occurrences(start, Duration.ZERO, TAIPEI, r, FAR_PAST, FAR_FUTURE), TAIPEI))
                .containsExactly(
                        LocalDateTime.parse("2026-01-31T10:00"),
                        LocalDateTime.parse("2026-02-28T10:00"),
                        LocalDateTime.parse("2026-03-31T10:00"),
                        LocalDateTime.parse("2026-04-30T10:00"));
    }

    @Test
    void yearlyOnLeapDay() {
        Instant start = at(TAIPEI, "2028-02-29T10:00");
        Recurrence r = rule(Frequency.YEARLY, 1, null, null, 2);
        assertThat(local(RecurrenceExpander.occurrences(start, Duration.ZERO, TAIPEI, r, FAR_PAST, FAR_FUTURE), TAIPEI))
                .containsExactly(LocalDateTime.parse("2028-02-29T10:00"), LocalDateTime.parse("2029-02-28T10:00"));
    }

    @Test
    void keepsLocalTimeAcrossDaylightSavingChanges() {
        // US DST ends 2026-11-01: the UTC offset changes but the meeting stays at 19:00 local
        Instant start = at(NEW_YORK, "2026-10-26T19:00");
        Recurrence r = rule(Frequency.WEEKLY, 1, null, null, 2);
        List<Instant> occurrences = RecurrenceExpander.occurrences(start, Duration.ZERO, NEW_YORK, r, FAR_PAST, FAR_FUTURE);
        assertThat(local(occurrences, NEW_YORK)).containsExactly(
                LocalDateTime.parse("2026-10-26T19:00"), LocalDateTime.parse("2026-11-02T19:00"));
        assertThat(Duration.between(occurrences.get(0), occurrences.get(1))).isEqualTo(Duration.ofDays(7).plusHours(1));
    }

    @Test
    void infiniteSeriesIsWindowedAndHasNoLastEnd() {
        Instant start = at(TAIPEI, "2026-01-05T09:00"); // Monday
        Recurrence r = rule(Frequency.WEEKLY, 1, null, null, null);
        assertThat(RecurrenceExpander.lastEnd(start, Duration.ofHours(1), TAIPEI, r)).isNull();
        // Two years later, one week window
        assertThat(local(RecurrenceExpander.occurrences(start, Duration.ofHours(1), TAIPEI, r,
                at(TAIPEI, "2028-01-03T00:00"), at(TAIPEI, "2028-01-10T00:00")), TAIPEI))
                .containsExactly(LocalDateTime.parse("2028-01-03T09:00"));
    }

    @Test
    void occurrenceInProgressAtWindowStartIsIncluded() {
        Instant start = at(TAIPEI, "2026-10-01T22:00");
        Recurrence r = rule(Frequency.DAILY, 1, null, null, 3);
        // Each occurrence lasts 4h (22:00-02:00); a window from 01:00 on Oct 2 catches the one started on Oct 1
        assertThat(local(RecurrenceExpander.occurrences(start, Duration.ofHours(4), TAIPEI, r,
                at(TAIPEI, "2026-10-02T01:00"), at(TAIPEI, "2026-10-02T12:00")), TAIPEI))
                .containsExactly(LocalDateTime.parse("2026-10-01T22:00"));
    }
}
