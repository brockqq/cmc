<script setup>
import { onMounted, ref } from 'vue'
import { api } from '@/api/http'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()

const users = ref([])
const loading = ref(true)
const error = ref('')
const message = ref('')
const savingId = ref(null)

const roleLabels = { USER: '一般會員', ADMIN: '平台管理員' }

async function load() {
  loading.value = true
  error.value = ''
  try {
    users.value = await api.admin.listUsers()
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function changeRole(user, event) {
  const role = event.target.value
  error.value = ''
  message.value = ''
  savingId.value = user.id
  try {
    const updated = await api.admin.updateRole(user.id, role)
    Object.assign(user, updated)
    message.value = `已將 ${user.username} 設為${roleLabels[role]}`
  } catch (e) {
    error.value = e.message
    event.target.value = user.role // revert the select
  } finally {
    savingId.value = null
  }
}

const formatDate = (iso) => new Date(iso).toLocaleString('zh-TW')

onMounted(load)
</script>

<template>
  <section class="card">
    <h1>會員管理</h1>

    <div v-if="error" class="alert error">{{ error }}</div>
    <div v-if="message" class="alert success">{{ message }}</div>

    <p v-if="loading" class="hint">載入中…</p>
    <div v-else class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>#</th>
            <th>帳號</th>
            <th>Email</th>
            <th>註冊時間</th>
            <th>角色</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="u in users" :key="u.id">
            <td>{{ u.id }}</td>
            <td>{{ u.username }}</td>
            <td>{{ u.email }}</td>
            <td>{{ formatDate(u.createdAt) }}</td>
            <td>
              <span v-if="u.username === auth.user?.username" class="self">{{ roleLabels[u.role] }}（自己）</span>
              <select v-else :value="u.role" :disabled="savingId === u.id" @change="changeRole(u, $event)">
                <option v-for="(label, value) in roleLabels" :key="value" :value="value">{{ label }}</option>
              </select>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<style scoped>
.self {
  color: var(--muted);
}
</style>
