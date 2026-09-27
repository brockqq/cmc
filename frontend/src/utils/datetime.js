const pad = (n) => String(n).padStart(2, '0')

export const WEEKDAYS = ['日', '一', '二', '三', '四', '五', '六']

/** Local calendar key, e.g. "2026-09-25". */
export const dayKey = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`

export const startOfDay = (d) => new Date(d.getFullYear(), d.getMonth(), d.getDate())

export const addDays = (d, n) => new Date(d.getFullYear(), d.getMonth(), d.getDate() + n)

export const isSameDay = (a, b) => dayKey(a) === dayKey(b)

export const formatTime = (d) => `${pad(d.getHours())}:${pad(d.getMinutes())}`

export const formatDate = (iso) =>
  new Date(iso).toLocaleDateString('zh-TW', { month: 'numeric', day: 'numeric', weekday: 'short' })

export const formatDateTime = (iso) =>
  new Date(iso).toLocaleString('zh-TW', {
    year: 'numeric',
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  })

/** "9/30(二) 14:00 – 16:00" or across days "9/30(二) 14:00 – 10/2(四) 12:00". */
export function formatEventRange(startIso, endIso) {
  const start = new Date(startIso)
  const head = `${formatDate(startIso)} ${formatTime(start)}`
  if (!endIso) return head
  const end = new Date(endIso)
  return isSameDay(start, end)
    ? `${head} – ${formatTime(end)}`
    : `${head} – ${formatDate(endIso)} ${formatTime(end)}`
}

/** ISO string → value for <input type="datetime-local"> in local time. */
export function toLocalInput(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  return `${dayKey(d)}T${formatTime(d)}`
}

/** <input type="datetime-local"> value → ISO instant (or null when empty). */
export const fromLocalInput = (value) => (value ? new Date(value).toISOString() : null)
