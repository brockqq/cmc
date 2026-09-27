package com.kata.backend.event;

import com.kata.backend.event.EventDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class CalendarEventController {

    private final CalendarEventService eventService;

    @GetMapping
    public List<EventResponse> list(@AuthenticationPrincipal Jwt jwt,
                                    @RequestParam(required = false) Long communityId,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return eventService.list(jwt.getSubject(), communityId, from, to);
    }

    /** The event/series itself with series-level fields (not an individual occurrence). */
    @GetMapping("/{id}")
    public EventResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return eventService.get(jwt.getSubject(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateRequest request) {
        return eventService.create(jwt.getSubject(), request);
    }

    @PutMapping("/{id}")
    public EventResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                @Valid @RequestBody UpdateRequest request) {
        return eventService.update(jwt.getSubject(), id, request);
    }

    /** Deletes the event, or the whole series for a recurring event. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                       @RequestParam(required = false) Long version) {
        eventService.delete(jwt.getSubject(), id, version);
    }

    /**
     * Changes one occurrence only, identified by its original start ({@code originalStart} in responses).
     * Every change accepts the {@code version} the client last saw (body or {@code ?version=}); if the event has
     * changed since, the request is refused with 409 instead of overwriting the other change.
     */
    @PutMapping("/{id}/occurrences")
    public EventResponse updateOccurrence(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
                                          @Valid @RequestBody OccurrenceRequest request) {
        return eventService.updateOccurrence(jwt.getSubject(), id, start, request);
    }

    /** Reverts an individually edited occurrence to the series' settings. */
    @DeleteMapping("/{id}/occurrences/changes")
    public EventResponse resetOccurrence(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
                                         @RequestParam(required = false) Long version) {
        return eventService.resetOccurrence(jwt.getSubject(), id, start, version);
    }

    /** Cancels one occurrence of a recurring series, identified by its original start. */
    @DeleteMapping("/{id}/occurrences")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelOccurrence(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
                                 @RequestParam(required = false) Long version) {
        eventService.cancelOccurrence(jwt.getSubject(), id, start, version);
    }
}
