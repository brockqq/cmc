<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '@/api/http'

const route = useRoute()
const router = useRouter()

// ?invite=CODE lets managers share a ready-to-use sign-up link
const form = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  inviteCode: route.query.invite ?? '',
})
const fieldErrors = ref({})
const error = ref('')
const loading = ref(false)

async function submit() {
  error.value = ''
  fieldErrors.value = {}
  if (form.password !== form.confirmPassword) {
    fieldErrors.value = { confirmPassword: '兩次輸入的密碼不一致' }
    return
  }
  loading.value = true
  try {
    await api.register({
      username: form.username,
      email: form.email,
      password: form.password,
      inviteCode: form.inviteCode || null,
    })
    router.push({ name: 'login', query: { registered: '1' } })
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
    <h1>註冊</h1>

    <div v-if="error" class="alert error">{{ error }}</div>

    <div class="field">
      <label for="username">帳號</label>
      <input id="username" v-model.trim="form.username" autocomplete="username" required />
      <span v-if="fieldErrors.username" class="error">{{ fieldErrors.username }}</span>
    </div>

    <div class="field">
      <label for="email">Email</label>
      <input id="email" v-model.trim="form.email" type="email" autocomplete="email" required />
      <span v-if="fieldErrors.email" class="error">{{ fieldErrors.email }}</span>
    </div>

    <div class="field">
      <label for="password">密碼（至少 8 字元）</label>
      <input id="password" v-model="form.password" type="password" autocomplete="new-password" required />
      <span v-if="fieldErrors.password" class="error">{{ fieldErrors.password }}</span>
    </div>

    <div class="field">
      <label for="confirmPassword">確認密碼</label>
      <input id="confirmPassword" v-model="form.confirmPassword" type="password" autocomplete="new-password" required />
      <span v-if="fieldErrors.confirmPassword" class="error">{{ fieldErrors.confirmPassword }}</span>
    </div>

    <div class="field">
      <label for="inviteCode">社區邀請碼（選填）</label>
      <input id="inviteCode" v-model.trim="form.inviteCode" style="text-transform: uppercase" />
      <span v-if="fieldErrors.inviteCode" class="error">{{ fieldErrors.inviteCode }}</span>
    </div>

    <button class="btn" type="submit" :disabled="loading">{{ loading ? '註冊中…' : '註冊' }}</button>

    <p class="hint">已經有帳號？<RouterLink to="/login">前往登入</RouterLink></p>
  </form>
</template>
