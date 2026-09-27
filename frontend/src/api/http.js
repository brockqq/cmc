const TOKEN_KEY = 'kata.token'

export const tokenStorage = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

export class ApiError extends Error {
  constructor(status, message, fieldErrors = {}) {
    super(message)
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

let onUnauthorized = () => {}

/** Register a callback invoked when an authenticated request returns 401 (e.g. token expired). */
export function setUnauthorizedHandler(handler) {
  onUnauthorized = handler
}

/**
 * Calls the API. `body` is sent as JSON, or as-is when it is FormData (file uploads).
 * With `responseType: 'blob'` the response body is returned as a Blob (file downloads).
 */
export async function request(path, { method = 'GET', body, responseType = 'json' } = {}) {
  const headers = { Accept: responseType === 'blob' ? '*/*' : 'application/json' }
  const token = tokenStorage.get()
  if (token) headers.Authorization = `Bearer ${token}`
  const isForm = body instanceof FormData
  // For FormData the browser sets the multipart Content-Type (with boundary) itself
  if (body !== undefined && !isForm) headers['Content-Type'] = 'application/json'

  const res = await fetch(`/api${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : isForm ? body : JSON.stringify(body),
  })

  if (res.ok && responseType === 'blob') return res.blob()

  const data = res.headers.get('content-type')?.includes('json') ? await res.json() : null

  if (!res.ok) {
    if (res.status === 401 && token) onUnauthorized()
    const fallback = res.status === 403 ? '權限不足' : `請求失敗 (${res.status})`
    throw new ApiError(res.status, data?.detail ?? fallback, data?.errors ?? {})
  }
  return data
}

const occurrenceParam = (originalStart) => `start=${encodeURIComponent(new Date(originalStart).toISOString())}`
// The event version the change is based on; the server refuses it (409) if someone changed the event since
const versionParam = (version) => (version != null ? `version=${version}` : '')
const join = (...params) => params.filter(Boolean).join('&')

export const api = {
  register: (payload) => request('/auth/register', { method: 'POST', body: payload }),
  login: (payload) => request('/auth/login', { method: 'POST', body: payload }),
  me: () => request('/users/me'),
  changePassword: (payload) => request('/users/me/password', { method: 'PUT', body: payload }),
  communities: {
    mine: () => request('/communities/mine'),
    join: (inviteCode) => request('/communities/join', { method: 'POST', body: { inviteCode } }),
    get: (id) => request(`/communities/${id}`),
    members: (id) => request(`/communities/${id}/members`),
    updateMemberRole: (id, userId, role) =>
      request(`/communities/${id}/members/${userId}/role`, { method: 'PUT', body: { role } }),
    removeMember: (id, userId) => request(`/communities/${id}/members/${userId}`, { method: 'DELETE' }),
    regenerateInviteCode: (id) => request(`/communities/${id}/invite-code`, { method: 'POST' }),
  },
  announcements: {
    /** params: { communityId?, platform?, page? (0-based), size? } → { items, page, size, totalItems, totalPages } */
    list: (params = {}) => {
      const query = new URLSearchParams()
      if (params.communityId) query.set('communityId', params.communityId)
      if (params.platform) query.set('platform', 'true')
      if (params.page != null) query.set('page', params.page)
      if (params.size != null) query.set('size', params.size)
      const qs = query.toString()
      return request(`/announcements${qs ? `?${qs}` : ''}`)
    },
    create: (payload) => request('/announcements', { method: 'POST', body: payload }),
    update: (id, payload) => request(`/announcements/${id}`, { method: 'PUT', body: payload }),
    remove: (id) => request(`/announcements/${id}`, { method: 'DELETE' }),
  },
  events: {
    /** All params optional: { communityId, from, to } (from/to are Date or ISO strings). */
    list: (params = {}) => {
      const query = new URLSearchParams()
      if (params.communityId) query.set('communityId', params.communityId)
      if (params.from) query.set('from', new Date(params.from).toISOString())
      if (params.to) query.set('to', new Date(params.to).toISOString())
      const qs = query.toString()
      return request(`/events${qs ? `?${qs}` : ''}`)
    },
    /** The event/series itself (series-level fields), for the edit-series form. */
    get: (id) => request(`/events/${id}`),
    create: (payload) => request('/events', { method: 'POST', body: payload }),
    /** payload may carry `version` (from the event as last loaded) to refuse overwriting someone else's change. */
    update: (id, payload) => request(`/events/${id}`, { method: 'PUT', body: payload }),
    /** Deletes a one-off event, or a whole recurring series. */
    remove: (id, version) => request(`/events/${id}?${versionParam(version)}`, { method: 'DELETE' }),
    // Single occurrences of a series are identified by `originalStart` from the list response
    /** Changes one occurrence only: { title, description, location, startAt, endAt, version? }. */
    updateOccurrence: (id, originalStart, payload) =>
      request(`/events/${id}/occurrences?${occurrenceParam(originalStart)}`, { method: 'PUT', body: payload }),
    /** Reverts an individually changed occurrence to the series' settings. */
    resetOccurrence: (id, originalStart, version) =>
      request(`/events/${id}/occurrences/changes?${join(occurrenceParam(originalStart), versionParam(version))}`, {
        method: 'DELETE',
      }),
    /** Cancels one occurrence of a recurring series. */
    cancelOccurrence: (id, originalStart, version) =>
      request(`/events/${id}/occurrences?${join(occurrenceParam(originalStart), versionParam(version))}`, {
        method: 'DELETE',
      }),
  },
  pettyCash: {
    summary: (communityId) => request(`/communities/${communityId}/petty-cash`),
    setFund: (communityId, amount) =>
      request(`/communities/${communityId}/petty-cash/fund`, { method: 'PUT', body: { amount } }),
    replenish: (communityId, note) =>
      request(`/communities/${communityId}/petty-cash/replenish`, { method: 'POST', body: { note } }),
    /** `period` = period number; omit for the open period. */
    expenses: (communityId, period) =>
      request(`/communities/${communityId}/petty-cash/expenses${period ? `?period=${period}` : ''}`),
    /** Audit trail of a period (default: the open one), newest first. */
    log: (communityId, period) =>
      request(`/communities/${communityId}/petty-cash/log${period ? `?period=${period}` : ''}`),
    createExpense: (communityId, payload) =>
      request(`/communities/${communityId}/petty-cash/expenses`, { method: 'POST', body: payload }),
    updateExpense: (id, payload) => request(`/petty-cash/expenses/${id}`, { method: 'PUT', body: payload }),
    deleteExpense: (id) => request(`/petty-cash/expenses/${id}`, { method: 'DELETE' }),
    uploadAttachment: (id, file) => {
      const form = new FormData()
      form.append('file', file)
      return request(`/petty-cash/expenses/${id}/attachment`, { method: 'POST', body: form })
    },
    removeAttachment: (id) => request(`/petty-cash/expenses/${id}/attachment`, { method: 'DELETE' }),
    /** The receipt file as a Blob (needs the auth header, so it can't be a plain link). */
    attachment: (id) => request(`/petty-cash/expenses/${id}/attachment`, { responseType: 'blob' }),
  },
  admin: {
    listUsers: () => request('/admin/users'),
    updateRole: (id, role) => request(`/admin/users/${id}/role`, { method: 'PUT', body: { role } }),
    listCommunities: () => request('/admin/communities'),
    createCommunity: (payload) => request('/admin/communities', { method: 'POST', body: payload }),
    assignManager: (id, username) =>
      request(`/admin/communities/${id}/managers`, { method: 'POST', body: { username } }),
  },
}
