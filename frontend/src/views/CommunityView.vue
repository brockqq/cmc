<script setup>
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import { COMMUNITY_ROLE_LABELS } from '@/stores/communities'
import { formatEventRange } from '@/utils/datetime'
import { createLatest } from '@/utils/latest'

const auth = useAuthStore()
const route = useRoute()

const community = ref(null)
const members = ref([])
const announcements = ref([])
const events = ref([])
const error = ref('')
const message = ref('')
const busyId = ref(null)
const copied = ref(false)

// Moving between communities quickly must not show the previous community's data under the new one
const loads = createLatest()

async function load(id) {
  const isCurrent = loads.begin()
  community.value = null
  members.value = []
  announcements.value = []
  events.value = []
  error.value = ''
  message.value = ''
  try {
    const info = await api.communities.get(id)
    if (!isCurrent()) return
    const [latest, upcoming, memberList] = await Promise.all([
      api.announcements.list({ communityId: id, size: 3 }),
      api.events.list({ communityId: id }),
      info.canManage ? api.communities.members(id) : [],
    ])
    if (!isCurrent()) return
    community.value = info
    announcements.value = latest.items
    events.value = upcoming.slice(0, 5)
    members.value = memberList
  } catch (e) {
    if (isCurrent()) error.value = e.message
  }
}

watch(() => route.params.id, (id) => id && load(id), { immediate: true })

async function run(action, successMessage) {
  error.value = ''
  message.value = ''
  try {
    await action()
    if (successMessage) message.value = successMessage
  } catch (e) {
    error.value = e.message
  }
}

async function changeRole(member, event) {
  const role = event.target.value
  busyId.value = member.userId
  await run(async () => {
    try {
      Object.assign(member, await api.communities.updateMemberRole(community.value.id, member.userId, role))
    } catch (e) {
      event.target.value = member.role // revert the select
      throw e
    }
  }, `已將 ${member.username} 設為${COMMUNITY_ROLE_LABELS[role]}`)
  busyId.value = null
}

async function remove(member) {
  if (!confirm(`確定要將 ${member.username} 移出「${community.value.name}」嗎？`)) return
  busyId.value = member.userId
  await run(async () => {
    await api.communities.removeMember(community.value.id, member.userId)
    members.value = members.value.filter((m) => m.userId !== member.userId)
    community.value.memberCount--
  }, `已將 ${member.username} 移出社區`)
  busyId.value = null
}

async function regenerate() {
  if (!confirm('重新產生後，舊的邀請碼將立即失效。確定要繼續嗎？')) return
  await run(async () => {
    const res = await api.communities.regenerateInviteCode(community.value.id)
    community.value.inviteCode = res.inviteCode
  }, '已產生新的邀請碼')
}

async function copyCode() {
  try {
    await navigator.clipboard.writeText(community.value.inviteCode)
    copied.value = true
    setTimeout(() => (copied.value = false), 1500)
  } catch {
    // Clipboard may be unavailable (e.g. non-HTTPS); the code is visible to copy manually
  }
}

const formatDate = (iso) => new Date(iso).toLocaleDateString('zh-TW')
</script>

<template>
  <div v-if="error && !community" class="card">
    <div class="alert error">{{ error }}</div>
    <RouterLink to="/communities">回到我的社區</RouterLink>
  </div>

  <template v-else-if="community">
    <section class="card">
      <h1>{{ community.name }}</h1>
      <p v-if="community.description" class="desc">{{ community.description }}</p>
      <p class="meta">
        <span v-if="community.myRole" class="badge" :class="community.myRole.toLowerCase()">
          {{ COMMUNITY_ROLE_LABELS[community.myRole] }}
        </span>
        <span v-else-if="community.canManage" class="badge manager">平台管理員</span>
        <span>{{ community.memberCount }} 位成員</span>
        <RouterLink
          v-if="community.canManage || community.myRole === 'COMMITTEE'"
          :to="{ name: 'petty-cash', query: { community: community.id } }"
        >
          零用金 ›
        </RouterLink>
      </p>

      <div v-if="community.canManage" class="invite">
        <span class="label">邀請碼</span>
        <code>{{ community.inviteCode }}</code>
        <button class="link" @click="copyCode">{{ copied ? '已複製' : '複製' }}</button>
        <button class="link" @click="regenerate">重新產生</button>
      </div>
    </section>

    <div class="panels">
      <section class="card">
        <div class="panel-head">
          <h2>最新公告</h2>
          <RouterLink :to="{ name: 'announcements', query: { scope: community.id } }">全部 ›</RouterLink>
        </div>
        <p v-if="!announcements.length" class="hint left">尚無公告。</p>
        <ul class="brief">
          <li v-for="a in announcements" :key="a.id">
            <span v-if="a.pinned" class="badge pin">置頂</span>
            <span class="title">{{ a.title }}</span>
            <span class="muted">{{ formatDate(a.createdAt) }}</span>
          </li>
        </ul>
      </section>

      <section class="card">
        <div class="panel-head">
          <h2>近期活動</h2>
          <RouterLink to="/calendar">行事曆 ›</RouterLink>
        </div>
        <p v-if="!events.length" class="hint left">近期沒有活動。</p>
        <ul class="brief">
          <li v-for="e in events" :key="`${e.id}@${e.originalStart}`">
            <span class="title">{{ e.title }}</span>
            <span class="muted">{{ formatEventRange(e.startAt, e.endAt) }}</span>
          </li>
        </ul>
      </section>
    </div>

    <section v-if="community.canManage" class="card">
      <h2>成員管理</h2>
      <div v-if="error" class="alert error">{{ error }}</div>
      <div v-if="message" class="alert success">{{ message }}</div>

      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>帳號</th>
              <th>Email</th>
              <th>加入日期</th>
              <th>角色</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="m in members" :key="m.userId">
              <td>{{ m.username }}</td>
              <td>{{ m.email }}</td>
              <td>{{ formatDate(m.joinedAt) }}</td>
              <template v-if="m.username === auth.user?.username">
                <td class="muted">{{ COMMUNITY_ROLE_LABELS[m.role] }}（自己）</td>
                <td></td>
              </template>
              <!-- Managers cannot change or remove each other; only a platform admin can -->
              <template v-else-if="m.role === 'MANAGER' && !auth.isAdmin">
                <td class="muted" title="只有平台管理員可以變更或移除社區管理員">{{ COMMUNITY_ROLE_LABELS[m.role] }}</td>
                <td></td>
              </template>
              <template v-else>
                <td>
                  <select :value="m.role" :disabled="busyId === m.userId" @change="changeRole(m, $event)">
                    <option v-for="(label, value) in COMMUNITY_ROLE_LABELS" :key="value" :value="value">
                      {{ label }}
                    </option>
                  </select>
                </td>
                <td>
                  <button class="link danger" :disabled="busyId === m.userId" @click="remove(m)">移除</button>
                </td>
              </template>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-if="!members.length" class="hint">目前沒有成員，把邀請碼分享給住戶即可加入。</p>
    </section>
  </template>

  <p v-else class="hint">載入中…</p>
</template>

<style scoped>
.card + .card {
  margin-top: 1.25rem;
}
h2 {
  font-size: 1.1rem;
  margin: 0 0 1rem;
}
.desc {
  margin: -0.5rem 0 0.75rem;
  color: var(--muted);
}
.meta {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin: 0;
  color: var(--muted);
  font-size: 0.9rem;
}
.invite {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem;
  margin-top: 1.25rem;
  padding: 0.75rem 1rem;
  background: var(--bg);
  border: 1px dashed var(--border);
  border-radius: 6px;
}
.invite .label {
  color: var(--muted);
  font-size: 0.9rem;
}
.invite code {
  font-size: 1.2rem;
  font-weight: 700;
  letter-spacing: 0.15em;
}
.muted {
  color: var(--muted);
}
.panels {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 1.25rem;
  margin-top: 1.25rem;
}
.panels .card {
  margin-top: 0;
}
.panels + .card {
  margin-top: 1.25rem;
}
.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}
.hint.left {
  text-align: left;
  margin-top: 0;
}
.brief {
  list-style: none;
  margin: 0;
  padding: 0;
}
.brief li {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 0.25rem 0.5rem;
  padding: 0.4rem 0;
  border-bottom: 1px solid var(--border);
}
.brief li:last-child {
  border-bottom: none;
}
.brief .title {
  flex: 1;
  min-width: 8rem;
}
.brief .muted {
  font-size: 0.85rem;
}
</style>
