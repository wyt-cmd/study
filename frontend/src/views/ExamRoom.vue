<template>
  <div class="page" v-if="session">
    <el-card>
      <div class="head">
        <div>
          <h2>{{ session.paperTitle }}</h2>
          <span>总分 {{ session.totalScore }}，及格 {{ session.passScore }}</span>
        </div>
        <div v-if="session.status === 'ONGOING'" class="timer">剩余 {{ remainText }}</div>
        <div v-else class="score">得分 {{ session.score }}</div>
      </div>
      <div v-for="(item, index) in session.questions" :key="item.questionId" class="question">
        <div class="stem">{{ index + 1 }}. [{{ typeName(item.type) }} / {{ item.score }}分] {{ item.stem }}</div>
        <el-radio-group v-if="item.type === 'SINGLE' || item.type === 'JUDGE'" v-model="item.userAnswer" :disabled="done">
          <el-radio v-for="opt in options(item)" :key="opt.key" :value="opt.key">{{ opt.key }}. {{ opt.text }}</el-radio>
        </el-radio-group>
        <el-checkbox-group v-else-if="item.type === 'MULTI'" v-model="multi[item.questionId]" :disabled="done">
          <el-checkbox v-for="opt in options(item)" :key="opt.key" :value="opt.key">{{ opt.key }}. {{ opt.text }}</el-checkbox>
        </el-checkbox-group>
        <el-input v-else v-model="item.userAnswer" type="textarea" :disabled="done" />
        <div v-if="session.showAnswer" class="review">
          得分 {{ item.gotScore }} / {{ item.score }}，参考答案 {{ item.standardAnswer }}
          <div>{{ item.comment }} {{ item.analysis }}</div>
        </div>
      </div>
      <div v-if="!done" class="toolbar">
        <el-button @click="save">保存答案</el-button>
        <el-button type="primary" @click="submit">交卷</el-button>
      </div>
      <p v-if="session.aiComment">{{ session.aiComment }}</p>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api'

const route = useRoute()
const session = ref(null)
const multi = reactive({})
const now = ref(Date.now())
let timer
let autosave

const done = computed(() => session.value && session.value.status !== 'ONGOING')
const remainText = computed(() => {
  if (!session.value) return ''
  const left = Math.max(0, new Date(session.value.deadline).getTime() - now.value)
  const min = Math.floor(left / 60000)
  const sec = Math.floor((left % 60000) / 1000)
  return `${min}:${String(sec).padStart(2, '0')}`
})

function typeName(type) {
  return { SINGLE: '单选', MULTI: '多选', JUDGE: '判断', SHORT: '简答' }[type] || type
}

function options(item) {
  if (item.type === 'JUDGE') {
    return [{ key: 'T', text: '正确' }, { key: 'F', text: '错误' }]
  }
  if (!item.optionsJson) return []
  const parsed = JSON.parse(item.optionsJson)
  return Array.isArray(parsed) ? parsed : []
}

function payload() {
  return {
    answers: session.value.questions.map((item) => ({
      questionId: item.questionId,
      userAnswer: item.type === 'MULTI' ? (multi[item.questionId] || []).slice().sort().join(',') : item.userAnswer
    }))
  }
}

async function load() {
  session.value = await http.get(`/exams/${route.params.id}`)
  session.value.questions.forEach((item) => {
    if (item.type === 'MULTI') {
      multi[item.questionId] = item.userAnswer ? item.userAnswer.split(',') : []
    }
  })
}

async function save() {
  await http.put(`/exams/${session.value.recordId}/answers`, payload())
  ElMessage.success('答案已保存')
}

async function submit() {
  await ElMessageBox.confirm('交卷后不能再修改', '确认交卷')
  await http.put(`/exams/${session.value.recordId}/answers`, payload())
  session.value = await http.post(`/exams/${session.value.recordId}/submit`)
}

onMounted(async () => {
  await load()
  timer = setInterval(() => {
    now.value = Date.now()
    if (session.value?.status === 'ONGOING' && new Date(session.value.deadline).getTime() <= now.value) {
      load()
    }
  }, 1000)
  autosave = setInterval(() => {
    if (session.value?.status === 'ONGOING') {
      http.put(`/exams/${session.value.recordId}/answers`, payload()).catch(() => {})
    }
  }, 20000)
})

onBeforeUnmount(() => {
  clearInterval(timer)
  clearInterval(autosave)
})
</script>

<style scoped>
.head { display: flex; justify-content: space-between; align-items: center; }
.timer, .score { font-size: 22px; font-weight: 700; }
.question { padding: 12px 0; border-top: 1px solid #e5e7eb; }
.stem { margin-bottom: 8px; }
.review { margin-top: 8px; color: #067647; }
</style>
