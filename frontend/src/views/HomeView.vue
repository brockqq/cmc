<script setup>
import { onMounted, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { COMMUNITY_ROLE_LABELS, useCommunityStore } from '@/stores/communities'

const auth = useAuthStore()
const communities = useCommunityStore()
const error = ref('')

onMounted(async () => {
  try {
    await Promise.all([auth.fetchMe(), communities.fetchMine()])
  } catch (e) {
    // 401 is handled globally (redirect to login)
    if (e.status !== 401) error.value = e.message
  }
})

const formatDate = (iso) => new Date(iso).toLocaleString('zh-TW')
</script>

<template>
  <section class="card narrow">
    <h1>會員中心</h1>
    <div v-if="error" class="alert error">{{ error }}</div>
    <template v-else-if="auth.user">
      <dl class="profile">
        <dt>會員編號</dt>
        <dd>{{ auth.user.id }}</dd>
        <dt>帳號</dt>
        <dd>{{ auth.user.username }}</dd>
        <dt>Email</dt>
        <dd>{{ auth.user.email }}</dd>
        <template v-if="auth.isAdmin">
          <dt>平台角色</dt>
          <dd><span class="badge manager">平台管理員</span></dd>
        </template>
        <dt>所屬社區</dt>
        <dd>
          <span v-if="!communities.mine.length" class="muted">
            尚未加入 · <RouterLink to="/communities">用邀請碼加入</RouterLink>
          </span>
          <ul v-else class="communities">
            <li v-for="c in communities.mine" :key="c.id">
              <RouterLink :to="{ name: 'community', params: { id: c.id } }">{{ c.name }}</RouterLink>
              <span class="badge" :class="c.role.toLowerCase()">{{ COMMUNITY_ROLE_LABELS[c.role] }}</span>
            </li>
          </ul>
        </dd>
        <dt>註冊時間</dt>
        <dd>{{ formatDate(auth.user.createdAt) }}</dd>
      </dl>
      <p class="hint"><RouterLink to="/account/password">修改密碼</RouterLink></p>
    </template>
    <p v-else class="hint">載入中…</p>
  </section>
</template>

<style scoped>
.profile {
  display: grid;
  grid-template-columns: max-content 1fr;
  gap: 0.6rem 1.25rem;
  margin: 0;
}
.profile dt {
  color: var(--muted);
}
.profile dd {
  margin: 0;
  word-break: break-all;
}
.communities {
  list-style: none;
  margin: 0;
  padding: 0;
}
.communities li + li {
  margin-top: 0.3rem;
}
.communities .badge {
  margin-left: 0.4rem;
}
.muted {
  color: var(--muted);
}
</style>
