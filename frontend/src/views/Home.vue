<template>
  <div class="page">
    <el-carousel v-if="banners.length" height="180px" class="banner">
      <el-carousel-item v-for="item in banners" :key="item.id">
        <a class="slide" :href="item.linkUrl || undefined">
          <img :src="fileUrl(item.imageUrl)" :alt="item.title" />
          <span>{{ item.title }}</span>
        </a>
      </el-carousel-item>
    </el-carousel>
    <el-row :gutter="12">
      <el-col :span="8">
        <el-card>
          <div class="label">练习正确率</div>
          <div class="num">{{ summary.accuracy || 0 }}%</div>
          <div>已练 {{ summary.total || 0 }} 题，答对 {{ summary.hit || 0 }} 题</div>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card>
          <template #header>热门题目</template>
          <el-table :data="hot" size="small">
            <el-table-column prop="stem" label="题干" show-overflow-tooltip />
            <el-table-column prop="type" label="题型" width="100" />
            <el-table-column prop="hot" label="练习次数" width="100" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import http from '../api'

const summary = ref({})
const hot = ref([])
const banners = ref([])

function fileUrl(name) {
  if (!name) return ''
  if (name.startsWith('http')) return name
  return `/api/files/${name}`
}

onMounted(async () => {
  banners.value = await http.get('/banners/active')
  summary.value = await http.get('/practice/summary')
  hot.value = await http.get('/questions/hot')
})
</script>

<style scoped>
.label { color: #667085; }
.num { font-size: 36px; font-weight: 700; margin: 8px 0; }
.banner { margin-bottom: 12px; border-radius: 8px; overflow: hidden; background: #111827; }
.slide { display: block; height: 180px; position: relative; color: #fff; }
.slide img { width: 100%; height: 180px; object-fit: cover; }
.slide span { position: absolute; left: 16px; bottom: 12px; font-weight: 700; text-shadow: 0 1px 4px #000; }
</style>
