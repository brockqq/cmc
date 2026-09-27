package com.kata.backend.event;

import com.kata.backend.common.ApiException;
import com.kata.backend.community.CommunityAccess;
import com.kata.backend.event.EventDtos.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@Service
public class CalendarEventService {

    static final Duration DEFAULT_RANGE = Duration.ofDays(90);
    static final Duration MAX_RANGE = Duration.ofDays(400);
    static final int MAX_UNTIL_YEARS = 10;
    /** Event times must fall in this range; it bounds how far any series expansion can reach. */
    static final Instant EARLIEST = Instant.parse("2000-01-01T00:00:00Z");
    static final Instant LATEST = Instant.parse("2100-01-01T00:00:00Z");

    private final CalendarEventRepository eventRepository;
    private final OccurrenceOverrideRepository overrideRepository;
    private final CommunityAccess access;
    private final String defaultTimeZone;

    public CalendarEventService(CalendarEventRepository eventRepository,
                                OccurrenceOverrideRepository overrideRepository, CommunityAccess access,
                                @Value("${app.default-time-zone}") String defaultTimeZone) {
        this.eventRepository = eventRepository;
        this.overrideRepository = overrideRepository;
        this.access = access;
        this.defaultTimeZone = defaultTimeZone;
    }

    /**
     * Occurrences overlapping [from, to) in the user's communities, or in one community when {@code communityId}
     * is given, sorted by start time. Defaults to the upcoming 90 days.
     */
    @Transactional(readOnly = true)
    public List<EventResponse> list(String username, Long communityId, Instant from, Instant to) {
        Instant start = from != null ? from : Instant.now();
        Instant end = to != null ? to : start.plus(DEFAULT_RANGE);
        if (!end.isAfter(start) || Duration.between(start, end).compareTo(MAX_RANGE) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "查詢區間需大於 0 且不超過 400 天");
        }

        CommunityAccess.Viewer viewer = access.viewer(username);
        Set<Long> communityIds;
        if (communityId != null) {
            access.requireMember(communityId, username);
            communityIds = Set.of(communityId);
        } else {
            communityIds = viewer.memberOf();
        }
        if (communityIds.isEmpty()) {
            return List.of();
        }

        List<CalendarEvent> candidates = eventRepository.findCandidates(communityIds, start, end);
        // Occurrences edited individually are left out of the regular expansion...
        Map<Long, Set<Instant>> modifiedSlots = candidates.isEmpty() ? Map.of()
                : overrideRepository.findByEventIdIn(candidates.stream().map(CalendarEvent::getId).toList()).stream()
                .collect(Collectors.groupingBy(o -> o.getEvent().getId(),
                        Collectors.mapping(OccurrenceOverride::getOriginalStart, Collectors.toSet())));

        List<EventResponse> result = new ArrayList<>();
        for (CalendarEvent e : candidates) {
            boolean canEdit = viewer.canManage(e.getCommunity().getId());
            Set<Instant> modified = modifiedSlots.getOrDefault(e.getId(), Set.of());
            for (Instant occurrence : RecurrenceExpander.occurrences(
                    e.getStartAt(), e.duration(), e.zone(), e.getRecurrence(), start, end)) {
                if (!e.getExclusions().contains(occurrence) && !modified.contains(occurrence)) {
                    result.add(EventResponse.occurrence(e, occurrence, canEdit));
                }
            }
        }
        // ...and added back at their new times, even if moved in from outside the range
        for (OccurrenceOverride o : overrideRepository.findOverlapping(communityIds, start, end)) {
            result.add(EventResponse.modified(o, viewer.canManage(o.getEvent().getCommunity().getId())));
        }
        result.sort(Comparator.comparing(EventResponse::startAt).thenComparing(EventResponse::id));
        return result;
    }

    /** The event/series itself (first occurrence, series-level fields) — what an edit form should start from. */
    @Transactional(readOnly = true)
    public EventResponse get(String username, Long id) {
        CalendarEvent e = eventRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "找不到此活動"));
        CommunityAccess.Viewer viewer = access.viewer(username);
        if (!viewer.canView(e.getCommunity().getId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "找不到此活動");
        }
        return EventResponse.series(e, viewer.canManage(e.getCommunity().getId()));
    }

    @Transactional
    public EventResponse create(String username, CreateRequest request) {
        CommunityAccess.Context ctx = access.requireManager(request.communityId(), username);
        CalendarEvent e = new CalendarEvent();
        e.setCommunity(ctx.community());
        e.setCreatedBy(ctx.user());
        apply(e, request);
        return EventResponse.series(eventRepository.save(e), true);
    }

    /**
     * Updates the whole series. Individually cancelled/edited occurrences are kept as long as the series still
     * has an occurrence in that slot; ones orphaned by a time or recurrence change are dropped.
     */
    @Transactional
    public EventResponse update(String username, Long id, UpdateRequest request) {
        CalendarEvent e = findEditable(username, id, request.version());
        Instant oldStart = e.getStartAt();
        Duration oldDuration = e.duration();
        boolean oldHadEnd = e.getEndAt() != null;
        ZoneId oldZone = e.zone();
        RecurrenceDto oldRule = RecurrenceDto.from(e.getRecurrence());

        apply(e, request);
        ZonedDateTime newStart = e.getStartAt().atZone(e.zone());
        boolean sameRule = e.isRecurring() && oldRule != null && oldRule.equals(RecurrenceDto.from(e.getRecurrence()));
        if (sameRule && oldZone.equals(e.zone()) && !oldStart.equals(e.getStartAt())
                && oldStart.atZone(oldZone).toLocalDate().equals(newStart.toLocalDate())) {
            // Same series at another time of day: cancellations and edits move along instead of being dropped
            e.moveExceptionsToTimeOfDay(newStart.toLocalTime(),
                    slot -> oldHadEnd ? slot.plus(oldDuration) : null);
        }
        e.pruneStaleExceptions();
        return saved(EventResponse::series, e);
    }

    @Transactional
    public void delete(String username, Long id, Long version) {
        eventRepository.delete(findEditable(username, id, version));
    }

    /** Cancels a single occurrence of a recurring series (including one that was edited individually). */
    @Transactional
    public void cancelOccurrence(String username, Long id, Instant originalStart, Long version) {
        Instant slot = seconds(originalStart);
        CalendarEvent e = findOccurrence(username, id, slot, version, "單次活動請直接刪除");
        e.findOverride(slot).ifPresent(e.getOverrides()::remove);
        e.getExclusions().add(slot);
        e.touch();
    }

    /**
     * Changes one occurrence only ("edit this event"). Title/description/location equal to the series' values
     * keep following the series; times are always fixed for this occurrence.
     */
    @Transactional
    public EventResponse updateOccurrence(String username, Long id, Instant originalStart, OccurrenceRequest request) {
        Instant slot = seconds(originalStart);
        CalendarEvent e = findOccurrence(username, id, slot, request.version(), "單次活動請直接編輯");
        Instant startAt = seconds(request.startAt());
        Instant endAt = seconds(request.endAt());
        checkTimes(startAt, endAt);
        OccurrenceOverride o = e.overrideFor(slot);
        o.setStartAt(startAt);
        o.setEndAt(endAt);
        o.setTitle(differing(request.title().trim(), e.getTitle()));
        o.setDescription(differing(blankToEmpty(request.description()), e.getDescription()));
        o.setLocation(differing(blankToEmpty(request.location()), e.getLocation()));
        e.touch();
        eventRepository.flush(); // assign the override's id and the new version
        return EventResponse.modified(o, true);
    }

    /** Drops an occurrence's individual changes so it follows the series again. */
    @Transactional
    public EventResponse resetOccurrence(String username, Long id, Instant originalStart, Long version) {
        Instant slot = seconds(originalStart);
        CalendarEvent e = findOccurrence(username, id, slot, version, "單次活動沒有個別調整");
        OccurrenceOverride o = e.findOverride(slot)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "此場次沒有個別調整"));
        e.getOverrides().remove(o);
        return saved((ev, canEdit) -> EventResponse.occurrence(ev, slot, canEdit), e);
    }

    /** Marks the event changed and flushes, so the response carries its new version. */
    private EventResponse saved(BiFunction<CalendarEvent, Boolean, EventResponse> toResponse, CalendarEvent e) {
        e.touch();
        eventRepository.flush();
        return toResponse.apply(e, true);
    }

    /** Loads an editable recurring series and checks that it has a (not cancelled) occurrence at that slot. */
    private CalendarEvent findOccurrence(String username, Long id, Instant originalStart, Long version,
                                         String notRecurringMessage) {
        CalendarEvent e = findEditable(username, id, version);
        if (!e.isRecurring()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, notRecurringMessage);
        }
        if (!e.hasOccurrenceAt(originalStart) || e.getExclusions().contains(originalStart)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "此時間沒有這個活動");
        }
        return e;
    }

    /**
     * Value to store on an override: null when it matches the series (so it keeps following it),
     * "" when the series has a value but this occurrence explicitly has none.
     */
    private static String differing(String value, String seriesValue) {
        String normalizedSeries = seriesValue == null ? "" : seriesValue;
        return value.equals(normalizedSeries) ? null : value;
    }

    private static String blankToEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    /**
     * Loads an event for changing: locked (see {@link CalendarEventRepository#lockById}) and, when the client says
     * which version its change is based on, refused if someone else changed the event since.
     */
    private CalendarEvent findEditable(String username, Long id, Long expectedVersion) {
        CalendarEvent e = eventRepository.lockById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "找不到此活動"));
        CommunityAccess.Viewer viewer = access.viewer(username);
        Long communityId = e.getCommunity().getId();
        if (!viewer.canView(communityId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "找不到此活動");
        }
        if (!viewer.canManage(communityId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "需要社區管理員權限");
        }
        if (expectedVersion != null && expectedVersion != e.getVersion()) {
            throw new ApiException(HttpStatus.CONFLICT, "此活動剛被其他人修改過，請重新整理後再試");
        }
        return e;
    }

    private void apply(CalendarEvent e, EventFields f) {
        Instant startAt = seconds(f.startAt());
        Instant endAt = seconds(f.endAt());
        checkTimes(startAt, endAt);
        ZoneId zone = parseZone(f.timeZone());
        Recurrence recurrence = f.recurrence() == null ? null : f.recurrence().toEntity();
        if (recurrence != null) {
            validate(recurrence, startAt, zone);
        }
        e.setTitle(f.title().trim());
        e.setDescription(blankToNull(f.description()));
        e.setLocation(blankToNull(f.location()));
        e.setStartAt(startAt);
        e.setEndAt(endAt);
        e.setTimeZone(zone.getId());
        e.setRecurrence(recurrence);
        e.refreshLastEnd();
    }

    /**
     * Event times are kept to whole seconds. Occurrences are identified by their exact start, and sub-second parts
     * could be rounded by the database (H2 keeps microseconds), so a stored slot would not match the requested one.
     */
    private static Instant seconds(Instant t) {
        return t == null ? null : t.truncatedTo(ChronoUnit.SECONDS);
    }

    private static void checkTimes(Instant startAt, Instant endAt) {
        if (endAt != null && endAt.isBefore(startAt)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "結束時間不可早於開始時間");
        }
        Instant last = endAt == null ? startAt : endAt;
        if (startAt.isBefore(EARLIEST) || !last.isBefore(LATEST)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "活動時間需在 2000 年至 2099 年之間");
        }
    }

    private static void validate(Recurrence r, Instant startAt, ZoneId zone) {
        if (r.getUntil() != null && r.getCount() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "結束日期與重複次數只能擇一");
        }
        var startDate = startAt.atZone(zone).toLocalDate();
        if (r.getUntil() != null) {
            if (r.getUntil().isBefore(startDate)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "重複結束日期不可早於開始日期");
            }
            if (r.getUntil().isAfter(startDate.plusYears(MAX_UNTIL_YEARS))) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "重複結束日期最多為開始後 " + MAX_UNTIL_YEARS + " 年");
            }
        }
    }

    private ZoneId parseZone(String timeZone) {
        try {
            return ZoneId.of(timeZone == null || timeZone.isBlank() ? defaultTimeZone : timeZone);
        } catch (DateTimeException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "無效的時區：" + timeZone);
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
