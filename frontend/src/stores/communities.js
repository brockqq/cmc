import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import { api } from '@/api/http'
import { useAuthStore } from './auth'

export const COMMUNITY_ROLE_LABELS = { MANAGER: '社區管理員', COMMITTEE: '行政委員', RESIDENT: '住戶' }

/** The communities the current user belongs to (drives the nav switcher and filters). */
export const useCommunityStore = defineStore('communities', () => {
  const auth = useAuthStore()
  const mine = ref([])
  const allForAdmin = ref([])

  /** Communities the user may post announcements / events to. Platform admins may use any community. */
  const manageable = computed(() =>
    auth.isAdmin ? allForAdmin.value : mine.value.filter((c) => c.role === 'MANAGER'),
  )

  /** Communities whose petty cash the user can see: managers and committee members (admins: all). */
  const pettyCashCommunities = computed(() =>
    auth.isAdmin ? allForAdmin.value : mine.value.filter((c) => c.role === 'MANAGER' || c.role === 'COMMITTEE'),
  )

  async function fetchMine() {
    mine.value = auth.isLoggedIn ? await api.communities.mine() : []
    return mine.value
  }

  async function fetchManageable() {
    if (auth.isAdmin) {
      allForAdmin.value = await api.admin.listCommunities()
    } else {
      await fetchMine()
    }
    return manageable.value
  }

  async function join(inviteCode) {
    const joined = await api.communities.join(inviteCode)
    await fetchMine()
    return joined
  }

  // Drop cached memberships when the user logs out or switches account
  watch(
    () => auth.token,
    (token) => {
      allForAdmin.value = []
      if (token) fetchMine().catch(() => {})
      else mine.value = []
    },
    { immediate: true },
  )

  // Platform admins see every community; load the list once we know the user is one (drives nav links)
  watch(
    () => auth.isAdmin,
    (isAdmin) => isAdmin && fetchManageable().catch(() => {}),
    { immediate: true },
  )

  return { mine, manageable, pettyCashCommunities, fetchMine, fetchManageable, join }
})
