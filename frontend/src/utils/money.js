/** "NT$ 5,000" / "-NT$ 700" (whole dollars). */
export function formatMoney(amount) {
  if (amount == null) return '—'
  const abs = Math.abs(amount).toLocaleString('zh-TW')
  return `${amount < 0 ? '-' : ''}NT$ ${abs}`
}

export const formatFileSize = (bytes) =>
  bytes < 1024 * 1024 ? `${Math.max(1, Math.round(bytes / 1024))} KB` : `${(bytes / 1024 / 1024).toFixed(1)} MB`

export const MAX_RECEIPT_BYTES = 5 * 1024 * 1024
export const RECEIPT_ACCEPT = 'image/jpeg,image/png,image/gif,image/webp,application/pdf'
