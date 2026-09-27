<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const form = reactive({ username: '', password: '' })
const fieldErrors = ref({})
const error = ref('')
const loading = ref(false)

async function submit() {
  error.value = ''
  fieldErrors.value = {}
  loading.value = true
  try {
    await auth.login(form)
    router.push(route.query.redirect || '/')
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
    <h1>登入</h1>

    <div v-if="route.query.registered" class="alert success">註冊成功，請登入</div>
    <div v-if="route.query.passwordChanged" class="alert success">密碼已更新，請使用新密碼登入</div>
    <div v-if="error" class="alert error">{{ error }}</div>

    <div class="field">
      <label for="username">帳號</label>
      <input id="username" v-model.trim="form.username" autocomplete="username" required />
      <span v-if="fieldErrors.username" class="error">{{ fieldErrors.username }}</span>
    </div>

    <div class="field">
      <label for="password">密碼</label>
      <input id="password" v-model="form.password" type="password" autocomplete="current-password" required />
      <span v-if="fieldErrors.password" class="error">{{ fieldErrors.password }}</span>
    </div>

    <button class="btn" type="submit" :disabled="loading">{{ loading ? '登入中…' : '登入' }}</button>

    <p class="hint">還沒有帳號？<RouterLink to="/register">立即註冊</RouterLink></p>
  </form>
</template>
