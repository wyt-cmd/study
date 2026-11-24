<template>
  <div class="page">
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="试卷标题" style="width: 200px" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button @click="openManual">手工组卷</el-button>
      <el-button @click="openSmart">智能组卷</el-button>
    </div>
    <el-table :data="rows">
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="assembleMode" label="方式" width="100" />
      <el-table-column prop="totalScore" label="总分" width="80" />
      <el-table-column prop="durationMin" label="时长" width="80" />
      <el-table-column prop="status" label="状态" width="110" />
      <el-table-column label="操作" width="260">
        <template #default="{ row }">
          <el-button link @click="change(row, 'PUBLISHED')">发布</el-button>
          <el-button link @click="change(row, 'CLOSED')">关闭</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top: 12px" layout="total, prev, pager, next" :total="total" v-model:current-page="current" @current-change="load" />

    <el-dialog v-model="manualVisible" title="手工组卷" width="720px">
      <el-form label-width="80px">
        <el-form-item label="标题"><el-input v-model="manual.title" /></el-form-item>
        <el-form-item label="时长"><el-input-number v-model="manual.durationMin" :min="1" :max="300" /></el-form-item>
        <el-form-item label="总分"><el-input-number v-model="manual.totalScore" :min="1" /></el-form-item>
        <el-form-item label="及格"><el-input-number v-model="manual.passScore" :min="0" /></el-form-item>
        <el-form-item label="题目">
          <div v-for="(item, index) in manual.questions" :key="index" class="line">
            <el-select v-model="item.questionId" filterable placeholder="选择题目" style="width: 420px">
              <el-option v-for="q in questionOptions" :key="q.id" :label="q.stem" :value="q.id" />
            </el-select>
            <el-input-number v-model="item.score" :min="1" />
            <el-button link type="danger" @click="manual.questions.splice(index, 1)">移除</el-button>
          </div>
          <el-button @click="manual.questions.push({ questionId: null, score: 5 })">加一题</el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="saveManual">保存为草稿</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="smartVisible" title="智能组卷" width="720px">
      <el-form label-width="80px">
        <el-form-item label="标题"><el-input v-model="smart.title" /></el-form-item>
        <el-form-item label="时长"><el-input-number v-model="smart.durationMin" :min="1" :max="300" /></el-form-item>
        <el-form-item label="及格"><el-input-number v-model="smart.passScore" :min="0" :max="smartTotal" /></el-form-item>
        <el-form-item label="总分"><span>{{ smartTotal }}</span></el-form-item>
        <el-form-item label="规则">
          <div v-for="(rule, index) in smart.rules" :key="index" class="line">
            <el-select v-model="rule.categoryId" placeholder="分类" style="width: 140px">
              <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
            <el-select v-model="rule.type" style="width: 110px">
              <el-option label="单选" value="SINGLE" />
              <el-option label="多选" value="MULTI" />
              <el-option label="判断" value="JUDGE" />
              <el-option label="简答" value="SHORT" />
            </el-select>
            <el-input-number v-model="rule.count" :min="1" />
            <el-input-number v-model="rule.scoreEach" :min="1" />
          </div>
          <el-button @click="smart.rules.push({ categoryId: categories[0]?.id ?? null, type: 'SINGLE', count: 2, scoreEach: 5 })">加规则</el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="saveSmart">抽题并保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../api'

const rows = ref([])
const total = ref(0)
const current = ref(1)
const keyword = ref('')
const categories = ref([])
const questionOptions = ref([])
const manualVisible = ref(false)
const smartVisible = ref(false)
const manual = reactive({ title: '', durationMin: 60, totalScore: 100, passScore: 60, description: '', questions: [] })
const smart = reactive({ title: '', durationMin: 60, passScore: 60, description: '', rules: [] })
const smartTotal = computed(() => smart.rules.reduce((sum, rule) => sum + Number(rule.count || 0) * Number(rule.scoreEach || 0), 0))

async function load() {
  const data = await http.get('/papers', { params: { current: current.value, size: 10, keyword: keyword.value } })
  rows.value = data.records
  total.value = data.total
}

async function openManual() {
  questionOptions.value = (await http.get('/questions', { params: { current: 1, size: 200 } })).records
  manual.questions = [{ questionId: null, score: 5 }]
  manualVisible.value = true
}

function openSmart() {
  const categoryId = categories.value[0]?.id ?? null
  smart.rules = [{ categoryId, type: 'SINGLE', count: 2, scoreEach: 5 }]
  smartVisible.value = true
}

async function saveManual() {
  await http.post('/papers/manual', manual)
  manualVisible.value = false
  ElMessage.success('已保存')
  load()
}

async function saveSmart() {
  if (smart.rules.some((rule) => !rule.categoryId)) {
    ElMessage.warning('每条规则都要选择分类')
    return
  }
  await http.post('/papers/smart', smart)
  smartVisible.value = false
  ElMessage.success('已按规则抽题')
  load()
}

async function change(row, status) {
  await http.put(`/papers/${row.id}/status`, null, { params: { status } })
  load()
}

async function remove(row) {
  await http.delete(`/papers/${row.id}`)
  load()
}

onMounted(async () => {
  categories.value = await http.get('/categories')
  await load()
})
</script>

<style scoped>
.line { display: flex; gap: 8px; margin-bottom: 8px; align-items: center; }
</style>
