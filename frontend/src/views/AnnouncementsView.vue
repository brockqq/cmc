<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import { useCommunityStore } from '@/stores/communities'
import AnnouncementForm from '@/components/AnnouncementForm.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import { formatDateTime } from '@/utils/datetime'
import { createLatest } from '@/utils/latest'

const PAGE_SIZES = [5, 10, 20, 50]
const SIZE_KEY = 'kata.announcements.size'

const auth = useAuthStore()
const communities = useCommunityStore()
const route = useRoute()
const router = useRouter()

const result = ref({ items: [], page: 0, size: 10, totalItems: 0, totalPages: 0 })
const loading = ref(true)
const error = ref('')
const composing = ref(false)
const editingId = ref(null)

function savedSize() {
  try {
    const n = Number(localStorage.getItem(SIZE_KEY))
    return PAGE_SIZES.includes(n) ? n : 10
  } catch {
    return 10
  }
}

// View state lives in the URL (?scope=platform|<communityId>&page=2&size=20) so reloads and shared links keep it
const scope = computed(() => {
  const s = route.query.scope
  return !s ? 'all' : s === 'platform' ? 'platform' : Number(s)
})
const page = computed(() => Math.max(1, Number(route.query.page) || 1))
const size = computed(() => {
  const n = Number(route.query.size)
  return PAGE_SIZES.includes(n) ? n : savedSize()
})

function setQuery(changes) {
  const next = { scope: scope.value, page: page.value, size: size.value, ...changes }
  router.replace({
    query: {
      ...(next.scope !== 'all' && { scope: String(next.scope) }),
      ...(next.page > 1 && { page: String(next.page) }),
      size: String(next.size),
    },
  })
}

const setScope = (s) => setQuery({ scope: s, page: 1 })
const setPage = (p) => setQuery({ page: p })
function setSize(s) {
  try {
    localStorage.setItem(SIZE_KEY, String(s))
  } catch {
    // storage unavailable (private mode) — the URL still carries the size
  }
  setQuery({ size: s, page: 1 })
}

/** Where the user may post: platform-wide (admins) plus the communities they manage. */
const scopes = computed(() => [
  ...(auth.isAdmin ? [{ value: null, label: '全平台' }] : []),
  ...communities.manageable.map((c) => ({ value: c.id, label: c.name })),
])

const filters = computed(() => [
  { key: 'all', label: '全部' },
  { key: 'platform', label: '全平台' },
  // Platform admins can read any community; everyone else only the ones they joined
  ...(auth.isAdmin ? communities.manageable : communities.mine).map((c) => ({ key: c.id, label: c.name })),
])

// Paging or switching scope quickly must not let an older response overwrite a newer one
const loads = createLatest()

async function load() {
  const isCurrent = loads.begin()
  loading.value = true
  error.value = ''
  try {
    const res = await api.announcements.list({
      communityId: typeof scope.value === 'number' ? scope.value : undefined,
      platform: scope.value === 'platform',
      page: page.value - 1,
      size: size.value,
    })
    if (!isCurrent()) return
    // e.g. the last item on the last page was deleted, or a stale link: jump to the last existing page
    if (!res.items.length && res.totalPages > 0 && page.value > res.totalPages) {
      setPage(res.totalPages)
      return
    }
    result.value = res
  } catch (e) {
    if (isCurrent()) error.value = e.message
  } finally {
    if (isCurrent()) loading.value = false
  }
}

// Guard: the query also changes when navigating away from this page
watch(() => route.query, () => route.name === 'announcements' && load())

async function create(payload) {
  await api.announcements.create(payload)
  composing.value = false
  // Show the new post: first page of its scope, unless the current view already includes it
  // (an admin posting to a community they have not joined won't see it under "all")
  const target = payload.communityId ?? 'platform'
  const inCurrentView =
    scope.value === target ||
    (scope.value === 'all' && (target === 'platform' || communities.mine.some((c) => c.id === target)))
  if (inCurrentView && page.value === 1) await load()
  else setQuery({ scope: inCurrentView ? scope.value : target, page: 1 })
}

async function update(announcement, payload) {
  await api.announcements.update(announcement.id, payload)
  editingId.value = null
  await load()
}

async function remove(announcement) {
  if (!confirm(`確定要刪除「${announcement.title}」嗎？`)) return
  try {
    await api.announcements.remove(announcement.id)
    await load()
  } catch (e) {
    error.value = e.message
  }
}

onMounted(async () => {
  await Promise.all([load(), communities.fetchManageable().catch(() => {})])
})
</script>

<template>
  <section class="card">
    <div class="head">
      <h1>公布欄</h1>
      <button v-if="scopes.length && !composing" class="btn small" @click="composing = true">發布公告</button>
    </div>

    <AnnouncementForm v-if="composing" :scopes="scopes" :save="create" @cancel="composing = false" />

    <div class="chips" role="tablist" aria-label="篩選公告">
      <button
        v-for="f in filters"
        :key="f.key"
        class="chip"
        :class="{ active: scope === f.key }"
        role="tab"
        :aria-selected="scope === f.key"
        @click="setScope(f.key)"
      >
        {{ f.label }}
      </button>
    </div>

    <div v-if="error" class="alert error">{{ error }}</div>
    <p v-if="loading && !result.items.length" class="hint">載入中…</p>
    <p v-else-if="!result.items.length" class="hint">目前沒有公告。</p>

    <div :class="{ dim: loading }">
      <article v-for="a in result.items" :key="a.id" class="post" :class="{ pinned: a.pinned }">
        <AnnouncementForm
          v-if="editingId === a.id"
          :initial="a"
          :scopes="scopes"
          :save="(payload) => update(a, payload)"
          @cancel="editingId = null"
        />
        <template v-else>
          <div class="tags">
            <span v-if="a.pinned" class="badge pin">置頂</span>
            <span class="badge" :class="{ manager: a.communityId == null }">{{ a.communityName ?? '全平台' }}</span>
          </div>
          <h2>{{ a.title }}</h2>
          <p class="content">{{ a.content }}</p>
          <footer>
            <span>{{ a.author }} · {{ formatDateTime(a.createdAt) }}</span>
            <span v-if="a.updatedAt !== a.createdAt">（已編輯）</span>
            <span v-if="a.canEdit" class="ops">
              <button class="link" @click="editingId = a.id">編輯</button>
              <button class="link danger" @click="remove(a)">刪除</button>
            </span>
          </footer>
        </template>
      </article>
    </div>

    <PaginationBar
      v-if="result.totalItems"
      :page="page"
      :size="size"
      :total-pages="result.totalPages"
      :total-items="result.totalItems"
      :sizes="PAGE_SIZES"
      @update:page="setPage"
      @update:size="setSize"
    />
  </section>
</template>

<style scoped>
.post {
  padding: 1rem 0;
  border-top: 1px solid var(--border);
}
.post.pinned {
  background: linear-gradient(90deg, var(--pin-bg), transparent 60%);
  margin: 0 -1.75rem;
  padding-left: 1.75rem;
  padding-right: 1.75rem;
}
.dim {
  opacity: 0.6;
  transition: opacity 0.15s;
}
.tags {
  display: flex;
  gap: 0.4rem;
}
.post h2 {
  margin: 0.4rem 0 0.3rem;
  font-size: 1.1rem;
}
.content {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}
footer {
  margin-top: 0.5rem;
  color: var(--muted);
  font-size: 0.85rem;
}
.ops {
  margin-left: 1rem;
  display: inline-flex;
  gap: 0.75rem;
}
</style>
