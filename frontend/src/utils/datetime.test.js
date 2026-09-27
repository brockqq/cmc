import { describe, expect, it } from 'vitest'
import { addDays, dayKey, formatEventRange, fromLocalInput, isSameDay, startOfDay, toLocalInput } from './datetime'

// vitest.config.js pins TZ=Asia/Taipei (UTC+8, no DST)

describe('day arithmetic', () => {
  it('keys days by local calendar date', () => {
    expect(dayKey(new Date('2026-09-30T16:30:00Z'))).toBe('2026-10-01') // 00:30 in Taipei
  })

  it('adds calendar days across month and year ends', () => {
    expect(dayKey(addDays(new Date(2026, 0, 31), 1))).toBe('2026-02-01')
    expect(dayKey(addDays(new Date(2026, 11, 31), 1))).toBe('2027-01-01')
    expect(dayKey(addDays(new Date(2026, 2, 1), -1))).toBe('2026-02-28')
  })

  it('startOfDay drops the time and isSameDay ignores it', () => {
    const d = new Date(2026, 8, 30, 23, 59)
    expect(startOfDay(d)).toEqual(new Date(2026, 8, 30))
    expect(isSameDay(d, new Date(2026, 8, 30, 0, 0))).toBe(true)
    expect(isSameDay(d, new Date(2026, 9, 1, 0, 0))).toBe(false)
  })
})

describe('datetime-local inputs', () => {
  it('round-trips through local time', () => {
    const iso = '2026-10-01T11:00:00.000Z'
    expect(toLocalInput(iso)).toBe('2026-10-01T19:00')
    expect(fromLocalInput('2026-10-01T19:00')).toBe(iso)
  })

  it('treats empty as no value', () => {
    expect(toLocalInput(null)).toBe('')
    expect(fromLocalInput('')).toBeNull()
  })
})

describe('formatEventRange', () => {
  it('shows only the end time on the same day', () => {
    const text = formatEventRange('2026-09-30T06:00:00Z', '2026-09-30T08:00:00Z')
    expect(text).toMatch(/14:00 – 16:00$/)
  })

  it('repeats the date when the event spans days', () => {
    const text = formatEventRange('2026-09-30T06:00:00Z', '2026-10-02T04:00:00Z')
    expect(text).toMatch(/14:00 – .*10.*2.* 12:00$/)
  })

  it('shows just the start without an end', () => {
    expect(formatEventRange('2026-09-30T06:00:00Z')).toMatch(/14:00$/)
  })
})
