<template>
  <div class="farmer-view">
    <div class="page-header">
      <h2 class="page-title">农户管理</h2>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <p class="stat-label">总农户</p>
          <p class="stat-value">{{ stats.total }}</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <p class="stat-label">已启用</p>
          <p class="stat-value">{{ stats.verified }}</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <p class="stat-label">已禁用</p>
          <p class="stat-value">{{ stats.pending }}</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <p class="stat-label">本月新增</p>
          <p class="stat-value">{{ stats.newThisMonth }}</p>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选栏 -->
    <el-card class="filter-card">
      <el-form :model="filterForm" inline class="filter-form">
        <el-form-item label="农户名称" class="filter-item">
          <el-input v-model="filterForm.name" placeholder="请输入农户名称" clearable class="filter-input" />
        </el-form-item>
        <el-form-item label="状态" class="filter-item">
          <el-select v-model="filterForm.status" placeholder="请选择状态" clearable class="filter-select">
            <el-option label="已启用" :value="1" />
            <el-option label="已禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item class="filter-item filter-buttons">
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 农户列表 -->
    <el-card>
      <el-table :data="farmerList" v-loading="loading">
        <el-table-column label="农户信息" min-width="250">
          <template #default="{ row }">
            <div class="farmer-info">
              <el-avatar :size="50" :src="getImageUrl(row.avatar)" />
              <div class="farmer-detail">
                <p class="farmer-name">{{ row.nickname || row.username }}</p>
                <p class="farmer-phone">{{ row.phone || '未绑定手机号' }}</p>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" min-width="150" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '已启用' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="160" align="center">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewDetail(row)">详情</el-button>
            <el-button 
              link 
              :type="row.status === 1 ? 'danger' : 'success'" 
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination 
          v-model:current-page="page" 
          v-model:page-size="pageSize" 
          :total="total" 
          layout="total, prev, pager, next" 
          @current-change="handlePageChange" 
        />
      </div>
    </el-card>

    <!-- 农户详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="农户详情" width="500px">
      <el-descriptions :column="1" border v-if="currentFarmer">
        <el-descriptions-item label="农户ID">{{ currentFarmer.id }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ currentFarmer.username }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ currentFarmer.nickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ currentFarmer.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ currentFarmer.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentFarmer.status === 1 ? 'success' : 'danger'">
            {{ currentFarmer.status === 1 ? '已启用' : '已禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ formatDate(currentFarmer.createTime) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../../api/request'
import { getImageUrl } from '@/utils/image'

interface Farmer {
  id: number
  username: string
  nickname: string
  avatar: string
  phone: string
  email: string
  status: number
  createTime: string
  updateTime: string
}

interface Stats {
  total: number
  verified: number
  pending: number
  newThisMonth: number
}

const loading = ref(false)
const filterForm = reactive({
  name: '',
  status: null as number | null
})
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const stats = ref<Stats>({
  total: 0,
  verified: 0,
  pending: 0,
  newThisMonth: 0
})

const farmerList = ref<Farmer[]>([])
const detailDialogVisible = ref(false)
const currentFarmer = ref<Farmer | null>(null)

// 格式化日期
const formatDate = (date: string) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 获取农户列表
const fetchFarmerList = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/admin/farmers', {
      params: {
        page: page.value,
        size: pageSize.value,
        name: filterForm.name || undefined,
        status: filterForm.status
      }
    })
    farmerList.value = res.list || []
    total.value = res.total || 0
  } catch (error) {
    console.error('获取农户列表失败:', error)
    ElMessage.error('获取农户列表失败')
  } finally {
    loading.value = false
  }
}

// 获取统计信息
const fetchStats = async () => {
  try {
    const res: any = await request.get('/admin/farmers/stats')
    stats.value = res as Stats
  } catch (error) {
    console.error('获取统计失败:', error)
  }
}

// 切换农户状态
const toggleStatus = async (row: Farmer) => {
  const newStatus = row.status === 1 ? 0 : 1
  try {
    await request.put(`/admin/farmers/${row.id}/status`, { status: newStatus })
    ElMessage.success(newStatus === 1 ? '已启用' : '已禁用')
    row.status = newStatus
    fetchStats()
  } catch (error) {
    console.error('切换状态失败:', error)
    ElMessage.error('操作失败')
  }
}

// 查看详情
const viewDetail = async (row: Farmer) => {
  try {
    const res: any = await request.get(`/admin/farmers/${row.id}`)
    currentFarmer.value = res as Farmer
    detailDialogVisible.value = true
  } catch (error) {
    console.error('获取农户详情失败:', error)
    ElMessage.error('获取农户详情失败')
  }
}

const handleSearch = () => {
  page.value = 1
  fetchFarmerList()
}

const resetFilter = () => {
  filterForm.name = ''
  filterForm.status = null
  page.value = 1
  fetchFarmerList()
}

const handlePageChange = (val: number) => {
  page.value = val
  fetchFarmerList()
}

onMounted(() => {
  fetchStats()
  fetchFarmerList()
})
</script>

<style scoped>
.farmer-view {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.stat-row {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}

.filter-card {
  margin-bottom: 20px;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-item {
  margin-bottom: 0;
  margin-right: 0;
}

.filter-input {
  width: 160px;
}

.filter-select {
  width: 120px;
}

.filter-buttons {
  margin-left: auto;
}

.farmer-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.farmer-name {
  font-weight: 500;
  color: #303133;
}

.farmer-phone {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
