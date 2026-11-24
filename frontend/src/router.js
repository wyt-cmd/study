import { createRouter, createWebHistory } from 'vue-router'
import Login from './views/Login.vue'
import Layout from './views/Layout.vue'
import Home from './views/Home.vue'
import Questions from './views/Questions.vue'
import Papers from './views/Papers.vue'
import ExamList from './views/ExamList.vue'
import ExamRoom from './views/ExamRoom.vue'
import Practice from './views/Practice.vue'
import Stats from './views/Stats.vue'
import Users from './views/Users.vue'
import Banners from './views/Banners.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: Login },
    {
      path: '/',
      component: Layout,
      children: [
        { path: '', component: Home },
        { path: 'questions', component: Questions, meta: { roles: ['ADMIN', 'TEACHER'] } },
        { path: 'papers', component: Papers, meta: { roles: ['ADMIN', 'TEACHER'] } },
        { path: 'exams', component: ExamList },
        { path: 'exams/:id', component: ExamRoom },
        { path: 'practice', component: Practice },
        { path: 'stats', component: Stats },
        { path: 'users', component: Users, meta: { roles: ['ADMIN'] } },
        { path: 'banners', component: Banners, meta: { roles: ['ADMIN'] } }
      ]
    }
  ]
})

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    return '/login'
  }
  if (to.path === '/login' && token) {
    return '/'
  }
  const roles = to.meta.roles
  if (roles) {
    const profile = JSON.parse(localStorage.getItem('profile') || '{}')
    if (!roles.includes(profile.role)) {
      return '/'
    }
  }
  return true
})

export default router
