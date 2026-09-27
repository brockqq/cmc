<script setup>
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useCommunityStore } from '@/stores/communities'

const auth = useAuthStore()
const communities = useCommunityStore()
const route = useRoute()
const router = useRouter()

const currentCommunityId = computed(() => (route.name === 'community' ? String(route.params.id) : ''))

function switchCommunity(event) {
  const id = event.target.value
  router.push(id ? { name: 'community', params: { id } } : { name: 'communities' })
}

function logout() {
  auth.logout()
  router.push({ name: 'login' })
}
</script>

<template>
  <header class="topbar">
    <RouterLink to="/" class="brand">Kata 社區</RouterLink>
    <nav>
      <template v-if="auth.isLoggedIn">
        <select
          v-if="communities.mine.length"
          class="switcher"
          aria-label="切換社區"
          :value="currentCommunityId"
          @change="switchCommunity"
        >
          <option value="">— 選擇社區 —</option>
          <option v-for="c in communities.mine" :key="c.id" :value="String(c.id)">{{ c.name }}</option>
        </select>
        <RouterLink to="/announcements">公布欄</RouterLink>
        <RouterLink to="/calendar">行事曆</RouterLink>
        <RouterLink v-if="communities.pettyCashCommunities.length" to="/petty-cash">零用金</RouterLink>
        <RouterLink to="/communities">我的社區</RouterLink>
        <template v-if="auth.isAdmin">
          <RouterLink to="/admin/communities">社區管理</RouterLink>
          <RouterLink to="/admin/users">會員管理</RouterLink>
        </template>
        <RouterLink to="/" class="who" v-if="auth.user">{{ auth.user.username }}</RouterLink>
        <button class="link" @click="logout">登出</button>
      </template>
      <template v-else>
        <RouterLink to="/login">登入</RouterLink>
        <RouterLink to="/register">註冊</RouterLink>
      </template>
    </nav>
  </header>

  <main class="container">
    <RouterView />
  </main>
</template>

<style scoped>
.topbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem 1rem;
  padding: 0.75rem 1.5rem;
  background: var(--surface);
  border-bottom: 1px solid var(--border);
}
.brand {
  font-weight: 700;
  color: var(--text);
}
nav {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem 1rem;
}
.who {
  color: var(--muted);
}
.switcher {
  max-width: 12rem;
}
.container {
  max-width: 800px;
  margin: 3rem auto;
  padding: 0 1rem;
}
</style>
