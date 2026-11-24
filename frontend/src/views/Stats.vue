<template>
  <div class="page">
    <template v-if="staff">
      <div class="toolbar">
        <el-button type="primary" @click="load">刷新</el-button>
        <el-button @click="download">导出成绩</el-button>
      </div>
      <el-table :data="aggregate">
        <el-table-column prop="title" label="试卷" />
        <el-table-column prop="attendCount" label="参考人数" width="100" />
        <el-table-column prop="avgScore" label="平均分" width="90" />
        <el-table-column prop="maxScore" label="最高分" width="90" />
        <el-table-column prop="minScore" label="最低分" width="90" />
        <el-table-column prop="passRate" label="及格率%" width="100" />
        <el-table-column label="排名" width="100">
          <template #default="{ row }">
            <el-button link @click="openRank(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-table :data="records" style="margin-top: 16px">
        <el-table-column prop="paperTitle" label="试卷" />
        <el-table-column prop="nickname" label="考生" width="120" />
        <el-table-column prop="score" label="得分" width="80" />
        <el-table-column prop="objectiveScore" label="客观" width="80" />
        <el-table-column prop="subjectiveScore" label="主观" width="80" />
        <el-table-column prop="status" label="状态" width="110" />
      </el-table>
      <el-pagination style="margin-top: 12px" layout="total, prev, pager, next" :total="total" v-model:current-page="current" @current-change="loadRecords" />
      <el-dialog v-model="rankVisible" title="排名" width="480px">
        <el-table :data="rank">
          <el-table-column prop="rankNo" label="名次" width="80" />
          <el-table-column prop="nickname" label="考生" />
          <el-table-column prop="score" label="得分" width="80" />
        </el-table>
      </el-dialog>
    </template>
    <template v-else>
      <el-table :data="records">
        <el-table-column prop="paperTitle" label="试卷" />
        <el-table-column prop="score" label="得分" width="80" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column prop="submitTime" label="交卷时间" />
      </el-table>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import http from '../api'

const staff = computed(() => ['ADMIN', 'TEACHER'].includes(JSON.parse(localStorage.getItem('profile') || '{}').role))
const aggregate = ref([])
const records = ref([])
const rank = ref([])
const rankVisible = ref(false)
const current = ref(1)
const total = ref(0)

async function loadRecords() {
  const path = staff.value ? '/stats/records' : '/stats/mine'
  const data = await http.get(path, { params: { current: current.value, size: 10 } })
  records.value = data.records
  total.value = data.total
}

async function load() {
  if (staff.value) {
    aggregate.value = await http.get('/stats/aggregate')
  }
  await loadRecords()
}

async function openRank(row) {
  rank.value = await http.get('/stats/rank', { params: { paperId: row.paperId } })
  rankVisible.value = true
}

async function download() {
  const res = await http.get('/stats/export', { responseType: 'blob' })
  const url = URL.createObjectURL(res.data)
  const a = document.createElement('a')
  a.href = url
  a.download = 'scores.xlsx'
  a.click()
  URL.revokeObjectURL(url)
}

onMounted(load)
</script>
