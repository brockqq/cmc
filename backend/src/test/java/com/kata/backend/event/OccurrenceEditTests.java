package com.kata.backend.event;

import com.jayway.jsonpath.JsonPath;
import com.kata.backend.ApiTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** "Edit only this occurrence" on recurring series. */
@SpringBootTest
@AutoConfigureMockMvc
class OccurrenceEditTests extends ApiTestSupport {

    static final ZoneId TAIPEI = ZoneId.of("Asia/Taipei");

    @Autowired
    CalendarEventService events;

    @Autowired
    TransactionTemplate transactions;

    String suffix;
    String admin;
    String mgr;
    String res;
    Created a;
    String seriesId;
    /** Original starts of the 6 weekly occurrences (Mondays 19:00-20:00 Taipei). */
    List<Instant> slots;

    @BeforeEach
    void setUp() throws Exception {
        suffix = String.valueOf(System.nanoTime()).substring(8);
        admin = adminToken();
        a = createCommunity(admin, "單次調整" + suffix);
        Created b = createCommunity(admin, "他社區" + suffix);
        register("omgr" + suffix, null);
        makeManager(admin, a, "omgr" + suffix);
        register("ores" + suffix, a.inviteCode());
        register("oresb" + suffix, b.inviteCode());
        mgr = login("omgr" + suffix);
        res = login("ores" + suffix);

        ZonedDateTime first = LocalDate.now(TAIPEI).with(TemporalAdjusters.next(DayOfWeek.MONDAY))
                .atTime(19, 0).atZone(TAIPEI);
        seriesId = idOf(call(mgr, post("/api/events"), """
                {"communityId":%s,"title":"讀書會","location":"中庭","startAt":"%s","endAt":"%s",
                 "timeZone":"Asia/Taipei","recurrence":{"frequency":"WEEKLY","count":6}}"""
                .formatted(a.id(), first.toInstant(), first.plusHours(1).toInstant()))
                .andExpect(status().isCreated()));
        slots = java.util.stream.IntStream.range(0, 6).mapToObj(i -> first.plusWeeks(i).toInstant()).toList();
    }

    @Test
    void movingOneOccurrenceChangesOnlyThatOne() throws Exception {
        // Week 2: move from Monday 19:00 to Wednesday 20:00-21:30 in another room
        ZonedDateTime moved = slots.get(1).atZone(TAIPEI).plusDays(2).withHour(20);
        editOccurrence(slots.get(1), """
                {"title":"讀書會","location":"B棟會議室","startAt":"%s","endAt":"%s"}"""
                .formatted(moved.toInstant(), moved.plusMinutes(90).toInstant()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modified").value(true))
                .andExpect(jsonPath("$.originalStart").value(slots.get(1).toString()));

        List<Map<String, Object>> events = list(res, null, null);
        assertThat(events).hasSize(6);
        Map<String, Object> changed = byOriginal(events, slots.get(1));
        assertThat(changed.get("startAt")).isEqualTo(moved.toInstant().toString());
        assertThat(changed.get("endAt")).isEqualTo(moved.plusMinutes(90).toInstant().toString());
        assertThat(changed.get("location")).isEqualTo("B棟會議室");
        assertThat(changed.get("modified")).isEqualTo(true);
        // The original Monday slot is empty; every other week is untouched
        assertThat(events).noneMatch(e -> e.get("startAt").equals(slots.get(1).toString()));
        assertThat(events.stream().filter(e -> !(Boolean) e.get("modified")))
                .hasSize(5)
                .allMatch(e -> e.get("startAt").equals(e.get("originalStart")) && "中庭".equals(e.get("location")));

        // A title-only series edit keeps the individual change, and the title (not overridden) follows the series
        call(mgr, put("/api/events/{id}", seriesId), """
                {"title":"讀書會（新書）","location":"中庭","startAt":"%s","endAt":"%s","timeZone":"Asia/Taipei",
                 "recurrence":{"frequency":"WEEKLY","count":6}}"""
                .formatted(slots.get(0), slots.get(0).plus(Duration.ofHours(1))))
                .andExpect(status().isOk());
        // The series itself still reports its own values, not the edited occurrence's
        call(res, get("/api/events/{id}", seriesId), null)
                .andExpect(jsonPath("$.location").value("中庭"))
                .andExpect(jsonPath("$.startAt").value(slots.get(0).toString()))
                .andExpect(jsonPath("$.canEdit").value(false));
        call(login("oresb" + suffix), get("/api/events/{id}", seriesId), null).andExpect(status().isNotFound());

        Map<String, Object> afterSeriesEdit = byOriginal(list(res, null, null), slots.get(1));
        assertThat(afterSeriesEdit.get("title")).isEqualTo("讀書會（新書）");
        assertThat(afterSeriesEdit.get("location")).isEqualTo("B棟會議室");
        assertThat(afterSeriesEdit.get("startAt")).isEqualTo(moved.toInstant().toString());
    }

    @Test
    void occurrenceCanMoveOutOfAndIntoTheQueriedRange() throws Exception {
        // Move the first occurrence ~200 days ahead: gone from the default 90-day view, visible far ahead
        Instant farAhead = slots.get(0).plus(Duration.ofDays(200));
        editOccurrence(slots.get(0), """
                {"title":"讀書會","startAt":"%s"}""".formatted(farAhead)).andExpect(status().isOk());

        assertThat(list(res, null, null)).hasSize(5);
        List<Map<String, Object>> later = list(res, farAhead.minus(Duration.ofDays(1)), farAhead.plus(Duration.ofDays(1)));
        assertThat(later).hasSize(1);
        assertThat(later.getFirst().get("originalStart")).isEqualTo(slots.get(0).toString());
        // Moved-away occurrence has no end (none was given) → point in time
        assertThat(later.getFirst().get("endAt")).isNull();
    }

    @Test
    void resetCancelAndExplicitlyClearedFields() throws Exception {
        Instant moved = slots.get(2).plus(Duration.ofHours(2));
        editOccurrence(slots.get(2), """
                {"title":"讀書會","location":"","startAt":"%s"}""".formatted(moved)).andExpect(status().isOk());
        // Clearing the location for this one occurrence does not fall back to the series' location
        assertThat(byOriginal(list(res, null, null), slots.get(2)).get("location")).isNull();

        call(mgr, delete("/api/events/{id}/occurrences/changes", seriesId).param("start", slots.get(2).toString()), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modified").value(false));
        Map<String, Object> reverted = byOriginal(list(res, null, null), slots.get(2));
        assertThat(reverted.get("startAt")).isEqualTo(slots.get(2).toString());
        assertThat(reverted.get("location")).isEqualTo("中庭");
        call(mgr, delete("/api/events/{id}/occurrences/changes", seriesId).param("start", slots.get(2).toString()), null)
                .andExpect(status().isNotFound());

        // Cancelling an edited occurrence removes it entirely; it can no longer be edited
        editOccurrence(slots.get(3), """
                {"title":"特別場","startAt":"%s"}""".formatted(slots.get(3).plus(Duration.ofDays(1))))
                .andExpect(status().isOk());
        call(mgr, delete("/api/events/{id}/occurrences", seriesId).param("start", slots.get(3).toString()), null)
                .andExpect(status().isNoContent());
        List<Map<String, Object>> events = list(res, null, null);
        assertThat(events).hasSize(5).noneMatch(e -> "特別場".equals(e.get("title")));
        editOccurrence(slots.get(3), """
                {"title":"x","startAt":"%s"}""".formatted(slots.get(3))).andExpect(status().isNotFound());
    }

    @Test
    void movingTheSeriesToAnotherTimeOfDayKeepsCancellationsAndEdits() throws Exception {
        Duration hour = Duration.ofHours(1);
        // Occurrence 1: only the title changed (its times follow the slot); occurrence 3: moved to the next day
        editOccurrence(slots.get(1), """
                {"title":"改過","startAt":"%s","endAt":"%s"}""".formatted(slots.get(1), slots.get(1).plus(hour)));
        Instant ownTime = slots.get(3).plus(Duration.ofDays(1));
        editOccurrence(slots.get(3), """
                {"title":"讀書會","startAt":"%s","endAt":"%s"}""".formatted(ownTime, ownTime.plus(hour)));
        call(mgr, delete("/api/events/{id}/occurrences", seriesId).param("start", slots.get(2).toString()), null)
                .andExpect(status().isNoContent()); // e.g. a public holiday

        // Same dates, 19:00 → 20:30 and now two hours long
        Instant newFirst = slots.get(0).plus(Duration.ofMinutes(90));
        call(mgr, put("/api/events/{id}", seriesId), """
                {"title":"讀書會","startAt":"%s","endAt":"%s","timeZone":"Asia/Taipei",
                 "recurrence":{"frequency":"WEEKLY","count":6}}"""
                .formatted(newFirst, newFirst.plus(Duration.ofHours(2))))
                .andExpect(status().isOk());

        List<Map<String, Object>> events = list(res, null, null);
        Duration shift = Duration.ofMinutes(90);
        assertThat(events).hasSize(5) // the cancellation still holds
                .noneMatch(e -> e.get("originalStart").equals(slots.get(2).plus(shift).toString()));
        Map<String, Object> retitled = byOriginal(events, slots.get(1).plus(shift));
        assertThat(retitled.get("title")).isEqualTo("改過");
        assertThat(retitled.get("startAt")).isEqualTo(slots.get(1).plus(shift).toString());
        assertThat(retitled.get("endAt")).isEqualTo(slots.get(1).plus(shift).plus(Duration.ofHours(2)).toString());
        Map<String, Object> moved = byOriginal(events, slots.get(3).plus(shift));
        assertThat(moved.get("startAt")).isEqualTo(ownTime.toString()); // its own time is kept
    }

    @Test
    void movingTheSeriesToOtherDatesDropsOrphanedExceptions() throws Exception {
        editOccurrence(slots.get(1), """
                {"title":"改過","startAt":"%s"}""".formatted(slots.get(1).plus(Duration.ofDays(1))));
        call(mgr, delete("/api/events/{id}/occurrences", seriesId).param("start", slots.get(2).toString()), null)
                .andExpect(status().isNoContent());

        // Series moves from Mondays to Tuesdays: the old slots no longer exist, so their exceptions are discarded
        call(mgr, put("/api/events/{id}", seriesId), """
                {"title":"讀書會","startAt":"%s","timeZone":"Asia/Taipei","recurrence":{"frequency":"WEEKLY","count":6}}"""
                .formatted(slots.get(0).plus(Duration.ofDays(1))))
                .andExpect(status().isOk());

        List<Map<String, Object>> events = list(res, null, null);
        assertThat(events).hasSize(6).noneMatch(e -> (Boolean) e.get("modified"));
    }

    @Test
    void anEditBasedOnAnOutdatedVersionIsRefused() throws Exception {
        long seen = ((Number) byOriginal(list(mgr, null, null), slots.get(1)).get("version")).longValue();

        // Someone else cancels an occurrence in the meantime...
        call(admin, delete("/api/events/{id}/occurrences", seriesId).param("start", slots.get(4).toString()), null)
                .andExpect(status().isNoContent());

        // ...so changes based on what this manager saw earlier are refused rather than applied blindly
        editOccurrence(slots.get(1), """
                {"title":"改過","startAt":"%s","version":%d}""".formatted(slots.get(1), seen))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("此活動剛被其他人修改過，請重新整理後再試"));
        call(mgr, put("/api/events/{id}", seriesId), """
                {"title":"新名稱","startAt":"%s","timeZone":"Asia/Taipei","recurrence":{"frequency":"WEEKLY","count":6},
                 "version":%d}""".formatted(slots.get(0), seen))
                .andExpect(status().isConflict());
        call(mgr, delete("/api/events/{id}", seriesId).param("version", String.valueOf(seen)), null)
                .andExpect(status().isConflict());

        // With the current version it goes through, and every change moves the version on
        long current = ((Number) byOriginal(list(mgr, null, null), slots.get(1)).get("version")).longValue();
        assertThat(current).isGreaterThan(seen);
        String body = editOccurrence(slots.get(1), """
                {"title":"改過","startAt":"%s","version":%d}""".formatted(slots.get(1), current))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(((Number) JsonPath.read(body, "$.version")).longValue()).isGreaterThan(current);
    }

    @Test
    void anOccurrenceEditWaitsForAConcurrentSeriesChangeAndLeavesNoGhost() throws Exception {
        String managerName = "omgr" + suffix;
        EventDtos.RecurrenceDto weekly = new EventDtos.RecurrenceDto(Frequency.WEEKLY, 1, List.of(), null, 6);
        CountDownLatch moved = new CountDownLatch(1);
        CountDownLatch commit = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            // Another manager moves the series from Mondays to Tuesdays, not yet committed
            Future<?> seriesChange = pool.submit(() -> transactions.executeWithoutResult(tx -> {
                events.update(managerName, Long.valueOf(seriesId), new EventDtos.UpdateRequest("讀書會", null, null,
                        slots.get(0).plus(Duration.ofDays(1)), null, "Asia/Taipei", weekly, null));
                moved.countDown();
                await(commit);
            }));
            assertThat(moved.await(5, TimeUnit.SECONDS)).isTrue();

            // Meanwhile this manager cancels the old Monday slot 2
            Future<Integer> cancel = pool.submit(() -> call(mgr, delete("/api/events/{id}/occurrences", seriesId)
                    .param("start", slots.get(2).toString()), null).andReturn().getResponse().getStatus());
            Thread.sleep(300);
            assertThat(cancel.isDone()).as("the cancel must wait for the series lock").isFalse();

            commit.countDown();
            seriesChange.get(5, TimeUnit.SECONDS);
            // It then sees the new series, where that Monday no longer exists: nothing is stored for it
            assertThat(cancel.get(5, TimeUnit.SECONDS)).isEqualTo(404);
        } finally {
            commit.countDown();
            pool.shutdownNow();
        }
        assertThat(list(res, null, null)).hasSize(6);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void subSecondTimesAreIgnoredWhenIdentifyingAnOccurrence() throws Exception {
        // A client sending the slot with milliseconds still addresses the same occurrence
        editOccurrence(slots.get(1).plusMillis(123), """
                {"title":"改過","startAt":"%s"}""".formatted(slots.get(1).plusNanos(456_789)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalStart").value(slots.get(1).toString()))
                .andExpect(jsonPath("$.startAt").value(slots.get(1).toString()));
    }

    @Test
    void permissionsAndValidation() throws Exception {
        String body = """
                {"title":"x","startAt":"%s"}""".formatted(slots.get(1));

        call(res, put("/api/events/{id}/occurrences", seriesId).param("start", slots.get(1).toString()), body)
                .andExpect(status().isForbidden());
        call(login("oresb" + suffix), put("/api/events/{id}/occurrences", seriesId)
                .param("start", slots.get(1).toString()), body)
                .andExpect(status().isNotFound());
        // Not a slot of this series
        editOccurrence(slots.get(1).plus(Duration.ofMinutes(5)), body).andExpect(status().isNotFound());
        editOccurrence(slots.get(1), """
                {"title":"x","startAt":"%s","endAt":"%s"}""".formatted(slots.get(1), slots.get(0)))
                .andExpect(status().isBadRequest());
        editOccurrence(slots.get(1), """
                {"title":"","startAt":"%s"}""".formatted(slots.get(1)))
                .andExpect(status().isBadRequest());

        String oneOff = idOf(call(mgr, post("/api/events"), """
                {"communityId":%s,"title":"單次","startAt":"%s"}""".formatted(a.id(), slots.get(0)))
                .andExpect(status().isCreated()));
        call(mgr, put("/api/events/{id}/occurrences", oneOff).param("start", slots.get(0).toString()), body)
                .andExpect(status().isBadRequest());
    }

    private org.springframework.test.web.servlet.ResultActions editOccurrence(Instant originalStart, String json)
            throws Exception {
        return call(mgr, put("/api/events/{id}/occurrences", seriesId).param("start", originalStart.toString()), json);
    }

    private List<Map<String, Object>> list(String token, Instant from, Instant to) throws Exception {
        var req = get("/api/events").param("communityId", a.id());
        if (from != null) {
            req.param("from", from.toString()).param("to", to.toString());
        }
        return JsonPath.read(call(token, req, null).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$");
    }

    private static Map<String, Object> byOriginal(List<Map<String, Object>> events, Instant originalStart) {
        return events.stream().filter(e -> e.get("originalStart").equals(originalStart.toString()))
                .findFirst().orElseThrow(() -> new AssertionError("no occurrence for " + originalStart));
    }
}
