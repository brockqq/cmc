import { addDays, dayKey, startOfDay } from './datetime'

export const GRID_DAYS = 42

/** First day of the 6-week grid showing `month` (a Date in that month): the Sunday on/before the 1st. */
export function gridStartFor(month) {
  const first = new Date(month.getFullYear(), month.getMonth(), 1)
  return addDays(first, -first.getDay())
}

/** The 42 days of the grid starting at `gridStart`. */
export const gridDays = (gridStart) => Array.from({ length: GRID_DAYS }, (_, i) => addDays(gridStart, i))

/**
 * dayKey → events touching that day within the grid. Multi-day events appear on every day they span;
 * each day keeps the events' original order.
 */
export function groupByDay(events, gridStart) {
  const map = {}
  const gridEnd = addDays(gridStart, GRID_DAYS)
  for (const e of events) {
    let d = startOfDay(new Date(e.startAt))
    const last = startOfDay(new Date(e.endAt ?? e.startAt))
    for (; d <= last && d < gridEnd; d = addDays(d, 1)) {
      if (d >= gridStart) (map[dayKey(d)] ??= []).push(e)
    }
  }
  return map
}

/** Occurrences of a series share the event id, so key them by id + original slot. */
export const occurrenceKey = (e) => `${e.id}@${e.originalStart}`
