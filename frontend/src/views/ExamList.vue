<template>
  <div class="page">
    <el-tabs v-model="tab">
      <el-tab-pane label="可参加" name="open">
        <el-table :data="papers">
          <el-table-column prop="title" label="试卷" />
          <el-table-column prop="durationMin" label="时长(分钟)" width="120" />
          <el-table-column prop="totalScore" label="总分" width="80" />
          <el-table-column prop="passScore" label="及格分" width="90" />
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button type="primary" @click="start(row)">开始考试</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="我的成绩" name="mine">
        <el-table :data="mine">
          <el-table-column prop="paperTitle" label="试卷" />
          <el-table-column prop="score" label="得分" width="80" />
          <el-table-column prop="status" label="状态" width="120" />
          <el-table-column prop="submitTime" label="交卷时间" width="180" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '../api'

const router = useRouter()
const tab = ref('open')
const papers = ref([])
const mine = ref([])

async function start(row) {
  const session = await http.post('/exams/start', { paperId: row.id })
  router.push(`/exams/${session.recordId}`)
}

onMounted(async () => {
  papers.value = (await http.get('/papers/available', { params: { current: 1, size: 50 } })).records
  mine.value = (await http.get('/stats/mine', { params: { current: 1, size: 50 } })).records
})
</script>
