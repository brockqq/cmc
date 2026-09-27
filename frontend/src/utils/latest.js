/**
 * Guards against out-of-order responses: when the user switches community/month/page quickly, an older request
 * may finish after a newer one and overwrite its results.
 *
 *   const latest = createLatest()
 *   async function load() {
 *     const isCurrent = latest.begin()
 *     const data = await api.something()
 *     if (!isCurrent()) return // a newer load has started; drop this response
 *     ...
 *   }
 */
export function createLatest() {
  let current = 0
  return {
    /** Starts a new request and returns a function telling whether it is still the latest one. */
    begin() {
      const id = ++current
      return () => id === current
    },
    /** Makes every request in flight stale (e.g. when the thing being viewed has changed). */
    invalidate() {
      current++
    },
  }
}
