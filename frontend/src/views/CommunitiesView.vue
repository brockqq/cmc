<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { COMMUNITY_ROLE_LABELS, useCommunityStore } from '@/stores/communities'

const communities = useCommunityStore()
const router = useRouter()

const inviteCode = ref('')
const error = ref('')
const joining = ref(false)

async function join() {
  error.value = ''
  joining.value = true
  try {
    const joined = await communities.join(inviteCode.value)
    router.push({ name: 'community', params: { id: joined.id } })
  } catch (e) {
    error.value = e.fieldErrors?.inviteCode ?? e.message
  } finally {
    joining.value = false
  }
}

onMounted(() => communities.fetchMine())
</script>

<template>
  <section class="card">
    <h1>我的社區</h1>

    <p v-if="!communities.mine.length" class="hint">你還沒有加入任何社區，請輸入社區提供的邀請碼。</p>
    <ul v-else class="list">
      <li v-for="c in communities.mine" :key="c.id">
        <RouterLink :to="{ name: 'community', params: { id: c.id } }" class="name">{{ c.name }}</RouterLink>
        <span class="badge" :class="c.role.toLowerCase()">{{ COMMUNITY_ROLE_LABELS[c.role] }}</span>
        <p v-if="c.description" class="desc">{{ c.description }}</p>
      </li>
    </ul>

    <form class="join" @submit.prevent="join">
      <h2>用邀請碼加入社區</h2>
      <div v-if="error" class="alert error">{{ error }}</div>
      <div class="row">
        <input v-model.trim="inviteCode" placeholder="例如 ABCD2345" aria-label="邀請碼" required />
        <button class="btn" type="submit" :disabled="joining">{{ joining ? '加入中…' : '加入' }}</button>
      </div>
    </form>
  </section>
</template>

<style scoped>
.list {
  list-style: none;
  padding: 0;
  margin: 0 0 1.5rem;
}
.list li {
  padding: 0.75rem 0;
  border-bottom: 1px solid var(--border);
}
.name {
  font-weight: 600;
  margin-right: 0.5rem;
}
.desc {
  margin: 0.25rem 0 0;
  color: var(--muted);
  font-size: 0.9rem;
}
.join h2 {
  font-size: 1.05rem;
  margin: 0 0 0.75rem;
}
.row {
  display: flex;
  gap: 0.5rem;
}
.row input {
  flex: 1;
  min-width: 0;
  padding: 0.55rem 0.7rem;
  font-size: 1rem;
  text-transform: uppercase;
  color: var(--text);
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 6px;
}
.row .btn {
  width: auto;
  padding: 0.55rem 1.25rem;
}
</style>
