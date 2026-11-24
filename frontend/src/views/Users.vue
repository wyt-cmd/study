<template>
  <div class="page">
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="用户名" style="width: 180px" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button @click="open()">新增用户</el-button>
    </div>
    <el-table :data="rows">
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column prop="role" label="角色" width="120" />
      <el-table-column prop="status" label="状态" width="80" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button link @click="open(row)">编辑</el-button>
          <el-button link @click="toggle(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="visible" title="用户" width="420px">
      <el-form label-width="70px">
        <el-form-item label="用户名"><el-input v-model="form.username" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="昵称"><el-input v-model="form.nickname" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="教师" value="TEACHER" />
            <el-option label="考生" value="STUDENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="密码"><el-input v-model="form.password" placeholder="编辑时留空则不修改" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../api'

const rows = ref([])
const keyword = ref('')
const visible = ref(false)
const form = reactive({ id: null, username: '', nickname: '', role: 'STUDENT', password: '' })

async function load() {
  rows.value = (await http.get('/users', { params: { current: 1, size: 50, keyword: keyword.value } })).records
}

function open(row) {
  Object.assign(form, { id: null, username: '', nickname: '', role: 'STUDENT', password: '' }, row || {})
  form.password = ''
  visible.value = true
}

async function save() {
  if (form.id) {
    await http.put(`/users/${form.id}`, form)
  } else {
    await http.post('/users', form)
  }
  visible.value = false
  ElMessage.success('已保存')
  load()
}

async function toggle(row) {
  await http.put(`/users/${row.id}/status`, null, { params: { status: row.status === 1 ? 0 : 1 } })
  load()
}

onMounted(load)
</script>
