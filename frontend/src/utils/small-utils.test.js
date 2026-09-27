import { describe, expect, it } from 'vitest'
import { createLatest } from './latest'
import { formatFileSize, formatMoney } from './money'
import { describeRecurrence } from './recurrence'

describe('createLatest', () => {
  it('only the most recent request is current', () => {
    const latest = createLatest()
    const first = latest.begin()
    const second = latest.begin()
    expect(first()).toBe(false)
    expect(second()).toBe(true)
  })

  it('invalidate makes everything in flight stale', () => {
    const latest = createLatest()
    const pending = latest.begin()
    latest.invalidate()
    expect(pending()).toBe(false)
    expect(latest.begin()()).toBe(true)
  })
})

describe('formatMoney', () => {
  it('formats whole dollars with separators and sign', () => {
    expect(formatMoney(5000)).toBe('NT$ 5,000')
    expect(formatMoney(-700)).toBe('-NT$ 700')
    expect(formatMoney(0)).toBe('NT$ 0')
    expect(formatMoney(null)).toBe('—')
  })

  it('formats file sizes', () => {
    expect(formatFileSize(100)).toBe('1 KB')
    expect(formatFileSize(300 * 1024)).toBe('300 KB')
    expect(formatFileSize(2.5 * 1024 * 1024)).toBe('2.5 MB')
  })
})

describe('describeRecurrence', () => {
  it('describes one-off events as empty', () => {
    expect(describeRecurrence(null)).toBe('')
  })

  it('describes simple and interval rules', () => {
    expect(describeRecurrence({ frequency: 'DAILY' })).toBe('每天')
    expect(describeRecurrence({ frequency: 'MONTHLY', interval: 3 })).toBe('每 3 個月')
  })

  it('lists weekly days in week order, whatever order they come in', () => {
    const r = { frequency: 'WEEKLY', interval: 2, daysOfWeek: ['THURSDAY', 'TUESDAY'], count: 10 }
    expect(describeRecurrence(r)).toBe('每 2 週的星期二、四，共 10 次')
  })

  it('shows the end date', () => {
    expect(describeRecurrence({ frequency: 'YEARLY', until: '2030-12-31' })).toBe('每年，至 2030/12/31')
  })
})
