<script setup>
import { onMounted, reactive, ref } from 'vue'
import { api } from '@/api/http'
import { useCommunityStore } from '@/stores/communities'

const myCommunities = useCommunityStore()

const communities = ref([])
const loading = ref(true)
const error = ref('')
const message = ref('')

const createForm = reactive({ name: '', description: '' })
const createErrors = ref({})
const creating = ref(false)

const assignForm = reactive({ communityId: '', username: '' })
const assigning = ref(false)

async function load() {
  loading.value = true
  try {
    communities.value = await api.admin.listCommunities()
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function reset() {
  error.value = ''
  message.value = ''
}

async function create() {
  reset()
  createErrors.value = {}
  creating.value = true
  try {
    const created = await api.admin.createCommunity(createForm)
    communities.value.push(created)
    message.value = `已建立「${created.name}」，邀請碼 ${created.inviteCode}`
    createForm.name = ''
    createForm.description = ''
  } catch (e) {
    error.value = e.message
    createErrors.value = e.fieldErrors ?? {}
  } finally {
    creating.value = false
  }
}

async function assign() {
  reset()
  assigning.value = true
  try {
    const member = await api.admin.assignManager(assignForm.communityId, assignForm.username)
    const community = communities.value.find((c) => String(c.id) === String(assignForm.communityId))
    message.value = `已指派 ${member.username} 為「${community?.name}」的社區管理員`
    assignForm.username = ''
    await Promise.all([load(), myCommunities.fetchMine()])
  } catch (e) {
    error.value = e.message
  } finally {
    assigning.value = false
  }
}

const formatDate = (iso) => new Date(iso).toLocaleDateString('zh-TW')

onMounted(load)
</script>

<template>
  <section class="card">
    <h1>社區管理</h1>

    <div v-if="error" class="alert error">{{ error }}</div>
    <div v-if="message" class="alert success">{{ message }}</div>

    <div class="forms">
      <form @submit.prevent="create">
        <h2>建立社區</h2>
        <div class="field">
          <label for="name">社區名稱</label>
          <input id="name" v-model.trim="createForm.name" required maxlength="100" />
          <span v-if="createErrors.name" class="error">{{ createErrors.name }}</span>
        </div>
        <div class="field">
          <label for="description">描述（選填）</label>
          <input id="description" v-model.trim="createForm.description" maxlength="500" />
        </div>
        <button class="btn" type="submit" :disabled="creating">{{ creating ? '建立中…' : '建立' }}</button>
      </form>

      <form @submit.prevent="assign">
        <h2>指派社區管理員</h2>
        <div class="field">
          <label for="community">社區</label>
          <select id="community" v-model="assignForm.communityId" required>
            <option value="" disabled>請選擇</option>
            <option v-for="c in communities" :key="c.id" :value="c.id">{{ c.name }}</option>
          </select>
        </div>
        <div class="field">
          <label for="username">會員帳號</label>
          <input id="username" v-model.trim="assignForm.username" required />
        </div>
        <button class="btn" type="submit" :disabled="assigning || !communities.length">
          {{ assigning ? '指派中…' : '指派' }}
        </button>
      </form>
    </div>
  </section>

  <section class="card">
    <h2>所有社區</h2>
    <p v-if="loading" class="hint">載入中…</p>
    <p v-else-if="!communities.length" class="hint">尚未建立任何社區。</p>
    <div v-else class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>#</th>
            <th>名稱</th>
            <th>邀請碼</th>
            <th>成員數</th>
            <th>建立日期</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="c in communities" :key="c.id">
            <td>{{ c.id }}</td>
            <td>{{ c.name }}</td>
            <td><code>{{ c.inviteCode }}</code></td>
            <td>{{ c.memberCount }}</td>
            <td>{{ formatDate(c.createdAt) }}</td>
            <td><RouterLink :to="{ name: 'community', params: { id: c.id } }">管理成員</RouterLink></td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<style scoped>
.card + .card {
  margin-top: 1.25rem;
}
h2 {
  font-size: 1.1rem;
  margin: 0 0 1rem;
}
.forms {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 2rem;
}
.field select {
  padding: 0.55rem 0.7rem;
  font-size: 1rem;
}
</style>
