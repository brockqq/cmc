<script setup>
import { describeRecurrence } from '@/utils/recurrence'
import { formatEventRange } from '@/utils/datetime'

/** Read-only view of one occurrence, with the actions a manager can take on it. */
defineProps({
  event: { type: Object, required: true },
})
const emit = defineEmits(['edit-occurrence', 'reset-occurrence', 'edit-series', 'cancel-occurrence', 'remove'])
</script>

<template>
  <span class="badge">{{ event.communityName }}</span>
  <span v-if="event.modified" class="badge pin">已個別調整</span>
  <h2 class="ev-title">{{ event.title }}</h2>
  <dl class="ev-meta">
    <dt>時間</dt>
    <dd>{{ formatEventRange(event.startAt, event.endAt) }}</dd>
    <template v-if="event.modified">
      <dt>原定</dt>
      <dd class="struck">{{ formatEventRange(event.originalStart) }}</dd>
    </template>
    <template v-if="event.recurrence">
      <dt>重複</dt>
      <dd>{{ describeRecurrence(event.recurrence) }}</dd>
    </template>
    <template v-if="event.location">
      <dt>地點</dt>
      <dd>{{ event.location }}</dd>
    </template>
  </dl>
  <p v-if="event.description" class="ev-desc">{{ event.description }}</p>
  <p class="ev-foot">
    由 {{ event.createdBy }} 建立
    <span v-if="event.canEdit" class="ops">
      <template v-if="event.recurrence">
        <button class="link" @click="emit('edit-occurrence')">修改這一次</button>
        <button v-if="event.modified" class="link" @click="emit('reset-occurrence')">還原為系列設定</button>
        <button class="link" @click="emit('edit-series')">編輯系列</button>
        <button class="link danger" @click="emit('cancel-occurrence')">取消這一次</button>
        <button class="link danger" @click="emit('remove')">刪除整個系列</button>
      </template>
      <template v-else>
        <button class="link" @click="emit('edit-series')">編輯</button>
        <button class="link danger" @click="emit('remove')">刪除</button>
      </template>
    </span>
  </p>
</template>

<style scoped>
.ev-title {
  margin: 0.5rem 0;
}
.ev-meta {
  display: grid;
  grid-template-columns: max-content 1fr;
  gap: 0.3rem 1rem;
  margin: 0;
}
.ev-meta dt {
  color: var(--muted);
}
.ev-meta dd {
  margin: 0;
}
.ev-desc {
  white-space: pre-wrap;
}
.ev-foot {
  color: var(--muted);
  font-size: 0.85rem;
  margin-bottom: 0;
}
.ops {
  margin-left: 1rem;
  display: inline-flex;
  flex-wrap: wrap;
  gap: 0.4rem 0.75rem;
}
.struck {
  color: var(--muted);
  text-decoration: line-through;
}
</style>
