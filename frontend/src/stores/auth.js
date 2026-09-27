import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { api, tokenStorage } from '@/api/http'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(tokenStorage.get())
  const user = ref(null)

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')

  async function login(credentials) {
    const res = await api.login(credentials)
    token.value = res.token
    user.value = res.user
    tokenStorage.set(res.token)
  }

  async function fetchMe() {
    user.value = await api.me()
    return user.value
  }

  function logout() {
    token.value = null
    user.value = null
    tokenStorage.clear()
  }

  return { token, user, isLoggedIn, isAdmin, login, fetchMe, logout }
})
