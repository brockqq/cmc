<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { formatEventRange, fromLocalInput, toLocalInput } from '@/utils/datetime'
import { DAYS, DAY_LABELS, FREQUENCY_OPTIONS, UNIT_LABELS, browserTimeZone, describeRecurrence } from '@/utils/recurrence'

/**
 * Create/edit form for a calendar event.
 * - mode "series" (default): create an event, or edit the whole series (`initial` = series from GET /events/{id})
 * - mode "occurrence": edit one occurrence only (`initial` = that occurrence); no community/recurrence fields
 * `communities`: [{ id, name }] the user can manage. `defaultStart`: Date to prefill (e.g. the clicked day).
 * `save(payload)`: returns a promise; API errors are shown inside the form.
 */
const props = defineProps({
  initial: { type: Object, default: null },
  mode: { type: String, default: 'series' },
  communities: { type: Array, required: true },
  defaultStart: { type: Date, default: null },
  save: { type: Function, required: true },
})
const emit = defineEmits(['cancel'])

function defaultStartValue() {
  const d = props.defaultStart ? new Date(props.defaultStart) : new Date()
  d.setHours(props.defaultStart ? 10 : d.getHours() + 1, 0, 0, 0)
  return toLocalInput(d.toISOString())
}

const editing = !!props.initial
const single = props.mode === 'occurrence'
const initialRule = single ? null : props.initial?.recurrence

const form = reactive({
  communityId: props.initial?.communityId ?? props.communities[0]?.id ?? null,
  title: props.initial?.title ?? '',
  // Series: the first occurrence's times. Single occurrence: its own (possibly already changed) times.
  startAt: props.initial
    ? toLocalInput(single ? props.initial.startAt : props.initial.seriesStartAt)
    : defaultStartValue(),
  endAt: toLocalInput(single ? props.initial?.endAt : props.initial?.seriesEndAt),
  location: props.initial?.location ?? '',
  description: props.initial?.description ?? '',
})

const repeat = reactive({
  frequency: initialRule?.frequency ?? '',
  interval: initialRule?.interval ?? 1,
  daysOfWeek: [...(initialRule?.daysOfWeek ?? [])],
  ends: initialRule?.until ? 'until' : initialRule?.count ? 'count' : 'never',
  until: initialRule?.until ?? '',
  count: initialRule?.count ?? 10,
})

const startWeekday = computed(() => (form.startAt ? DAYS[new Date(form.startAt).getDay()] : null))

// Weekly: default to (and keep) the start date's weekday selected until the user picks days themselves
watch(
  () => [repeat.frequency, startWeekday.value],
  ([freq, day], [, prevDay] = []) => {
    if (freq !== 'WEEKLY' || !day) return
    const onlyPrev = repeat.daysOfWeek.length === 1 && repeat.daysOfWeek[0] === prevDay
    if (!repeat.daysOfWeek.length || onlyPrev) repeat.daysOfWeek = [day]
  },
  { immediate: true },
)

function toggleDay(day) {
  const i = repeat.daysOfWeek.indexOf(day)
  if (i >= 0) {
    if (repeat.daysOfWeek.length > 1) repeat.daysOfWeek.splice(i, 1)
  } else {
    repeat.daysOfWeek.push(day)
  }
}

const recurrence = computed(() => {
  if (!repeat.frequency) return null
  return {
    frequency: repeat.frequency,
    interval: Number(repeat.interval) || 1,
    daysOfWeek: repeat.frequency === 'WEEKLY' ? repeat.daysOfWeek : null,
    until: repeat.ends === 'until' ? repeat.until || null : null,
    count: repeat.ends === 'count' ? Number(repeat.count) || null : null,
  }
})
const summary = computed(() => describeRecurrence(recurrence.value))

const error = ref('')
const fieldErrors = ref({})
const saving = ref(false)

async function submit() {
  error.value = ''
  fieldErrors.value = {}
  if (form.endAt && form.endAt < form.startAt) {
    fieldErrors.value = { endAt: '結束時間不可早於開始時間' }
    return
  }
  if (recurrence.value?.until === null && repeat.ends === 'until') {
    fieldErrors.value = { until: '請選擇結束日期' }
    return
  }
  saving.value = true
  const times = { startAt: fromLocalInput(form.startAt), endAt: fromLocalInput(form.endAt) }
  try {
    await props.save(
      single
        ? { title: form.title, location: form.location, description: form.description, ...times }
        : {
            ...form,
            ...times,
            timeZone: props.initial?.timeZone ?? browserTimeZone(),
            recurrence: recurrence.value,
          },
    )
  } catch (e) {
    error.value = e.message
    fieldErrors.value = e.fieldErrors ?? {}
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <form class="editor" @submit.prevent="submit">
    <div v-if="error" class="alert error">{{ error }}</div>
    <p v-if="single" class="note">只修改 {{ formatEventRange(initial.originalStart) }} 這一次，其他日期不受影響。</p>
    <p v-else-if="editing && initial.recurrence" class="note">修改會套用到整個系列。</p>

    <div v-if="!single" class="field">
      <label for="ev-community">社區</label>
      <select id="ev-community" v-model="form.communityId" :disabled="editing" required>
        <option v-if="editing && !communities.some((c) => c.id === form.communityId)" :value="form.communityId">
          {{ initial.communityName }}
        </option>
        <option v-for="c in communities" :key="c.id" :value="c.id">{{ c.name }}</option>
      </select>
    </div>

    <div class="field">
      <label for="ev-title">活動名稱</label>
      <input id="ev-title" v-model="form.title" maxlength="200" required />
      <span v-if="fieldErrors.title" class="error">{{ fieldErrors.title }}</span>
    </div>

    <div class="row2">
      <div class="field">
        <label for="ev-start">{{ repeat.frequency ? '第一次開始' : '開始' }}</label>
        <input id="ev-start" v-model="form.startAt" type="datetime-local" required />
      </div>
      <div class="field">
        <label for="ev-end">結束（選填）</label>
        <input id="ev-end" v-model="form.endAt" type="datetime-local" :min="form.startAt" />
        <span v-if="fieldErrors.endAt" class="error">{{ fieldErrors.endAt }}</span>
      </div>
    </div>

    <fieldset v-if="!single" class="repeat">
      <legend>重複</legend>
      <div class="inline">
        <select v-model="repeat.frequency" aria-label="重複頻率">
          <option v-for="o in FREQUENCY_OPTIONS" :key="o.value" :value="o.value">{{ o.label }}</option>
        </select>
        <template v-if="repeat.frequency">
          <span>每</span>
          <input v-model.number="repeat.interval" class="num" type="number" min="1" max="99" aria-label="間隔" />
          <span>{{ UNIT_LABELS[repeat.frequency] }}</span>
        </template>
      </div>
      <span v-if="fieldErrors['recurrence.interval']" class="error">{{ fieldErrors['recurrence.interval'] }}</span>

      <template v-if="repeat.frequency">
        <div v-if="repeat.frequency === 'WEEKLY'" class="days" role="group" aria-label="星期">
          <button
            v-for="(day, i) in DAYS"
            :key="day"
            type="button"
            class="day-btn"
            :class="{ on: repeat.daysOfWeek.includes(day) }"
            :aria-pressed="repeat.daysOfWeek.includes(day)"
            @click="toggleDay(day)"
          >
            {{ DAY_LABELS[i] }}
          </button>
        </div>

        <div class="ends">
          <label class="radio"><input v-model="repeat.ends" type="radio" value="never" /> 永不結束</label>
          <label class="radio">
            <input v-model="repeat.ends" type="radio" value="until" /> 直到
            <input
              v-model="repeat.until"
              type="date"
              :min="form.startAt.slice(0, 10)"
              :disabled="repeat.ends !== 'until'"
              aria-label="結束日期"
            />
          </label>
          <label class="radio">
            <input v-model="repeat.ends" type="radio" value="count" /> 共
            <input
              v-model.number="repeat.count"
              class="num"
              type="number"
              min="1"
              max="500"
              :disabled="repeat.ends !== 'count'"
              aria-label="次數"
            />
            次
          </label>
        </div>
        <span v-if="fieldErrors.until" class="error">{{ fieldErrors.until }}</span>
        <span v-if="fieldErrors['recurrence.count']" class="error">{{ fieldErrors['recurrence.count'] }}</span>
        <p class="summary">{{ summary }}</p>
      </template>
    </fieldset>

    <div class="field">
      <label for="ev-location">地點（選填）</label>
      <input id="ev-location" v-model="form.location" maxlength="200" />
    </div>

    <div class="field">
      <label for="ev-description">說明（選填）</label>
      <textarea id="ev-description" v-model="form.description" rows="3" maxlength="2000"></textarea>
    </div>

    <div class="actions">
      <button class="btn" type="submit" :disabled="saving">{{ saving ? '儲存中…' : editing ? '儲存' : '新增' }}</button>
      <button class="btn secondary" type="button" @click="emit('cancel')">取消</button>
    </div>
  </form>
</template>

<style scoped>
.row2 {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 0 1rem;
}
.note {
  margin: 0 0 0.75rem;
  color: var(--warning);
  font-size: 0.9rem;
}
.repeat {
  margin: 0 0 1rem;
  padding: 0.75rem 1rem;
  border: 1px solid var(--border);
  border-radius: 6px;
}
.repeat legend {
  padding: 0 0.3rem;
  font-size: 0.9rem;
  font-weight: 600;
}
.inline,
.ends {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem 1rem;
}
.inline {
  gap: 0.5rem;
}
.ends {
  margin-top: 0.75rem;
}
.repeat input,
.repeat select {
  padding: 0.35rem 0.5rem;
  font: inherit;
  color: var(--text);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 6px;
}
.repeat input:disabled {
  opacity: 0.5;
}
.num {
  width: 4.5rem;
}
.radio {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  cursor: pointer;
}
.radio input[type='radio'] {
  padding: 0;
}
.days {
  display: flex;
  gap: 0.3rem;
  margin-top: 0.75rem;
}
.day-btn {
  width: 2.2rem;
  height: 2.2rem;
  font: inherit;
  color: var(--text);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 50%;
  cursor: pointer;
}
.day-btn.on {
  color: #fff;
  background: var(--primary);
  border-color: var(--primary);
}
.summary {
  margin: 0.75rem 0 0;
  color: var(--primary);
  font-size: 0.9rem;
}
.error {
  display: block;
  margin-top: 0.3rem;
  color: var(--danger);
  font-size: 0.85rem;
}
</style>
