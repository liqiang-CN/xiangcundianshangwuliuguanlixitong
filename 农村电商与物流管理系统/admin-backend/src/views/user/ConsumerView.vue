<template>
  <div class="consumer-view">
    <div class="page-header">
      <h2 class="page-title">用户管理</h2>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <p class="stat-label">总用户</p>
          <p class="stat-value">{{ stats.total }}</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <p class="stat-label">今日新增</p>
          <p class="stat-value">{{ stats.todayNew }}</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <p class="stat-label">活跃用户</p>
          <p class="stat-value">{{ stats.active }}</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <p class="stat-label">总订单数</p>
          <p class="stat-value">{{ stats.totalOrders }}</p>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选栏 -->
    <el-card class="filter-card">
      <el-form :model="filterForm" inline class="filter-form">
        <el-form-item label="用户昵称" class="filter-item">
          <el-input v-model="filterForm.nickname" placeholder="请输入昵称" clearable class="filter-input" />
        </el-form-item>
        <el-form-item label="手机号" class="filter-item">
          <el-input v-model="filterForm.phone" placeholder="请输入手机号" clearable class="filter-input" />
        </el-form-item>
        <el-form-item class="filter-item filter-buttons">
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 用户列表 -->
    <el-card>
      <el-table :data="consumerList" v-loading="loading">
        <el-table-column label="用户信息" min-width="220">
          <template #default="{ row }">
            <div class="user-info">
              <el-avatar :size="50" :src="getImageUrl(row.avatar)" />
              <div class="user-detail">
                <p class="user-name">{{ row.nickname || '-' }}</p>
                <p class="user-phone">{{ row.phone || '未绑定手机号' }}</p>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" min-width="120" align="center" />
        <el-table-column prop="orderCount" label="订单数" width="100" align="center">
          <template #default>
            <span>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="消费金额" width="120" align="center">
          <template #default>
            <span class="amount">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch 
              v-model="row.status" 
              :active-value="1" 
              :inactive-value="0"
              @change="(val: number) => toggleStatus(row, val)"
            />
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
              v-if="row.status === 1" 
              link 
              type="danger" 
              @click="handleDisableUserFromTable(row)"
            >
              禁用
            </el-button>
            <el-button 
              v-else 
              link 
              type="success" 
              @click="handleEnableUserFromTable(row)"
            >
              启用
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

    <!-- 用户详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="用户详情" width="500px">
      <el-descriptions :column="1" border v-if="currentUser">
        <el-descriptions-item label="用户ID">{{ currentUser.id }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ currentUser.username }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ currentUser.nickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ currentUser.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ currentUser.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentUser.status === 1 ? 'success' : 'danger'">
            {{ currentUser.status === 1 ? '正常' : '禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ formatDate(currentUser.createTime) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleClose, CircleCheck } from '@element-plus/icons-vue'
import request from '../../api/request'
import { getImageUrl } from '@/utils/image'

// 定义一个全局事件总线用于跨页面通信
const USER_UPDATE_EVENT = 'user-data-updated'

interface User {
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
  todayNew: number
  active: number
  totalOrders: number
}

const loading = ref(false)
const filterForm = reactive({
  nickname: '',
  phone: ''
})
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const stats = ref<Stats>({
  total: 0,
  todayNew: 0,
  active: 0,
  totalOrders: 0
})

const consumerList = ref<User[]>([])
const detailDialogVisible = ref(false)
const currentUser = ref<User | null>(null)
const disableLoading = ref(false)

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

// 获取用户列表
const fetchUserList = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/admin/users', {
      params: {
        page: page.value,
        size: pageSize.value,
        nickname: filterForm.nickname || undefined,
        phone: filterForm.phone || undefined
      }
    })
    consumerList.value = res.list || []
    total.value = res.total || 0
  } catch (error) {
    console.error('获取用户列表失败:', error)
    ElMessage.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

// 获取统计信息
const fetchStats = async () => {
  try {
    const res: any = await request.get('/admin/users/stats')
    stats.value = res as Stats
  } catch (error) {
    console.error('获取统计失败:', error)
  }
}

// 切换用户状态
const toggleStatus = async (row: User, val: number) => {
  try {
    await request.put(`/admin/users/${row.id}/status`, { status: val })
    ElMessage.success(val === 1 ? '已启用' : '已禁用')
  } catch (error) {
    console.error('切换状态失败:', error)
    ElMessage.error('操作失败')
    // 恢复原状态
    row.status = val === 1 ? 0 : 1
  }
}

// 查看详情
const viewDetail = async (row: User) => {
  try {
    const res: any = await request.get(`/admin/users/${row.id}`)
    currentUser.value = res as User
    detailDialogVisible.value = true
  } catch (error) {
    console.error('获取用户详情失败:', error)
    ElMessage.error('获取用户详情失败')
  }
}

// 禁用用户
const handleDisableUser = async () => {
  if (!currentUser.value) return
  
  try {
    await ElMessageBox.confirm(
      `确定要禁用用户 "${currentUser.value.nickname || currentUser.value.username}" 吗？禁用后该用户将无法登录。`,
      '确认禁用',
      {
        confirmButtonText: '确定禁用',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    disableLoading.value = true
    await request.put(`/admin/users/${currentUser.value.id}/status`, { status: 0 })
    ElMessage.success('用户已禁用')
    
    // 更新当前用户状态
    currentUser.value.status = 0
    
    // 刷新列表
    fetchUserList()
    fetchStats()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('禁用用户失败:', error)
      ElMessage.error('禁用用户失败')
    }
  } finally {
    disableLoading.value = false
  }
}

// 启用用户
const handleEnableUser = async () => {
  if (!currentUser.value) return
  
  try {
    await ElMessageBox.confirm(
      `确定要启用用户 "${currentUser.value.nickname || currentUser.value.username}" 吗？`,
      '确认启用',
      {
        confirmButtonText: '确定启用',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    
    disableLoading.value = true
    await request.put(`/admin/users/${currentUser.value.id}/status`, { status: 1 })
    ElMessage.success('用户已启用')
    
    // 更新当前用户状态
    currentUser.value.status = 1
    
    // 刷新列表
    fetchUserList()
    fetchStats()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('启用用户失败:', error)
      ElMessage.error('启用用户失败')
    }
  } finally {
    disableLoading.value = false
  }
}

// 从表格禁用用户
const handleDisableUserFromTable = async (row: User) => {
  try {
    await ElMessageBox.confirm(
      `确定要禁用用户 "${row.nickname || row.username}" 吗？禁用后该用户将无法登录。`,
      '确认禁用',
      {
        confirmButtonText: '确定禁用',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await request.put(`/admin/users/${row.id}/status`, { status: 0 })
    ElMessage.success('用户已禁用')
    
    // 刷新列表
    fetchUserList()
    fetchStats()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('禁用用户失败:', error)
      ElMessage.error('禁用用户失败')
    }
  }
}

// 从表格启用用户
const handleEnableUserFromTable = async (row: User) => {
  try {
    await ElMessageBox.confirm(
      `确定要启用用户 "${row.nickname || row.username}" 吗？`,
      '确认启用',
      {
        confirmButtonText: '确定启用',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    
    await request.put(`/admin/users/${row.id}/status`, { status: 1 })
    ElMessage.success('用户已启用')
    
    // 刷新列表
    fetchUserList()
    fetchStats()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('启用用户失败:', error)
      ElMessage.error('启用用户失败')
    }
  }
}

const handleSearch = () => {
  page.value = 1
  fetchUserList()
}

const resetFilter = () => {
  filterForm.nickname = ''
  filterForm.phone = ''
  page.value = 1
  fetchUserList()
}

const handlePageChange = (val: number) => {
  page.value = val
  fetchUserList()
}

// 监听页面可见性变化，当页面重新可见时刷新数据
const handleVisibilityChange = () => {
  if (!document.hidden) {
    fetchUserList()
    fetchStats()
  }
}

// 监听storage事件，当其他页面更新数据时刷新
const handleStorageChange = (e: StorageEvent) => {
  if (e.key === USER_UPDATE_EVENT) {
    fetchUserList()
    fetchStats()
    ElMessage.info('用户数据已更新')
  }
}

onMounted(() => {
  fetchStats()
  fetchUserList()
  
  // 监听页面可见性变化
  document.addEventListener('visibilitychange', handleVisibilityChange)
  // 监听storage事件
  window.addEventListener('storage', handleStorageChange)
})

onUnmounted(() => {
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  window.removeEventListener('storage', handleStorageChange)
})
</script>

<style scoped>
.consumer-view {
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

.filter-buttons {
  margin-left: auto;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-name {
  font-weight: 500;
  color: #303133;
}

.user-phone {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.amount {
  color: #f56c6c;
  font-weight: 600;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.dialog-footer {
  display: flex;
  justify-content: center;
  padding: 10px 0;
}

.dialog-footer .el-button {
  min-width: 120px;
  height: 40px;
  font-size: 14px;
}

.dialog-footer .el-icon {
  margin-right: 6px;
}
</style>
