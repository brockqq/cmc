<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '@/api/http'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

const form = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' })
const fieldErrors = ref({})
const error = ref('')
const loading = ref(false)

async function submit() {
  error.value = ''
  fieldErrors.value = {}
  if (form.newPassword !== form.confirmPassword) {
    fieldErrors.value = { confirmPassword: '兩次輸入的密碼不一致' }
    return
  }
  loading.value = true
  try {
    await api.changePassword({ currentPassword: form.currentPassword, newPassword: form.newPassword })
    // The server revokes all existing tokens, so the user must sign in again
    auth.logout()
    router.push({ name: 'login', query: { passwordChanged: '1' } })
  } catch (e) {
    error.value = e.message
    fieldErrors.value = e.fieldErrors ?? {}
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <form class="card narrow" @submit.prevent="submit">
    <h1>修改密碼</h1>

    <div v-if="error" class="alert error">{{ error }}</div>

    <div class="field">
      <label for="currentPassword">目前密碼</label>
      <input id="currentPassword" v-model="form.currentPassword" type="password" autocomplete="current-password" required />
      <span v-if="fieldErrors.currentPassword" class="error">{{ fieldErrors.currentPassword }}</span>
    </div>

    <div class="field">
      <label for="newPassword">新密碼（至少 8 字元）</label>
      <input id="newPassword" v-model="form.newPassword" type="password" autocomplete="new-password" required />
      <span v-if="fieldErrors.newPassword" class="error">{{ fieldErrors.newPassword }}</span>
    </div>

    <div class="field">
      <label for="confirmPassword">確認新密碼</label>
      <input id="confirmPassword" v-model="form.confirmPassword" type="password" autocomplete="new-password" required />
      <span v-if="fieldErrors.confirmPassword" class="error">{{ fieldErrors.confirmPassword }}</span>
    </div>

    <button class="btn" type="submit" :disabled="loading">{{ loading ? '更新中…' : '更新密碼' }}</button>

    <p class="hint">更新後所有裝置都會登出，需使用新密碼重新登入。</p>
  </form>
</template>
