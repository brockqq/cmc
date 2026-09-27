<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ApiError, api } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import { useCommunityStore } from '@/stores/communities'
import EventDetail from '@/components/EventDetail.vue'
import EventForm from '@/components/EventForm.vue'
import MonthGrid from '@/components/MonthGrid.vue'
import { useCalendarEvents } from '@/composables/useCalendarEvents'
import { GRID_DAYS, gridStartFor, groupByDay, occurrenceKey as occKey } from '@/utils/calendar'
import { addDays, dayKey, formatDate, formatEventRange, formatTime, startOfDay } from '@/utils/datetime'

const auth = useAuthStore()
const communities = useCommunityStore()

const today = startOfDay(new Date())
const cursor = ref(new Date(today.getFullYear(), today.getMonth(), 1)) // first day of shown month
const filter = ref('') // '' = all my communities, otherwise a community id
const selectedDay = ref(null)
const selectedEvent = ref(null)
const composing = ref(false)
const editing = ref(false) // false | 'series' | 'occurrence'
const seriesForEdit = ref(null) // series-level values (GET /events/{id}) for the edit-series form

/** Platform admins can look at any community; everyone else at the ones they joined. */
const filterOptions = computed(() => (auth.isAdmin ? communities.manageable : communities.mine))

// 6 full weeks starting on the Sunday on/before the 1st
const gridStart = computed(() => gridStartFor(cursor.value))

const { monthEvents, upcoming, error, loadMonth, reload } = useCalendarEvents({
  communityId: () => filter.value || undefined,
  range: () => ({ from: gridStart.value, to: addDays(gridStart.value, GRID_DAYS) }),
})

const selectedDayEvents = computed(() =>
  selectedDay.value ? (groupByDay(monthEvents.value, gridStart.value)[dayKey(selectedDay.value)] ?? []) : [],
)

/** Points the grid at the month containing `date` (callers load data afterwards). */
function setMonth(date) {
  cursor.value = new Date(date.getFullYear(), date.getMonth(), 1)
}

function shiftMonth(delta) {
  setMonth(new Date(cursor.value.getFullYear(), cursor.value.getMonth() + delta, 1))
  loadMonth()
}

function goToday() {
  setMonth(today)
  loadMonth()
  selectDay(today)
}

function selectDay(d) {
  selectedDay.value = d
  selectedEvent.value = null
  editing.value = false
}

function selectEvent(e) {
  selectedEvent.value = e
  editing.value = false
  composing.value = false
}

function startComposing() {
  composing.value = true
  selectedEvent.value = null
}

async function create(payload) {
  const created = await api.events.create(payload)
  composing.value = false
  // A platform admin may add events to a community they have not joined; switch the view so it shows up
  if (!filter.value && !communities.mine.some((c) => c.id === created.communityId)) {
    filter.value = created.communityId
  }
  setMonth(new Date(created.startAt))
  await reload()
  selectEvent(monthEvents.value.find((e) => e.id === created.id) ?? created)
}

async function startEditSeries() {
  try {
    // The clicked occurrence may have its own title/time; the form must start from the series itself
    seriesForEdit.value = await api.events.get(selectedEvent.value.id)
    editing.value = 'series'
  } catch (e) {
    error.value = e.message
  }
}

const sameOccurrence = (a, b) => a.id === b.id && a.originalStart === b.originalStart

/**
 * Every change sends the version of the event as loaded; if someone else changed it since, the server answers 409.
 * Then reload, so the calendar (and the version sent next time) is current, and point to the latest content.
 */
async function refreshAfterConflict() {
  const current = selectedEvent.value
  await reload()
  selectedEvent.value = current ? (monthEvents.value.find((e) => sameOccurrence(e, current)) ?? null) : null
  if (!selectedEvent.value) editing.value = false
}

/** For the edit forms: keeps the form (and what was typed) open, with a hint on how to see the latest version. */
async function explainingConflicts(action) {
  try {
    return await action()
  } catch (e) {
    if (e.status !== 409) throw e
    await refreshAfterConflict()
    throw new ApiError(409, '此活動剛被其他人修改過，已載入最新資料。請按「取消」後重新開啟，確認最新內容再修改。')
  }
}

const update = (payload) =>
  explainingConflicts(async () => {
    const updated = await api.events.update(seriesForEdit.value.id, { ...payload, version: seriesForEdit.value.version })
    await reload()
    selectEvent(monthEvents.value.find((e) => e.id === updated.id) ?? updated)
  })

const updateOccurrence = (payload) =>
  explainingConflicts(async () => {
    const e = selectedEvent.value
    await showOccurrence(await api.events.updateOccurrence(e.id, e.originalStart, { ...payload, version: e.version }))
  })

async function resetOccurrence(event) {
  if (!confirm(`確定要將這一次還原為系列設定（${formatEventRange(event.originalStart)}）嗎？`)) return
  try {
    await showOccurrence(await api.events.resetOccurrence(event.id, event.originalStart, event.version))
  } catch (e) {
    error.value = e.message
    if (e.status === 409) await refreshAfterConflict()
  }
}

/** Reloads and selects the occurrence, following it to its month if it was moved. */
async function showOccurrence(occurrence) {
  setMonth(new Date(occurrence.startAt))
  await reload()
  selectEvent(monthEvents.value.find((e) => sameOccurrence(e, occurrence)) ?? occurrence)
}

async function remove(event) {
  const what = event.recurrence ? `整個「${event.title}」系列（所有日期）` : `「${event.title}」`
  if (!confirm(`確定要刪除${what}嗎？`)) return
  await runAndReload(() => api.events.remove(event.id, event.version))
}

async function cancelOccurrence(event) {
  if (!confirm(`確定要取消 ${formatEventRange(event.startAt)} 這一次的「${event.title}」嗎？其他日期不受影響。`)) return
  await runAndReload(() => api.events.cancelOccurrence(event.id, event.originalStart, event.version))
}

async function runAndReload(action) {
  try {
    await action()
    selectedEvent.value = null
    await reload()
  } catch (e) {
    error.value = e.message
    if (e.status === 409) await refreshAfterConflict()
  }
}

const UPCOMING_PREVIEW = 15
const showAllUpcoming = ref(false)
const visibleUpcoming = computed(() =>
  showAllUpcoming.value ? upcoming.value : upcoming.value.slice(0, UPCOMING_PREVIEW),
)

watch(filter, () => {
  selectedEvent.value = null
  reload()
})

onMounted(async () => {
  await Promise.all([reload(), communities.fetchManageable().catch(() => {})])
})
</script>

<template>
  <section class="card">
    <div class="head">
      <h1>行事曆</h1>
      <div class="head-actions">
        <select v-model="filter" aria-label="篩選社區">
          <option value="">{{ auth.isAdmin ? '我加入的社區' : '全部社區' }}</option>
          <option v-for="c in filterOptions" :key="c.id" :value="c.id">{{ c.name }}</option>
        </select>
        <button v-if="communities.manageable.length && !composing" class="btn small" @click="startComposing">
          新增活動
        </button>
      </div>
    </div>

    <div v-if="error" class="alert error">{{ error }}</div>

    <EventForm
      v-if="composing"
      :communities="communities.manageable"
      :default-start="selectedDay"
      :save="create"
      @cancel="composing = false"
    />

    <MonthGrid
      :month="cursor"
      :events="monthEvents"
      :selected-day="selectedDay"
      @select-day="selectDay"
      @select-event="selectEvent"
      @shift-month="shiftMonth"
      @today="goToday"
    />
  </section>

  <!-- Event detail -->
  <section v-if="selectedEvent" class="card">
    <EventForm
      v-if="editing === 'series'"
      :initial="seriesForEdit"
      :communities="communities.manageable"
      :save="update"
      @cancel="editing = false"
    />
    <EventForm
      v-else-if="editing === 'occurrence'"
      mode="occurrence"
      :initial="selectedEvent"
      :communities="communities.manageable"
      :save="updateOccurrence"
      @cancel="editing = false"
    />
    <EventDetail
      v-else
      :event="selectedEvent"
      @edit-occurrence="editing = 'occurrence'"
      @reset-occurrence="resetOccurrence(selectedEvent)"
      @edit-series="startEditSeries"
      @cancel-occurrence="cancelOccurrence(selectedEvent)"
      @remove="remove(selectedEvent)"
    />
  </section>

  <!-- Selected day -->
  <section v-else-if="selectedDay" class="card">
    <h2 class="section-title">{{ formatDate(selectedDay.toISOString()) }}</h2>
    <p v-if="!selectedDayEvents.length" class="hint left">這天沒有活動。</p>
    <ul class="agenda">
      <li v-for="e in selectedDayEvents" :key="occKey(e)">
        <button class="link" @click="selectEvent(e)">{{ e.title }}</button>
        <span v-if="e.recurrence" class="repeat-mark" title="重複活動">↻</span>
        <span class="muted">{{ formatTime(new Date(e.startAt)) }} · {{ e.communityName }}</span>
      </li>
    </ul>
  </section>

  <section class="card">
    <h2 class="section-title">接下來 90 天</h2>
    <p v-if="!upcoming.length" class="hint left">近期沒有活動。</p>
    <ul class="agenda">
      <li v-for="e in visibleUpcoming" :key="occKey(e)">
        <div class="when">{{ formatEventRange(e.startAt, e.endAt) }}</div>
        <div>
          <button class="link strong" @click="selectEvent(e)">{{ e.title }}</button>
          <span v-if="e.recurrence" class="repeat-mark" title="重複活動">↻</span>
          <span class="muted"> · {{ e.communityName }}<template v-if="e.location"> · {{ e.location }}</template></span>
        </div>
      </li>
    </ul>
    <button
      v-if="upcoming.length > UPCOMING_PREVIEW"
      class="link show-more"
      @click="showAllUpcoming = !showAllUpcoming"
    >
      {{ showAllUpcoming ? '收合' : `顯示全部 ${upcoming.length} 筆` }}
    </button>
  </section>
</template>

<style scoped>
.card + .card {
  margin-top: 1.25rem;
}
.head-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  align-items: center;
}
.section-title {
  font-size: 1.05rem;
  margin: 0 0 0.75rem;
}
.hint.left {
  text-align: left;
  margin-top: 0;
}
.agenda {
  list-style: none;
  margin: 0;
  padding: 0;
}
.agenda li {
  padding: 0.5rem 0;
  border-bottom: 1px solid var(--border);
}
.agenda li:last-child {
  border-bottom: none;
}
.when {
  font-size: 0.85rem;
  color: var(--muted);
}
.repeat-mark {
  margin-left: 0.3rem;
  color: var(--muted);
}
.show-more {
  margin-top: 0.5rem;
}
.strong {
  font-weight: 600;
}
.muted {
  color: var(--muted);
  font-size: 0.9rem;
}
</style>
