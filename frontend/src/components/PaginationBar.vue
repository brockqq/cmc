<script setup>
import { computed } from 'vue'

/** `page` is 1-based. Emits `update:page` and `update:size`. */
const props = defineProps({
  page: { type: Number, required: true },
  size: { type: Number, required: true },
  totalPages: { type: Number, required: true },
  totalItems: { type: Number, required: true },
  sizes: { type: Array, default: () => [5, 10, 20, 50] },
})
const emit = defineEmits(['update:page', 'update:size'])

/** Page numbers with gaps, e.g. [1, '…', 4, 5, 6, '…', 12]. */
const pages = computed(() => {
  const total = props.totalPages
  const current = props.page
  const set = new Set([1, total, current - 1, current, current + 1].filter((p) => p >= 1 && p <= total))
  const sorted = [...set].sort((a, b) => a - b)
  const result = []
  sorted.forEach((p, i) => {
    if (i > 0 && p - sorted[i - 1] > 1) result.push('…' + p)
    result.push(p)
  })
  return result
})

const rangeText = computed(() => {
  if (!props.totalItems) return '共 0 筆'
  const from = (props.page - 1) * props.size + 1
  const to = Math.min(props.page * props.size, props.totalItems)
  return `第 ${from}–${to} 筆，共 ${props.totalItems} 筆`
})

const go = (p) => p !== props.page && p >= 1 && p <= props.totalPages && emit('update:page', p)
</script>

<template>
  <nav class="pager" aria-label="分頁">
    <span class="range">{{ rangeText }}</span>

    <div v-if="totalPages > 1" class="pages">
      <button class="pg" :disabled="page <= 1" aria-label="上一頁" @click="go(page - 1)">‹</button>
      <template v-for="p in pages" :key="p">
        <span v-if="typeof p === 'string'" class="gap">…</span>
        <button v-else class="pg" :class="{ current: p === page }" :aria-current="p === page ? 'page' : null" @click="go(p)">
          {{ p }}
        </button>
      </template>
      <button class="pg" :disabled="page >= totalPages" aria-label="下一頁" @click="go(page + 1)">›</button>
    </div>

    <label class="size">
      每頁
      <select :value="size" @change="emit('update:size', Number($event.target.value))">
        <option v-for="s in sizes" :key="s" :value="s">{{ s }}</option>
      </select>
      筆
    </label>
  </nav>
</template>

<style scoped>
.pager {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid var(--border);
  font-size: 0.9rem;
  color: var(--muted);
}
.pages {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}
.pg {
  min-width: 2rem;
  height: 2rem;
  padding: 0 0.4rem;
  font: inherit;
  color: var(--text);
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 6px;
  cursor: pointer;
}
.pg.current {
  color: #fff;
  background: var(--primary);
  border-color: var(--primary);
}
.pg:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.gap {
  padding: 0 0.2rem;
}
.size {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}
</style>
