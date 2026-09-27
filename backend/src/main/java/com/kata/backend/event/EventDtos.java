package com.kata.backend.event;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

public final class EventDtos {

    private EventDtos() {
    }

    /** Recurrence rule as sent/received by clients. {@code until} and {@code count} are mutually exclusive. */
    public record RecurrenceDto(
            @NotNull(message = "請選擇重複頻率") Frequency frequency,
            @Min(value = 1, message = "間隔需為 1-99") @Max(value = 99, message = "間隔需為 1-99") Integer interval,
            List<DayOfWeek> daysOfWeek,
            LocalDate until,
            @Min(value = 1, message = "次數需為 1-500") @Max(value = 500, message = "次數需為 1-500") Integer count
    ) {

        Recurrence toEntity() {
            String days = frequency == Frequency.WEEKLY && daysOfWeek != null && !daysOfWeek.isEmpty()
                    ? Recurrence.joinDays(EnumSet.copyOf(daysOfWeek)) : null;
            return new Recurrence(frequency, interval == null ? 1 : interval, days, until, count);
        }

        static RecurrenceDto from(Recurrence r) {
            if (r == null) {
                return null;
            }
            return new RecurrenceDto(r.getFrequency(), r.intervalOrDefault(), List.copyOf(r.daysOfWeek()),
                    r.getUntil(), r.getCount());
        }
    }

    /** Fields shared by create and update. */
    public interface EventFields {
        String title();

        String description();

        String location();

        Instant startAt();

        Instant endAt();

        String timeZone();

        RecurrenceDto recurrence();
    }

    public record CreateRequest(
            @NotNull(message = "請選擇社區") Long communityId,
            @NotBlank(message = "請輸入活動名稱") @Size(max = 200, message = "活動名稱過長") String title,
            @Size(max = 2000, message = "說明過長") String description,
            @Size(max = 200, message = "地點過長") String location,
            @NotNull(message = "請選擇開始時間") Instant startAt,
            Instant endAt,
            /** IANA zone, e.g. "Asia/Taipei"; defaults to the server's app.default-time-zone. */
            String timeZone,
            /** Null for a one-off event. */
            @Valid RecurrenceDto recurrence
    ) implements EventFields {
    }

    /** Updates the whole series. The community cannot be changed after creation. */
    public record UpdateRequest(
            @NotBlank(message = "請輸入活動名稱") @Size(max = 200, message = "活動名稱過長") String title,
            @Size(max = 2000, message = "說明過長") String description,
            @Size(max = 200, message = "地點過長") String location,
            @NotNull(message = "請選擇開始時間") Instant startAt,
            Instant endAt,
            String timeZone,
            @Valid RecurrenceDto recurrence,
            /** The {@code version} the edit is based on; when given, a newer event is refused with 409. */
            Long version
    ) implements EventFields {
    }

    /** Changes to one occurrence of a series. Fields equal to the series' values keep following the series. */
    public record OccurrenceRequest(
            @NotBlank(message = "請輸入活動名稱") @Size(max = 200, message = "活動名稱過長") String title,
            @Size(max = 2000, message = "說明過長") String description,
            @Size(max = 200, message = "地點過長") String location,
            @NotNull(message = "請選擇開始時間") Instant startAt,
            Instant endAt,
            /** The {@code version} the edit is based on; when given, a newer event is refused with 409. */
            Long version
    ) {
    }

    /**
     * One occurrence.
     * <ul>
     *   <li>{@code startAt}/{@code endAt}: when this occurrence actually happens</li>
     *   <li>{@code originalStart}: the slot the series placed it in — identifies the occurrence for
     *       per-occurrence edit/cancel; differs from {@code startAt} only when {@code modified}</li>
     *   <li>{@code seriesStartAt}/{@code seriesEndAt}: the first occurrence's times (for editing the series)</li>
     * </ul>
     */
    public record EventResponse(Long id, Long communityId, String communityName, String title, String description,
                                String location, Instant startAt, Instant endAt, Instant originalStart,
                                boolean modified, Instant seriesStartAt, Instant seriesEndAt, String timeZone,
                                RecurrenceDto recurrence, String createdBy, boolean canEdit, long version) {

        static EventResponse occurrence(CalendarEvent e, Instant occurrenceStart, boolean canEdit) {
            Instant occurrenceEnd = e.getEndAt() == null ? null : occurrenceStart.plus(e.duration());
            return new EventResponse(e.getId(), e.getCommunity().getId(), e.getCommunity().getName(), e.getTitle(),
                    e.getDescription(), e.getLocation(), occurrenceStart, occurrenceEnd, occurrenceStart, false,
                    e.getStartAt(), e.getEndAt(), e.getTimeZone(), RecurrenceDto.from(e.getRecurrence()),
                    e.getCreatedBy().getUsername(), canEdit, e.getVersion());
        }

        static EventResponse modified(OccurrenceOverride o, boolean canEdit) {
            CalendarEvent e = o.getEvent();
            return new EventResponse(e.getId(), e.getCommunity().getId(), e.getCommunity().getName(),
                    o.getTitle() != null ? o.getTitle() : e.getTitle(),
                    ownOrSeries(o.getDescription(), e.getDescription()),
                    ownOrSeries(o.getLocation(), e.getLocation()),
                    o.getStartAt(), o.getEndAt(), o.getOriginalStart(), true,
                    e.getStartAt(), e.getEndAt(), e.getTimeZone(), RecurrenceDto.from(e.getRecurrence()),
                    e.getCreatedBy().getUsername(), canEdit, e.getVersion());
        }

        static EventResponse series(CalendarEvent e, boolean canEdit) {
            return occurrence(e, e.getStartAt(), canEdit);
        }

        /** Override value: null follows the series, "" means explicitly none. */
        private static String ownOrSeries(String own, String series) {
            if (own == null) {
                return series;
            }
            return own.isEmpty() ? null : own;
        }
    }
}
