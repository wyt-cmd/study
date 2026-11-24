<template>
  <div class="page">
    <div class="toolbar">
      <el-button type="primary" @click="open()">新增轮播</el-button>
    </div>
    <el-table :data="rows">
      <el-table-column prop="title" label="标题" />
      <el-table-column label="图片" width="160">
        <template #default="{ row }">
          <img class="thumb" :src="fileUrl(row.imageUrl)" alt="" />
        </template>
      </el-table-column>
      <el-table-column prop="sortNo" label="排序" width="80" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">{{ row.status === 1 ? '启用' : '停用' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link @click="open(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" title="轮播" width="480px">
      <el-form label-width="70px">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="图片">
          <el-upload :show-file-list="false" :http-request="upload">
            <el-button>上传图片</el-button>
          </el-upload>
          <img v-if="form.imageUrl" class="thumb" :src="fileUrl(form.imageUrl)" alt="" />
        </el-form-item>
        <el-form-item label="链接"><el-input v-model="form.linkUrl" placeholder="可选，站内路径或网址" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortNo" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api'

const rows = ref([])
const visible = ref(false)
const form = reactive(blank())

function blank() {
  return { id: null, title: '', imageUrl: '', linkUrl: '', sortNo: 0, status: 1 }
}

function fileUrl(name) {
  if (!name) return ''
  if (name.startsWith('http')) return name
  return `/api/files/${name}`
}

async function load() {
  rows.value = await http.get('/banners')
}

function open(row) {
  Object.assign(form, blank(), row || {})
  visible.value = true
}

async function upload(option) {
  const body = new FormData()
  body.append('file', option.file)
  form.imageUrl = await http.post('/files', body)
}

async function save() {
  const payload = { title: form.title, imageUrl: form.imageUrl, linkUrl: form.linkUrl, sortNo: form.sortNo, status: form.status }
  if (form.id) {
    await http.put(`/banners/${form.id}`, payload)
  } else {
    await http.post('/banners', payload)
  }
  visible.value = false
  ElMessage.success('已保存')
  load()
}

async function remove(row) {
  await ElMessageBox.confirm('删除后首页不再展示', '确认删除')
  await http.delete(`/banners/${row.id}`)
  load()
}

onMounted(load)
</script>

<style scoped>
.thumb { width: 120px; height: 48px; object-fit: cover; margin-left: 8px; vertical-align: middle; }
</style>
