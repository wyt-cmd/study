<template>
  <div class="page">
    <div class="toolbar">
      <el-select v-model="query.categoryId" clearable placeholder="分类" style="width: 160px">
        <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-select v-model="query.type" clearable placeholder="题型" style="width: 140px">
        <el-option v-for="item in types" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-input v-model="query.keyword" placeholder="题干" style="width: 200px" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button @click="open()">新增题目</el-button>
      <el-button @click="openCategories">分类维护</el-button>
      <el-button @click="aiVisible = true">AI 出题</el-button>
      <el-upload :show-file-list="false" :http-request="upload">
        <el-button>导入 Excel</el-button>
      </el-upload>
      <el-button @click="downloadTemplate">下载模板</el-button>
    </div>
    <el-table :data="rows">
      <el-table-column prop="categoryName" label="分类" width="120" />
      <el-table-column prop="type" label="题型" width="90" />
      <el-table-column prop="stem" label="题干" show-overflow-tooltip />
      <el-table-column prop="score" label="分值" width="70" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="open(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      style="margin-top: 12px"
      layout="total, prev, pager, next"
      :total="total"
      v-model:current-page="query.current"
      :page-size="query.size"
      @current-change="load"
    />

    <el-dialog v-model="visible" title="题目" width="640px">
      <el-form label-width="80px">
        <el-form-item label="分类">
          <el-select v-model="form.categoryId">
            <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="题型">
          <el-select v-model="form.type">
            <el-option v-for="item in types" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="form.difficulty">
            <el-option :value="1" label="易" />
            <el-option :value="2" label="中" />
            <el-option :value="3" label="难" />
          </el-select>
        </el-form-item>
        <el-form-item label="分值"><el-input-number v-model="form.score" :min="1" /></el-form-item>
        <el-form-item label="题干"><el-input v-model="form.stem" type="textarea" /></el-form-item>
        <el-form-item v-if="form.type !== 'SHORT' && form.type !== 'JUDGE'" label="选项">
          <div v-for="(opt, index) in form.options" :key="index" class="opt">
            <el-input v-model="opt.key" style="width: 70px" />
            <el-input v-model="opt.text" />
          </div>
        </el-form-item>
        <el-form-item label="答案"><el-input v-model="form.answer" placeholder="单选 A，多选 A,B，判断 T/F" /></el-form-item>
        <el-form-item label="关键词"><el-input v-model="form.keywords" placeholder="主观题用逗号分隔" /></el-form-item>
        <el-form-item label="解析"><el-input v-model="form.analysis" type="textarea" /></el-form-item>
        <el-form-item label="配图">
          <el-upload :show-file-list="false" :http-request="uploadImage">
            <el-button>上传</el-button>
          </el-upload>
          <span v-if="form.imageUrl" class="hint">{{ form.imageUrl }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="categoryVisible" title="分类维护" width="640px">
      <el-table :data="categoryRows">
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="parentName" label="父分类" width="120" />
        <el-table-column prop="sortNo" label="排序" width="70" />
        <el-table-column prop="questionCount" label="题目数" width="80" />
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button link @click="editCategory(row)">编辑</el-button>
            <el-button link type="danger" @click="removeCategory(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-form label-width="70px" style="margin-top: 16px">
        <el-form-item label="名称"><el-input v-model="categoryForm.name" /></el-form-item>
        <el-form-item label="父分类">
          <el-select v-model="categoryForm.parentId" clearable placeholder="无">
            <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" :disabled="item.id === categoryForm.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="categoryForm.sortNo" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetCategory">清空</el-button>
        <el-button type="primary" @click="saveCategory">保存分类</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="aiVisible" title="AI 出题" width="520px">
      <el-form label-width="70px">
        <el-form-item label="分类">
          <el-select v-model="ai.categoryId">
            <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="题型">
          <el-select v-model="ai.type">
            <el-option v-for="item in types" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="ai.difficulty">
            <el-option :value="1" label="易" />
            <el-option :value="2" label="中" />
            <el-option :value="3" label="难" />
          </el-select>
        </el-form-item>
        <el-form-item label="数量"><el-input-number v-model="ai.count" :min="1" :max="20" /></el-form-item>
        <el-form-item label="分值"><el-input-number v-model="ai.score" :min="1" /></el-form-item>
        <el-form-item label="主题"><el-input v-model="ai.topic" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="generate">生成并入库</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api'

const types = [
  { value: 'SINGLE', label: '单选' },
  { value: 'MULTI', label: '多选' },
  { value: 'JUDGE', label: '判断' },
  { value: 'SHORT', label: '简答' }
]
const categories = ref([])
const rows = ref([])
const total = ref(0)
const visible = ref(false)
const categoryVisible = ref(false)
const aiVisible = ref(false)
const editingId = ref(null)
const categoryRows = ref([])
const query = reactive({ current: 1, size: 10, categoryId: null, type: '', keyword: '' })
const form = reactive(blank())
const categoryForm = reactive({ id: null, name: '', parentId: null, sortNo: 0 })
const ai = reactive({ categoryId: null, type: 'SINGLE', difficulty: 2, count: 3, score: 5, topic: '' })

function blank() {
  return {
    categoryId: null,
    type: 'SINGLE',
    difficulty: 1,
    score: 5,
    stem: '',
    options: [
      { key: 'A', text: '' },
      { key: 'B', text: '' },
      { key: 'C', text: '' },
      { key: 'D', text: '' }
    ],
    answer: '',
    keywords: '',
    analysis: '',
    imageUrl: ''
  }
}

function flatten(nodes, parentName = '') {
  const rows = []
  for (const node of nodes) {
    rows.push({ ...node, parentName: parentName || '—' })
    if (node.children?.length) {
      rows.push(...flatten(node.children, node.name))
    }
  }
  return rows
}

async function reloadCategories() {
  categories.value = await http.get('/categories')
  categoryRows.value = flatten(await http.get('/categories/tree'))
}

async function load() {
  const data = await http.get('/questions', { params: query })
  rows.value = data.records
  total.value = data.total
}

function open(row) {
  Object.assign(form, blank(), row || {})
  if (!form.options?.length) {
    form.options = blank().options
  }
  editingId.value = row?.id || null
  visible.value = true
}

async function save() {
  if (editingId.value) {
    await http.put(`/questions/${editingId.value}`, form)
  } else {
    await http.post('/questions', form)
  }
  visible.value = false
  ElMessage.success('已保存')
  load()
}

async function remove(row) {
  await ElMessageBox.confirm('删除后不可恢复', '确认删除')
  await http.delete(`/questions/${row.id}`)
  load()
}

async function downloadTemplate() {
  const res = await http.get('/questions/import/template', { responseType: 'blob' })
  const url = URL.createObjectURL(res.data)
  const a = document.createElement('a')
  a.href = url
  a.download = 'questions.xlsx'
  a.click()
  URL.revokeObjectURL(url)
}

async function upload(option) {
  const previewBody = new FormData()
  previewBody.append('file', option.file)
  const preview = await http.post('/questions/import/preview', previewBody)
  if (preview.errors?.length) {
    ElMessage.error(preview.errors.slice(0, 3).join('；'))
    return
  }
  const body = new FormData()
  body.append('file', option.file)
  const result = await http.post('/questions/import', body)
  ElMessage.success(`导入 ${result.inserted} 条，耗时 ${result.elapsedMs} ms`)
  load()
}

async function uploadImage(option) {
  const body = new FormData()
  body.append('file', option.file)
  form.imageUrl = await http.post('/files', body)
}

async function openCategories() {
  resetCategory()
  await reloadCategories()
  categoryVisible.value = true
}

function resetCategory() {
  Object.assign(categoryForm, { id: null, name: '', parentId: null, sortNo: 0 })
}

function editCategory(row) {
  Object.assign(categoryForm, { id: row.id, name: row.name, parentId: row.parentId || null, sortNo: row.sortNo })
}

async function saveCategory() {
  const payload = { name: categoryForm.name, parentId: categoryForm.parentId || 0, sortNo: categoryForm.sortNo }
  if (categoryForm.id) {
    await http.put(`/categories/${categoryForm.id}`, payload)
  } else {
    await http.post('/categories', payload)
  }
  ElMessage.success('分类已保存')
  resetCategory()
  await reloadCategories()
}

async function removeCategory(row) {
  await ElMessageBox.confirm('仅空分类可以删除', '确认删除')
  await http.delete(`/categories/${row.id}`)
  await reloadCategories()
}

async function generate() {
  const drafts = await http.post('/questions/ai-draft', ai)
  for (const draft of drafts) {
    await http.post('/questions', draft)
  }
  aiVisible.value = false
  ElMessage.success(`已入库 ${drafts.length} 题`)
  load()
}

onMounted(async () => {
  await reloadCategories()
  await load()
})
</script>

<style scoped>
.opt { display: flex; gap: 8px; margin-bottom: 6px; }
.hint { margin-left: 8px; color: #667085; }
</style>
