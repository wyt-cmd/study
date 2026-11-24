<template>
  <div class="page">
    <div class="toolbar">
      <el-select v-model="categoryId" clearable placeholder="分类" style="width: 160px">
        <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-select v-model="type" clearable placeholder="题型" style="width: 140px">
        <el-option label="单选" value="SINGLE" />
        <el-option label="多选" value="MULTI" />
        <el-option label="判断" value="JUDGE" />
        <el-option label="简答" value="SHORT" />
      </el-select>
      <el-button type="primary" @click="next">下一题</el-button>
    </div>
    <el-card v-if="question">
      <div>{{ question.stem }}</div>
      <div class="answer">
        <el-radio-group v-if="question.type === 'SINGLE' || question.type === 'JUDGE'" v-model="userAnswer">
          <el-radio v-for="opt in options" :key="opt.key" :value="opt.key">{{ opt.key }}. {{ opt.text }}</el-radio>
        </el-radio-group>
        <el-checkbox-group v-else-if="question.type === 'MULTI'" v-model="multi">
          <el-checkbox v-for="opt in options" :key="opt.key" :value="opt.key">{{ opt.key }}. {{ opt.text }}</el-checkbox>
        </el-checkbox-group>
        <el-input v-else v-model="userAnswer" type="textarea" />
      </div>
      <el-button type="primary" @click="submit">提交</el-button>
      <el-alert v-if="result" style="margin-top: 12px" :type="result.correct ? 'success' : 'warning'" :closable="false"
        :title="`${result.correct ? '正确' : '未得满分'} ${result.score}/${result.fullScore}`"
        :description="`参考答案：${result.answer || ''} ${result.analysis || ''}`" />
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import http from '../api'

const categories = ref([])
const categoryId = ref(null)
const type = ref('')
const question = ref(null)
const userAnswer = ref('')
const multi = ref([])
const result = ref(null)

const options = computed(() => {
  if (!question.value) return []
  if (question.value.type === 'JUDGE') {
    return [{ key: 'T', text: '正确' }, { key: 'F', text: '错误' }]
  }
  if (!question.value.optionsJson) return []
  return JSON.parse(question.value.optionsJson)
})

async function next() {
  result.value = null
  userAnswer.value = ''
  multi.value = []
  question.value = await http.get('/practice/next', { params: { categoryId: categoryId.value, type: type.value } })
}

async function submit() {
  const answer = question.value.type === 'MULTI' ? multi.value.slice().sort().join(',') : userAnswer.value
  result.value = await http.post('/practice/submit', { questionId: question.value.id, userAnswer: answer })
}

onMounted(async () => {
  categories.value = await http.get('/categories')
  await next()
})
</script>

<style scoped>
.answer { margin: 16px 0; display: flex; flex-direction: column; gap: 8px; }
</style>
