<template>
  <div class="delivery-staff-view">
    <div class="page-header">
      <h2 class="page-title">配送员管理</h2>
      <el-button type="primary" @click="showAddDialog = true">
        <el-icon><Plus /></el-icon>添加配送员
      </el-button>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :span="8">
        <el-card class="stat-card">
          <p class="stat-label">总配送员</p>
          <p class="stat-value">{{ stats.total }}</p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card">
          <p class="stat-label">在职</p>
          <p class="stat-value">{{ stats.active }}</p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card">
          <p class="stat-label">今日配送</p>
          <p class="stat-value">{{ stats.todayDelivery }}</p>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选栏 -->
    <el-card class="filter-card">
      <el-form :model="filterForm" inline class="filter-form">
        <el-form-item label="姓名" class="filter-item">
          <el-input v-model="filterForm.nickname" placeholder="请输入姓名" clearable class="filter-input" />
        </el-form-item>
        <el-form-item label="手机号" class="filter-item">
          <el-input v-model="filterForm.phone" placeholder="请输入手机号" clearable class="filter-input" />
        </el-form-item>
        <el-form-item label="工作区域" class="filter-item">
          <el-input v-model="filterForm.location" placeholder="请输入工作区域" clearable class="filter-input" />
        </el-form-item>
        <el-form-item label="状态" class="filter-item">
          <el-select v-model="filterForm.status" placeholder="请选择状态" clearable class="filter-select">
            <el-option label="在职" :value="1" />
            <el-option label="离职" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item class="filter-item filter-buttons">
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 配送员列表 -->
    <el-card>
      <el-table :data="staffList" v-loading="loading">
        <el-table-column label="配送员信息" min-width="200">
          <template #default="{ row }">
            <div class="staff-info">
              <el-avatar :size="40" :src="getImageUrl(row.avatar)" />
              <div class="staff-detail">
                <p class="staff-name">{{ row.nickname }}</p>
                <p class="staff-phone">{{ row.phone }}</p>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="location" label="工作区域" width="150" align="center" />
        <el-table-column prop="todayOrders" label="今日订单" width="100" align="center" />
        <el-table-column prop="totalOrders" label="累计订单" width="100" align="center" />
        <el-table-column label="评分" width="120" align="center">
          <template #default="{ row }">
            <el-rate v-model="row.rating" disabled show-score text-color="#ff9900" />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="editStaff(row)">编辑</el-button>
            <el-button link type="primary" @click="viewStats(row)">统计</el-button>
            <el-button link type="danger" @click="deleteStaff(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total" layout="total, prev, pager, next" @current-change="handlePageChange" />
      </div>
    </el-card>

    <!-- 添加/编辑配送员弹窗 -->
    <el-dialog v-model="showAddDialog" :title="isEdit ? '编辑配送员' : '添加配送员'" width="500px" @close="resetForm">
      <el-form :model="staffForm" label-width="100px">
        <el-form-item label="姓名">
          <el-input v-model="staffForm.nickname" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="staffForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="工作区域">
          <el-input v-model="staffForm.location" placeholder="请输入工作区域" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="saveStaff">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getImageUrl } from '@/utils/image'
import {
  getDeliveryStaffList,
  getDeliveryStaffStats,
  addDeliveryStaff,
  updateDeliveryStaff,
  deleteDeliveryStaff as deleteDeliveryStaffApi,
  toggleDeliveryStaffStatus,
  type DeliveryStaff
} from '@/api/deliveryStaff'

const loading = ref(false)
const showAddDialog = ref(false)
const isEdit = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const stats = ref({
  total: 0,
  active: 0,
  todayDelivery: 0
})

const staffForm = reactive({
  id: null as number | null,
  nickname: '',
  phone: '',
  location: ''
})

const filterForm = reactive({
  nickname: '',
  phone: '',
  location: '',
  status: null as number | null
})

const staffList = ref<DeliveryStaff[]>([])

// 获取配送员列表
const fetchStaffList = async () => {
  loading.value = true
  try {
    const params: any = {
      page: page.value,
      size: pageSize.value
    }
    if (filterForm.nickname) params.nickname = filterForm.nickname
    if (filterForm.phone) params.phone = filterForm.phone
    if (filterForm.location) params.location = filterForm.location
    if (filterForm.status !== null) params.status = filterForm.status

    const res: any = await getDeliveryStaffList(params)
    if (res) {
      // 为每个配送员添加统计字段（暂时使用模拟数据，后续可从后端获取）
      staffList.value = (res.list || []).map((staff: DeliveryStaff, index: number) => ({
        ...staff,
        todayOrders: Math.floor(Math.random() * 20) + 1, // 模拟今日订单 1-20
        totalOrders: Math.floor(Math.random() * 500) + 100, // 模拟累计订单 100-600
        rating: Number((Math.random() * 1.5 + 3.5).toFixed(1)) // 模拟评分 3.5-5.0
      }))
      total.value = res.total || 0
    }
  } catch (error) {
    console.error('获取配送员列表失败:', error)
    ElMessage.error('获取配送员列表失败')
  } finally {
    loading.value = false
  }
}

// 获取统计数据
const fetchStats = async () => {
  try {
    const res: any = await getDeliveryStaffStats()
    if (res) {
      stats.value = res
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

const editStaff = (row: DeliveryStaff) => {
  isEdit.value = true
  staffForm.id = row.id
  staffForm.nickname = row.nickname
  staffForm.phone = row.phone
  staffForm.location = row.location || ''
  showAddDialog.value = true
}

const viewStats = (row: DeliveryStaff) => {
  ElMessage.info(`查看${row.nickname}的统计数据`)
}

const deleteStaff = (row: DeliveryStaff) => {
  ElMessageBox.confirm('确定要删除该配送员吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteDeliveryStaffApi(row.id)
      ElMessage.success('删除成功')
      fetchStaffList()
      fetchStats()
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  })
}

const saveStaff = async () => {
  try {
    if (isEdit.value && staffForm.id) {
      await updateDeliveryStaff(staffForm.id, {
        nickname: staffForm.nickname,
        phone: staffForm.phone,
        location: staffForm.location
      })
      ElMessage.success('修改成功')
    } else {
      await addDeliveryStaff({
        nickname: staffForm.nickname,
        phone: staffForm.phone,
        location: staffForm.location
      })
      ElMessage.success('添加成功')
    }
    showAddDialog.value = false
    fetchStaffList()
    fetchStats()
    resetForm()
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  }
}

const resetForm = () => {
  staffForm.id = null
  staffForm.nickname = ''
  staffForm.phone = ''
  staffForm.location = ''
  isEdit.value = false
}

const handlePageChange = (val: number) => {
  page.value = val
  fetchStaffList()
}

// 查询
const handleSearch = () => {
  page.value = 1
  fetchStaffList()
}

// 重置筛选
const resetFilter = () => {
  filterForm.nickname = ''
  filterForm.phone = ''
  filterForm.location = ''
  filterForm.status = null
  page.value = 1
  fetchStaffList()
}

// 切换状态
const handleStatusChange = async (row: DeliveryStaff) => {
  try {
    await toggleDeliveryStaffStatus(row.id, row.status)
    ElMessage.success('状态更新成功')
    fetchStats()
  } catch (error) {
    console.error('状态更新失败:', error)
    ElMessage.error('状态更新失败')
    // 恢复原状态
    row.status = row.status === 1 ? 0 : 1
  }
}

onMounted(() => {
  fetchStaffList()
  fetchStats()
})
</script>

<style scoped>
.delivery-staff-view {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.staff-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.staff-name {
  font-weight: 500;
  color: #303133;
}

.staff-phone {
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
