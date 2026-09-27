import { describe, expect, it } from 'vitest'
import { GRID_DAYS, gridDays, gridStartFor, groupByDay, occurrenceKey } from './calendar'
import { dayKey } from './datetime'

describe('month grid', () => {
  it('starts on the Sunday on or before the 1st and spans 6 weeks', () => {
    // 2026-10-01 is a Thursday
    const start = gridStartFor(new Date(2026, 9, 15))
    expect(dayKey(start)).toBe('2026-09-27')
    const days = gridDays(start)
    expect(days).toHaveLength(GRID_DAYS)
    expect(dayKey(days.at(-1))).toBe('2026-11-07')
  })

  it('starts on the 1st itself when it is a Sunday', () => {
    expect(dayKey(gridStartFor(new Date(2026, 1, 10)))).toBe('2026-02-01')
  })
})

describe('groupByDay', () => {
  const gridStart = gridStartFor(new Date(2026, 9, 1)) // 2026-09-27 .. 2026-11-07
  const ev = (id, startAt, endAt) => ({ id, startAt, endAt })

  it('puts an event on every day it spans', () => {
    const map = groupByDay([ev(1, '2026-10-01T02:00:00Z', '2026-10-03T02:00:00Z')], gridStart)
    expect(Object.keys(map)).toEqual(['2026-10-01', '2026-10-02', '2026-10-03'])
  })

  it('clips events running outside the grid', () => {
    const map = groupByDay([ev(1, '2026-09-20T02:00:00Z', '2026-09-28T02:00:00Z')], gridStart)
    expect(Object.keys(map)).toEqual(['2026-09-27', '2026-09-28'])
    const late = groupByDay([ev(2, '2026-11-06T02:00:00Z', '2026-11-20T02:00:00Z')], gridStart)
    expect(Object.keys(late)).toEqual(['2026-11-06', '2026-11-07'])
  })

  it('keeps the given order within a day', () => {
    const map = groupByDay([ev(1, '2026-10-01T01:00:00Z'), ev(2, '2026-10-01T03:00:00Z')], gridStart)
    expect(map['2026-10-01'].map((e) => e.id)).toEqual([1, 2])
  })
})

it('keys occurrences of a series by id and original start', () => {
  const a = { id: 7, originalStart: '2026-10-01T11:00:00Z' }
  const b = { id: 7, originalStart: '2026-10-08T11:00:00Z' }
  expect(occurrenceKey(a)).not.toBe(occurrenceKey(b))
})
