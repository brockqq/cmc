<script setup>
import { onBeforeUnmount, ref } from 'vue'
import { api } from '@/api/http'

/**
 * Overlay showing a receipt image or PDF. The file needs the auth header, so it is fetched as a Blob and shown
 * through an object URL (revoked when closed). Call `open(expense)` via a template ref.
 */
const emit = defineEmits(['error'])
const preview = ref(null) // { url, type, name }

/** Only these are ever rendered; anything else (should the server be tricked) is not shown in the page. */
const PREVIEWABLE = ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'application/pdf']

async function open(expense) {
  try {
    const blob = await api.pettyCash.attachment(expense.id)
    const type = blob.type.split(';')[0].trim().toLowerCase()
    if (!PREVIEWABLE.includes(type)) {
      emit('error', '無法預覽此檔案類型')
      return
    }
    close()
    // Re-wrap so the object URL carries exactly the checked type
    const url = URL.createObjectURL(new Blob([blob], { type }))
    preview.value = { url, type, name: expense.attachment.name }
  } catch (e) {
    emit('error', e.message)
  }
}

function close() {
  if (preview.value) URL.revokeObjectURL(preview.value.url)
  preview.value = null
}

onBeforeUnmount(close)
defineExpose({ open, close })
</script>

<template>
  <div v-if="preview" class="overlay" role="dialog" aria-label="單據預覽" @click.self="close">
    <div class="viewer">
      <div class="viewer-head">
        <span>{{ preview.name }}</span>
        <span>
          <a :href="preview.url" :download="preview.name">下載</a>
          <button class="link" @click="close">關閉</button>
        </span>
      </div>
      <img v-if="preview.type.startsWith('image/')" :src="preview.url" :alt="preview.name" />
      <iframe v-else :src="preview.url" :title="preview.name"></iframe>
    </div>
  </div>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  background: rgb(0 0 0 / 0.6);
}
.viewer {
  display: flex;
  flex-direction: column;
  width: min(900px, 100%);
  max-height: 100%;
  background: var(--surface);
  border-radius: 8px;
  overflow: hidden;
}
.viewer-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.6rem 1rem;
  border-bottom: 1px solid var(--border);
  font-size: 0.9rem;
}
.viewer-head span:last-child {
  display: flex;
  gap: 1rem;
}
.viewer img {
  max-width: 100%;
  max-height: calc(100vh - 6rem);
  object-fit: contain;
  align-self: center;
}
.viewer iframe {
  width: 100%;
  height: calc(100vh - 6rem);
  border: none;
}
</style>
