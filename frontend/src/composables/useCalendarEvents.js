import { ref } from 'vue'
import { api } from '@/api/http'
import { createLatest } from '@/utils/latest'

/**
 * Loads the calendar's two lists: the occurrences in the visible grid and the upcoming ones (next 90 days).
 * `communityId()` returns the filter (undefined = all my communities); `range()` returns the grid's { from, to }.
 * Switching months or communities quickly never lets an older response overwrite a newer one.
 */
export function useCalendarEvents({ communityId, range }) {
  const monthEvents = ref([])
  const upcoming = ref([])
  const error = ref('')

  const monthLoads = createLatest()
  const upcomingLoads = createLatest()

  async function loadMonth() {
    const isCurrent = monthLoads.begin()
    error.value = ''
    try {
      const events = await api.events.list({ communityId: communityId(), ...range() })
      if (isCurrent()) monthEvents.value = events
    } catch (e) {
      if (isCurrent()) error.value = e.message
    }
  }

  async function loadUpcoming() {
    const isCurrent = upcomingLoads.begin()
    try {
      const events = await api.events.list({ communityId: communityId() })
      if (isCurrent()) upcoming.value = events
    } catch (e) {
      if (isCurrent()) error.value = e.message
    }
  }

  const reload = () => Promise.all([loadMonth(), loadUpcoming()])

  return { monthEvents, upcoming, error, loadMonth, loadUpcoming, reload }
}
