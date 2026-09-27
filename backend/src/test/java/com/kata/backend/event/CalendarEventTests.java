package com.kata.backend.event;

import com.jayway.jsonpath.JsonPath;
import com.kata.backend.ApiTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CalendarEventTests extends ApiTestSupport {

    static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.SECONDS);

    String admin;
    String suffix;
    Created a;
    Created b;

    @BeforeEach
    void setUp() throws Exception {
        suffix = String.valueOf(System.nanoTime()).substring(8);
        admin = adminToken();
        a = createCommunity(admin, "行事曆A" + suffix);
        b = createCommunity(admin, "行事曆B" + suffix);
        register("emgr" + suffix, null);
        makeManager(admin, a, "emgr" + suffix);
        register("eres" + suffix, a.inviteCode());
        register("eresb" + suffix, b.inviteCode());
    }

    @Test
    void membersSeeUpcomingEventsOfTheirOwnCommunities() throws Exception {
        String mgr = login("emgr" + suffix);
        createEvent(mgr, a, "下週大掃除", NOW.plus(Duration.ofDays(7)), null);
        createEvent(mgr, a, "昨天的會議", NOW.minus(Duration.ofDays(1)), NOW.minus(Duration.ofDays(1)).plus(Duration.ofHours(2)));
        createEvent(mgr, a, "明年的活動", NOW.plus(Duration.ofDays(200)), null);
        createEvent(admin, b, "B社區烤肉", NOW.plus(Duration.ofDays(3)), null);

        // Default window: now .. +90 days, sorted by start time
        List<String> upcoming = titles(call(login("eres" + suffix), get("/api/events"), null)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(upcoming).containsExactly("下週大掃除");

        List<String> seenByB = titles(call(login("eresb" + suffix), get("/api/events"), null)
                .andReturn().getResponse().getContentAsString());
        assertThat(seenByB).containsExactly("B社區烤肉");

        // Explicit range (e.g. a calendar month view) includes past events
        List<String> ranged = titles(call(login("eres" + suffix), get("/api/events")
                        .param("from", NOW.minus(Duration.ofDays(2)).toString())
                        .param("to", NOW.plus(Duration.ofDays(10)).toString()), null)
                .andReturn().getResponse().getContentAsString());
        assertThat(ranged).containsExactly("昨天的會議", "下週大掃除");

        call(login("eres" + suffix), get("/api/events").param("communityId", b.id()), null)
                .andExpect(status().isNotFound());
    }

    @Test
    void multiDayEventStillShowsWhileInProgress() throws Exception {
        String mgr = login("emgr" + suffix);
        createEvent(mgr, a, "進行中的市集", NOW.minus(Duration.ofDays(1)), NOW.plus(Duration.ofDays(1)));
        call(login("eres" + suffix), get("/api/events"), null)
                .andExpect(jsonPath("$[0].title").value("進行中的市集"));
    }

    @Test
    void onlyManagersCanCreateEditAndDelete() throws Exception {
        String mgr = login("emgr" + suffix);
        String res = login("eres" + suffix);
        String start = NOW.plus(Duration.ofDays(5)).toString();

        call(res, post("/api/events"), """
                {"communityId":%s,"title":"x","startAt":"%s"}""".formatted(a.id(), start))
                .andExpect(status().isForbidden());
        call(mgr, post("/api/events"), """
                {"communityId":%s,"title":"x","startAt":"%s"}""".formatted(b.id(), start))
                .andExpect(status().isNotFound());
        call(mgr, post("/api/events"), """
                {"communityId":%s,"title":"x","startAt":"%s","endAt":"%s"}"""
                .formatted(a.id(), start, NOW.toString()))
                .andExpect(status().isBadRequest());
        // Absurd dates would make every calendar query expand the series from year 1
        call(mgr, post("/api/events"), """
                {"communityId":%s,"title":"x","startAt":"0001-01-01T00:00:00Z","recurrence":{"frequency":"DAILY"}}"""
                .formatted(a.id()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("活動時間需在 2000 年至 2099 年之間"));

        String id = createEvent(mgr, a, "住戶大會", NOW.plus(Duration.ofDays(5)), null);
        call(res, get("/api/events"), null).andExpect(jsonPath("$[0].canEdit").value(false));
        call(res, delete("/api/events/{id}", id), null).andExpect(status().isForbidden());
        call(login("eresb" + suffix), delete("/api/events/{id}", id), null).andExpect(status().isNotFound());

        call(mgr, put("/api/events/{id}", id), """
                {"title":"住戶大會（改期）","location":"交誼廳","startAt":"%s"}"""
                .formatted(NOW.plus(Duration.ofDays(6))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location").value("交誼廳"));
        call(mgr, delete("/api/events/{id}", id), null).andExpect(status().isNoContent());
    }

    @Test
    void recurringSeriesExpandsIntoOccurrencesAndSingleOnesCanBeCancelled() throws Exception {
        String mgr = login("emgr" + suffix);
        String res = login("eres" + suffix);
        // Next Monday 19:00 Taipei time, weekly on Mon & Thu, 6 times
        ZoneId taipei = ZoneId.of("Asia/Taipei");
        ZonedDateTime monday = LocalDate.now(taipei).with(TemporalAdjusters.next(DayOfWeek.MONDAY))
                .atTime(19, 0).atZone(taipei);
        String id = idOf(call(mgr, post("/api/events"), """
                {"communityId":%s,"title":"資源回收","startAt":"%s","endAt":"%s","timeZone":"Asia/Taipei",
                 "recurrence":{"frequency":"WEEKLY","interval":1,"daysOfWeek":["MONDAY","THURSDAY"],"count":6}}"""
                .formatted(a.id(), monday.toInstant(), monday.plusHours(1).toInstant()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recurrence.frequency").value("WEEKLY"))
                .andExpect(jsonPath("$.recurrence.daysOfWeek.length()").value(2)));

        String body = call(res, get("/api/events").param("communityId", a.id()), null)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<String> starts = JsonPath.read(body, "$[*].startAt");
        assertThat(starts).hasSize(6);
        List<DayOfWeek> weekdays = starts.stream()
                .map(s -> Instant.parse(s).atZone(taipei).getDayOfWeek()).distinct().toList();
        assertThat(weekdays).containsExactlyInAnyOrder(DayOfWeek.MONDAY, DayOfWeek.THURSDAY);
        // Each occurrence keeps the 1-hour duration and carries the series start for editing
        assertThat((String) JsonPath.read(body, "$[1].endAt"))
                .isEqualTo(Instant.parse(starts.get(1)).plus(Duration.ofHours(1)).toString());
        assertThat((String) JsonPath.read(body, "$[3].seriesStartAt")).isEqualTo(monday.toInstant().toString());

        // Residents cannot cancel; managers can cancel one occurrence
        call(res, delete("/api/events/{id}/occurrences", id).param("start", starts.get(2)), null)
                .andExpect(status().isForbidden());
        call(mgr, delete("/api/events/{id}/occurrences", id).param("start", starts.get(2)), null)
                .andExpect(status().isNoContent());
        call(mgr, delete("/api/events/{id}/occurrences", id)
                        .param("start", Instant.parse(starts.get(2)).plusSeconds(60).toString()), null)
                .andExpect(status().isNotFound());

        List<String> afterCancel = JsonPath.read(call(res, get("/api/events").param("communityId", a.id()), null)
                .andReturn().getResponse().getContentAsString(), "$[*].startAt");
        assertThat(afterCancel).hasSize(5).doesNotContain(starts.get(2));

        // Deleting the series removes every occurrence
        call(mgr, delete("/api/events/{id}", id), null).andExpect(status().isNoContent());
        call(res, get("/api/events").param("communityId", a.id()), null)
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void infiniteSeriesShowsUpInAnyFutureWindow() throws Exception {
        String mgr = login("emgr" + suffix);
        createEventJson(mgr, """
                {"communityId":%s,"title":"每月住戶會議","startAt":"%s",
                 "recurrence":{"frequency":"MONTHLY"}}""".formatted(a.id(), NOW.plus(Duration.ofDays(1))));

        Instant from = NOW.plus(Duration.ofDays(365 * 3));
        call(login("eres" + suffix), get("/api/events")
                        .param("from", from.toString()).param("to", from.plus(Duration.ofDays(62)).toString()), null)
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void rejectsInvalidRecurrence() throws Exception {
        String mgr = login("emgr" + suffix);
        String start = NOW.plus(Duration.ofDays(1)).toString();
        String base = """
                {"communityId":%s,"title":"x","startAt":"%s","recurrence":%s}""";

        call(mgr, post("/api/events"), base.formatted(a.id(), start,
                "{\"frequency\":\"DAILY\",\"until\":\"2099-01-01\",\"count\":3}"))
                .andExpect(status().isBadRequest());
        call(mgr, post("/api/events"), base.formatted(a.id(), start,
                "{\"frequency\":\"DAILY\",\"until\":\"2000-01-01\"}"))
                .andExpect(status().isBadRequest());
        call(mgr, post("/api/events"), base.formatted(a.id(), start, "{\"frequency\":\"DAILY\",\"interval\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['recurrence.interval']").exists());
        call(mgr, post("/api/events"), base.formatted(a.id(), start, "{\"frequency\":\"DAILY\",\"count\":501}"))
                .andExpect(status().isBadRequest());
        call(mgr, post("/api/events"), base.formatted(a.id(), start, "{\"interval\":2}"))
                .andExpect(status().isBadRequest());
        call(mgr, post("/api/events"), """
                {"communityId":%s,"title":"x","startAt":"%s","timeZone":"Mars/Olympus"}""".formatted(a.id(), start))
                .andExpect(status().isBadRequest());
        // Cancelling an occurrence of a one-off event makes no sense
        String oneOff = createEvent(mgr, a, "單次", NOW.plus(Duration.ofDays(2)), null);
        call(mgr, delete("/api/events/{id}/occurrences", oneOff).param("start", NOW.plus(Duration.ofDays(2)).toString()), null)
                .andExpect(status().isBadRequest());
    }

    private String createEventJson(String token, String json) throws Exception {
        return idOf(call(token, post("/api/events"), json).andExpect(status().isCreated()));
    }

    private String createEvent(String token, Created community, String title, Instant start, Instant end)
            throws Exception {
        String endJson = end == null ? "null" : "\"" + end + "\"";
        return idOf(call(token, post("/api/events"), """
                {"communityId":%s,"title":"%s","startAt":"%s","endAt":%s}"""
                .formatted(community.id(), title, start, endJson))
                .andExpect(status().isCreated()));
    }

    private static List<String> titles(String json) {
        return JsonPath.read(json, "$[*].title");
    }
}
