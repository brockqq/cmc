// Matches java.time.DayOfWeek names; index 0 = Sunday to line up with Date#getDay()
export const DAYS = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY']
export const DAY_LABELS = ['日', '一', '二', '三', '四', '五', '六']

export const FREQUENCY_OPTIONS = [
  { value: '', label: '不重複' },
  { value: 'DAILY', label: '每天' },
  { value: 'WEEKLY', label: '每週' },
  { value: 'MONTHLY', label: '每月' },
  { value: 'YEARLY', label: '每年' },
]

export const UNIT_LABELS = { DAILY: '天', WEEKLY: '週', MONTHLY: '個月', YEARLY: '年' }

/** Human-readable rule, e.g. "每 2 週的星期二、四，共 10 次". Returns '' for one-off events. */
export function describeRecurrence(r) {
  if (!r) return ''
  const interval = r.interval ?? 1
  const base =
    interval === 1
      ? { DAILY: '每天', WEEKLY: '每週', MONTHLY: '每月', YEARLY: '每年' }[r.frequency]
      : `每 ${interval} ${UNIT_LABELS[r.frequency]}`

  let text = base
  if (r.frequency === 'WEEKLY' && r.daysOfWeek?.length) {
    const sorted = [...r.daysOfWeek].sort((a, b) => DAYS.indexOf(a) - DAYS.indexOf(b))
    text += `的星期${sorted.map((d) => DAY_LABELS[DAYS.indexOf(d)]).join('、')}`
  }
  if (r.until) text += `，至 ${r.until.replaceAll('-', '/')}`
  else if (r.count) text += `，共 ${r.count} 次`
  return text
}

/** IANA zone of this browser, sent with events so the server repeats them in local time. */
export const browserTimeZone = () => Intl.DateTimeFormat().resolvedOptions().timeZone
