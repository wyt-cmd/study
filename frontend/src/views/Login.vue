<template>
  <div class="login">
    <el-card class="card">
      <h2>智能刷题与在线考试</h2>
      <p>使用本地账号登录。演示账号由服务端初始化，密码不写在页面里。</p>
      <el-form @submit.prevent="submit">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" show-password placeholder="密码" @keyup.enter="submit" />
        </el-form-item>
        <el-button type="primary" :loading="loading" style="width: 100%" @click="submit">登录</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '../api'

const router = useRouter()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

async function submit() {
  loading.value = true
  try {
    const data = await http.post('/auth/login', form)
    localStorage.setItem('token', data.token)
    localStorage.setItem('profile', JSON.stringify(data))
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login {
  min-height: 100%;
  display: grid;
  place-items: center;
}
.card {
  width: 380px;
}
h2 {
  margin: 0 0 8px;
}
p {
  color: #667085;
  font-size: 13px;
}
</style>
