<template>
  <el-container class="shell">
    <el-aside width="220px" class="aside">
      <div class="brand">考试系统</div>
      <el-menu :default-active="$route.path" router>
        <el-menu-item index="/">首页</el-menu-item>
        <el-menu-item index="/practice">智能刷题</el-menu-item>
        <el-menu-item index="/exams">在线考试</el-menu-item>
        <el-menu-item index="/stats">成绩统计</el-menu-item>
        <el-menu-item v-if="staff" index="/questions">题目管理</el-menu-item>
        <el-menu-item v-if="staff" index="/papers">试卷管理</el-menu-item>
        <el-menu-item v-if="profile.role === 'ADMIN'" index="/users">用户管理</el-menu-item>
        <el-menu-item v-if="profile.role === 'ADMIN'" index="/banners">轮播管理</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span>{{ profile.nickname }}（{{ roleName }}）</span>
        <el-button link type="primary" @click="logout">退出</el-button>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import http from '../api'

const router = useRouter()
const profile = computed(() => JSON.parse(localStorage.getItem('profile') || '{}'))
const staff = computed(() => ['ADMIN', 'TEACHER'].includes(profile.value.role))
const roleName = computed(() => ({ ADMIN: '管理员', TEACHER: '教师', STUDENT: '考生' }[profile.value.role] || ''))

async function logout() {
  try {
    await http.post('/auth/logout')
  } catch {
    /* 令牌已失效时仍清理本地登录态 */
  }
  localStorage.removeItem('token')
  localStorage.removeItem('profile')
  router.push('/login')
}
</script>

<style scoped>
.shell {
  height: 100vh;
}
.aside {
  background: #111827;
}
.brand {
  color: #fff;
  font-weight: 700;
  padding: 18px 16px;
}
.header {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}
:deep(.el-menu) {
  border-right: none;
  background: transparent;
}
:deep(.el-menu-item) {
  color: #d1d5db;
}
:deep(.el-menu-item.is-active) {
  background: #1f2937;
  color: #fff;
}
</style>
