<script setup>
import { reactive, ref } from 'vue'
import { dayKey } from '@/utils/datetime'
import { MAX_RECEIPT_BYTES, RECEIPT_ACCEPT, formatFileSize } from '@/utils/money'

/**
 * Record / edit a petty cash expense.
 * `save({ data, file, removeAttachment })` returns a promise; errors are shown in the form.
 */
const props = defineProps({
  initial: { type: Object, default: null },
  save: { type: Function, required: true },
})
const emit = defineEmits(['cancel'])

const editing = !!props.initial
const form = reactive({
  spentOn: props.initial?.spentOn ?? dayKey(new Date()),
  amount: props.initial?.amount ?? '',
  purpose: props.initial?.purpose ?? '',
  payee: props.initial?.payee ?? '',
  receiptNo: props.initial?.receiptNo ?? '',
  note: props.initial?.note ?? '',
})
const file = ref(null)
const removeAttachment = ref(false)
const error = ref('')
const fieldErrors = ref({})
const saving = ref(false)

function onFile(event) {
  const chosen = event.target.files[0] ?? null
  fieldErrors.value = { ...fieldErrors.value, file: undefined }
  if (chosen && chosen.size > MAX_RECEIPT_BYTES) {
    fieldErrors.value = { ...fieldErrors.value, file: '檔案不可超過 5MB' }
    event.target.value = ''
    file.value = null
    return
  }
  file.value = chosen
  if (chosen) removeAttachment.value = false
}

async function submit() {
  error.value = ''
  fieldErrors.value = {}
  saving.value = true
  try {
    await props.save({
      data: { ...form, amount: Number(form.amount) },
      file: file.value,
      removeAttachment: removeAttachment.value,
    })
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

    <div class="grid2">
      <div class="field">
        <label for="ex-date">日期</label>
        <input id="ex-date" v-model="form.spentOn" type="date" required />
        <span v-if="fieldErrors.spentOn" class="error">{{ fieldErrors.spentOn }}</span>
      </div>
      <div class="field">
        <label for="ex-amount">金額（元）</label>
        <input id="ex-amount" v-model="form.amount" type="number" min="1" step="1" inputmode="numeric" required />
        <span v-if="fieldErrors.amount" class="error">{{ fieldErrors.amount }}</span>
      </div>
    </div>

    <div class="field">
      <label for="ex-purpose">用途</label>
      <input id="ex-purpose" v-model="form.purpose" maxlength="200" placeholder="例如：中庭燈具更換" required />
      <span v-if="fieldErrors.purpose" class="error">{{ fieldErrors.purpose }}</span>
    </div>

    <div class="grid2">
      <div class="field">
        <label for="ex-payee">付款對象（選填）</label>
        <input id="ex-payee" v-model="form.payee" maxlength="100" placeholder="例如：○○五金行" />
      </div>
      <div class="field">
        <label for="ex-receipt">單據號碼（選填）</label>
        <input id="ex-receipt" v-model="form.receiptNo" maxlength="50" placeholder="發票或收據號碼" />
      </div>
    </div>

    <div class="field">
      <label for="ex-note">備註（選填）</label>
      <textarea id="ex-note" v-model="form.note" rows="2" maxlength="500"></textarea>
    </div>

    <div class="field">
      <label for="ex-file">單據檔案（選填，圖片或 PDF，5MB 以內）</label>
      <p v-if="editing && initial.attachment && !file" class="current">
        目前：{{ initial.attachment.name }}（{{ formatFileSize(initial.attachment.size) }}）
        <label class="check inline"><input v-model="removeAttachment" type="checkbox" /> 移除</label>
      </p>
      <input id="ex-file" type="file" :accept="RECEIPT_ACCEPT" @change="onFile" />
      <span v-if="fieldErrors.file" class="error">{{ fieldErrors.file }}</span>
    </div>

    <div class="actions">
      <button class="btn" type="submit" :disabled="saving">{{ saving ? '儲存中…' : editing ? '儲存' : '登錄支出' }}</button>
      <button class="btn secondary" type="button" @click="emit('cancel')">取消</button>
    </div>
  </form>
</template>

<style scoped>
.grid2 {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 0 1rem;
}
.current {
  margin: 0 0 0.4rem;
  font-size: 0.9rem;
  color: var(--muted);
}
.check.inline {
  margin: 0 0 0 0.75rem;
}
input[type='file'] {
  font-size: 0.9rem;
}
</style>
