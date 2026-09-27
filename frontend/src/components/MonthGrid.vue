<script setup>
import { computed } from 'vue'
import { WEEKDAYS, dayKey, isSameDay, startOfDay } from '@/utils/datetime'
import { gridDays, gridStartFor, groupByDay, occurrenceKey } from '@/utils/calendar'

/** Month view: toolbar plus a 6-week grid showing up to two events per day. */
const props = defineProps({
  /** Any date in the month shown. */
  month: { type: Date, required: true },
  /** Occurrences to show (those overlapping the grid). */
  events: { type: Array, required: true },
  selectedDay: { type: Date, default: null },
})
const emit = defineEmits(['select-day', 'select-event', 'shift-month', 'today'])

const MAX_PER_DAY = 2
const today = startOfDay(new Date())

const days = computed(() => gridDays(gridStartFor(props.month)))
const eventsByDay = computed(() => groupByDay(props.events, gridStartFor(props.month)))
const label = computed(() => `${props.month.getFullYear()} 年 ${props.month.getMonth() + 1} 月`)

const eventsOn = (d) => eventsByDay.value[dayKey(d)] ?? []

function dayLabel(d) {
  const count = eventsOn(d).length
  return `${d.getMonth() + 1} 月 ${d.getDate()} 日${count ? `，${count} 個活動` : ''}`
}
</script>

<template>
  <div class="toolbar">
    <button class="nav-btn" aria-label="上個月" @click="emit('shift-month', -1)">‹</button>
    <strong>{{ label }}</strong>
    <button class="nav-btn" aria-label="下個月" @click="emit('shift-month', 1)">›</button>
    <button class="link" @click="emit('today')">今天</button>
  </div>

  <div class="grid" role="grid" :aria-label="label">
    <div v-for="w in WEEKDAYS" :key="w" class="weekday" role="columnheader">{{ w }}</div>
    <div
      v-for="d in days"
      :key="dayKey(d)"
      class="day"
      role="gridcell"
      tabindex="0"
      :aria-label="dayLabel(d)"
      :aria-selected="!!selectedDay && isSameDay(d, selectedDay)"
      :class="{
        outside: d.getMonth() !== month.getMonth(),
        today: isSameDay(d, today),
        selected: selectedDay && isSameDay(d, selectedDay),
      }"
      @click="emit('select-day', d)"
      @keydown.enter.self.prevent="emit('select-day', d)"
      @keydown.space.self.prevent="emit('select-day', d)"
    >
      <span class="num" aria-hidden="true">{{ d.getDate() }}</span>
      <button
        v-for="e in eventsOn(d).slice(0, MAX_PER_DAY)"
        :key="occurrenceKey(e)"
        class="ev"
        :title="e.title"
        @click.stop="emit('select-event', e)"
      >
        {{ e.title }}
      </button>
      <span v-if="eventsOn(d).length > MAX_PER_DAY" class="more">+{{ eventsOn(d).length - MAX_PER_DAY }}</span>
      <span v-if="eventsOn(d).length" class="dot" aria-hidden="true"></span>
    </div>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin: 0.5rem 0 0.75rem;
}
.nav-btn {
  width: 2rem;
  height: 2rem;
  font-size: 1.2rem;
  line-height: 1;
  color: var(--text);
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 6px;
  cursor: pointer;
}
.grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  border-top: 1px solid var(--border);
  border-left: 1px solid var(--border);
}
.weekday {
  padding: 0.3rem;
  text-align: center;
  font-size: 0.8rem;
  color: var(--muted);
  border-right: 1px solid var(--border);
  border-bottom: 1px solid var(--border);
}
.day {
  position: relative;
  min-height: 5.2rem;
  padding: 0.25rem;
  border-right: 1px solid var(--border);
  border-bottom: 1px solid var(--border);
  cursor: pointer;
  overflow: hidden;
}
.day:hover {
  background: var(--bg);
}
.day:focus-visible {
  outline: 2px solid var(--primary);
  outline-offset: -2px;
}
.day.outside .num {
  color: var(--muted);
  opacity: 0.6;
}
.day.selected {
  outline: 2px solid var(--primary);
  outline-offset: -2px;
}
.num {
  display: inline-block;
  min-width: 1.5rem;
  font-size: 0.8rem;
  text-align: center;
  border-radius: 999px;
}
.day.today .num {
  color: #fff;
  background: var(--primary);
}
.ev {
  display: block;
  width: 100%;
  margin-top: 2px;
  padding: 1px 4px;
  font: inherit;
  font-size: 0.72rem;
  text-align: left;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: var(--primary);
  background: var(--event-bg);
  border: none;
  border-radius: 3px;
  cursor: pointer;
}
.more {
  font-size: 0.7rem;
  color: var(--muted);
}
.dot {
  display: none;
}

/* Phones: cells too narrow for titles — show a dot instead */
@media (max-width: 560px) {
  .day {
    min-height: 3rem;
  }
  .ev,
  .more {
    display: none;
  }
  .dot {
    display: block;
    width: 6px;
    height: 6px;
    margin: 0.2rem auto 0;
    border-radius: 50%;
    background: var(--primary);
  }
}
</style>
