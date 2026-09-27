import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import HomeView from '../views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
      meta: { requiresAuth: true },
    },
    {
      path: '/account/password',
      name: 'change-password',
      component: () => import('../views/ChangePasswordView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/announcements',
      name: 'announcements',
      component: () => import('../views/AnnouncementsView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/calendar',
      name: 'calendar',
      component: () => import('../views/CalendarView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/petty-cash',
      name: 'petty-cash',
      component: () => import('../views/PettyCashView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/communities',
      name: 'communities',
      component: () => import('../views/CommunitiesView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/communities/:id',
      name: 'community',
      component: () => import('../views/CommunityView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/admin/users',
      name: 'admin-users',
      component: () => import('../views/AdminUsersView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/admin/communities',
      name: 'admin-communities',
      component: () => import('../views/AdminCommunitiesView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('../views/RegisterView.vue'),
      meta: { guestOnly: true },
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  // After a page reload only the token survives; load the user (and role) before checking access
  if (auth.isLoggedIn && !auth.user) {
    try {
      await auth.fetchMe()
    } catch (e) {
      // Only an invalid/expired token ends the session; a network or server error should not log the user out
      if (e.status === 401) auth.logout()
    }
  }

  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAdmin && !auth.isAdmin) {
    return { name: 'home' }
  }
  if (to.meta.guestOnly && auth.isLoggedIn) {
    return { name: 'home' }
  }
})

export default router
