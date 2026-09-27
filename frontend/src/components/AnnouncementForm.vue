<script setup>
import { reactive, ref } from 'vue'

/**
 * Create/edit form for an announcement.
 * `scopes`: [{ value: communityId | null, label }] — null means platform-wide.
 * `save(payload)`: returns a promise; API errors are shown inside the form.
 */
const props = defineProps({
  initial: { type: Object, default: null },
  scopes: { type: Array, required: true },
  save: { type: Function, required: true },
})
const emit = defineEmits(['cancel'])

const editing = !!props.initial
const form = reactive({
  communityId: props.initial ? props.initial.communityId : (props.scopes[0]?.value ?? null),
  title: props.initial?.title ?? '',
  content: props.initial?.content ?? '',
  pinned: props.initial?.pinned ?? false,
})
const error = ref('')
const fieldErrors = ref({})
const saving = ref(false)

async function submit() {
  error.value = ''
  fieldErrors.value = {}
  saving.value = true
  try {
    await props.save({ ...form })
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

    <div class="field">
      <label for="ann-scope">發布對象</label>
      <select id="ann-scope" v-model="form.communityId" :disabled="editing">
        <option v-if="editing && !scopes.some((s) => s.value === form.communityId)" :value="form.communityId">
          {{ initial.communityName ?? '全平台' }}
        </option>
        <option v-for="s in scopes" :key="String(s.value)" :value="s.value">{{ s.label }}</option>
      </select>
    </div>

    <div class="field">
      <label for="ann-title">標題</label>
      <input id="ann-title" v-model="form.title" maxlength="200" required />
      <span v-if="fieldErrors.title" class="error">{{ fieldErrors.title }}</span>
    </div>

    <div class="field">
      <label for="ann-content">內容</label>
      <textarea id="ann-content" v-model="form.content" rows="5" maxlength="5000" required></textarea>
      <span v-if="fieldErrors.content" class="error">{{ fieldErrors.content }}</span>
    </div>

    <label class="check"><input v-model="form.pinned" type="checkbox" /> 置頂</label>

    <div class="actions">
      <button class="btn" type="submit" :disabled="saving">{{ saving ? '儲存中…' : editing ? '儲存' : '發布' }}</button>
      <button class="btn secondary" type="button" @click="emit('cancel')">取消</button>
    </div>
  </form>
</template>
