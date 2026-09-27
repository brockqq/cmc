<script setup>
import { ref } from 'vue'
import { formatDateTime } from '@/utils/datetime'

/** Audit trail of one period, newest first (collapsed to the latest few entries by default). */
const props = defineProps({
  entries: { type: Array, required: true },
})

const ACTION_LABELS = {
  EXPENSE_CREATED: '登錄支出',
  EXPENSE_UPDATED: '修改支出',
  EXPENSE_DELETED: '刪除支出',
  ATTACHMENT_ADDED: '上傳單據',
  ATTACHMENT_REPLACED: '更換單據',
  ATTACHMENT_REMOVED: '移除單據',
  FUND_SET: '設定額度',
  REPLENISHED: '結算撥補',
}
const DANGER = new Set(['EXPENSE_DELETED', 'ATTACHMENT_REMOVED'])

const PREVIEW = 5
const showAll = ref(false)
const visible = () => (showAll.value ? props.entries : props.entries.slice(0, PREVIEW))
</script>

<template>
  <p v-if="!entries.length" class="hint left">這一期沒有異動紀錄。</p>
  <ol v-else class="log">
    <li v-for="l in visible()" :key="l.id">
      <div class="line">
        <span class="action" :class="{ danger: DANGER.has(l.action) }">{{ ACTION_LABELS[l.action] ?? l.action }}</span>
        <span class="subject">{{ l.subject }}</span>
        <span class="who">{{ l.actor }} · {{ formatDateTime(l.at) }}</span>
      </div>
      <ul v-if="l.changes.length" class="changes">
        <li v-for="(c, i) in l.changes" :key="i">{{ c }}</li>
      </ul>
    </li>
  </ol>
  <button v-if="entries.length > PREVIEW" class="link" @click="showAll = !showAll">
    {{ showAll ? '收合' : `顯示全部 ${entries.length} 筆` }}
  </button>
</template>

<style scoped>
.hint.left {
  text-align: left;
  margin-top: 0;
}
.log {
  list-style: none;
  margin: 0 0 0.5rem;
  padding: 0;
}
.log > li {
  padding: 0.5rem 0;
  border-bottom: 1px solid var(--border);
}
.log > li:last-child {
  border-bottom: none;
}
.line {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 0.25rem 0.6rem;
}
.action {
  padding: 0 0.4rem;
  font-size: 0.78rem;
  color: var(--primary);
  background: var(--event-bg);
  border-radius: 4px;
}
.action.danger {
  color: var(--danger);
  background: transparent;
  border: 1px solid currentColor;
}
.who {
  margin-left: auto;
  font-size: 0.8rem;
  color: var(--muted);
}
.changes {
  margin: 0.3rem 0 0;
  padding-left: 1.2rem;
  font-size: 0.85rem;
  color: var(--muted);
}
</style>
