<template>
  <div class="logistics-view">
    <div class="page-header">
      <h2 class="page-title">物流调度</h2>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <p class="stat-label">待揽件</p>
          <p class="stat-value">{{ stats.pendingPickup }}</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <p class="stat-label">运输中</p>
          <p class="stat-value">{{ stats.inTransit }}</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <p class="stat-label">派送中</p>
          <p class="stat-value">{{ stats.delivering }}</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <p class="stat-label">今日签收</p>
          <p class="stat-value">{{ stats.signed }}</p>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选栏 -->
    <el-card class="filter-card">
      <el-form :model="filterForm" inline class="filter-form">
        <el-form-item label="运单号" class="filter-item">
          <el-input v-model="filterForm.waybillNo" placeholder="请输入运单号" clearable class="filter-input" />
        </el-form-item>
        <el-form-item label="收件人" class="filter-item">
          <el-input v-model="filterForm.receiverName" placeholder="请输入收件人姓名" clearable class="filter-input" />
        </el-form-item>
        <el-form-item label="配送员" class="filter-item">
          <el-select v-model="filterForm.deliveryId" placeholder="请选择配送员" clearable class="filter-select">
            <el-option v-for="staff in deliveryStaffList" :key="staff.id" :label="staff.name" :value="staff.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="物流状态" class="filter-item">
          <el-select v-model="filterForm.status" placeholder="请选择状态" clearable class="filter-select-sm">
            <el-option label="待揽件" :value="0" />
            <el-option label="运输中" :value="1" />
            <el-option label="派送中" :value="2" />
            <el-option label="已签收" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item class="filter-item filter-buttons">
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 运单列表 -->
    <el-card>
      <el-table :data="waybillList" v-loading="loading">
        <el-table-column prop="waybillNo" label="运单号" width="160" align="center" />
        <el-table-column label="收件信息" min-width="200">
          <template #default="{ row }">
            <div class="receiver-info">
              <p>{{ row.receiverName }} {{ row.receiverPhone }}</p>
              <p class="address">{{ row.receiverAddress }}</p>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="商品信息" min-width="150">
          <template #default="{ row }">
            <p>{{ row.goodsName }}</p>
            <p class="weight">{{ row.weight }}kg</p>
          </template>
        </el-table-column>
        <el-table-column label="配送员" width="120" align="center">
          <template #default="{ row }">
            <span>{{ row.deliveryStaff || '未分配' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-if="!row.deliveryStaff" type="primary" size="small" @click="assignStaff(row)">分配配送员</el-button>
            <el-button link type="primary" @click="viewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total" layout="total, prev, pager, next" @current-change="handlePageChange" />
      </div>
    </el-card>

    <!-- 分配配送员弹窗 -->
    <el-dialog v-model="showAssignDialog" title="分配配送员" width="400px">
      <el-form :model="assignForm" label-width="100px">
        <el-form-item label="选择配送员">
          <el-select v-model="assignForm.staffId" placeholder="请选择配送员" style="width: 100%">
            <el-option v-for="staff in deliveryStaffList" :key="staff.id" :label="staff.name" :value="staff.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAssignDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmAssign">确定</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="showDetailDialog" title="物流详情" width="600px">
      <div v-if="currentDetail" class="detail-content">
        <h4>基本信息</h4>
        <p><strong>运单号：</strong>{{ currentDetail.waybillNo }}</p>
        <p><strong>状态：</strong><el-tag :type="getStatusType(currentDetail.status)">{{ getStatusText(currentDetail.status) }}</el-tag></p>
        <p v-if="currentDetail.pickupTime"><strong>揽件时间：</strong>{{ currentDetail.pickupTime }}</p>
        <p v-if="currentDetail.deliveryTime"><strong>派送时间：</strong>{{ currentDetail.deliveryTime }}</p>
        <p v-if="currentDetail.signTime"><strong>签收时间：</strong>{{ currentDetail.signTime }}</p>
        <p v-if="currentDetail.currentLocation"><strong>当前位置：</strong>{{ currentDetail.currentLocation }}</p>
        
        <h4>收件信息</h4>
        <p><strong>收件人：</strong>{{ currentDetail.receiverName }}</p>
        <p><strong>电话：</strong>{{ currentDetail.receiverPhone }}</p>
        <p><strong>地址：</strong>{{ currentDetail.receiverAddress }}</p>
        
        <h4>配送信息</h4>
        <p><strong>配送员：</strong>{{ currentDetail.deliveryStaff || '未分配' }}</p>
        <p v-if="currentDetail.deliveryPhone"><strong>配送员电话：</strong>{{ currentDetail.deliveryPhone }}</p>
        <p v-if="currentDetail.orderAmount"><strong>订单金额：</strong>¥{{ currentDetail.orderAmount }}</p>
        
        <h4>物流轨迹</h4>
        <el-timeline v-if="logisticsTracks.length > 0">
          <el-timeline-item
            v-for="track in logisticsTracks"
            :key="track.id"
            :type="getTrackType(track.status)"
            :timestamp="track.createTime"
          >
            <p>{{ track.description }}</p>
            <p v-if="track.location" class="track-location">{{ track.location }}</p>
          </el-timeline-item>
        </el-timeline>
        <p v-else>暂无物流轨迹</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const showAssignDialog = ref(false)
const showDetailDialog = ref(false)
const currentWaybill = ref<any>(null)
const currentDetail = ref<any>(null)
const logisticsTracks = ref<any[]>([])
const assignForm = reactive({
  staffId: null as number | null
})

const filterForm = reactive({
  waybillNo: '',
  receiverName: '',
  deliveryId: null as number | null,
  status: null as number | null
})

const stats = ref({
  pendingPickup: 0,
  inTransit: 0,
  delivering: 0,
  signed: 0
})

const waybillList = ref<any[]>([])
const deliveryStaffList = ref<any[]>([])

// 获取物流统计
const fetchStats = async () => {
  try {
    const data = await request.get('/admin/logistics/stats') as Record<string, number>
    stats.value = {
      pendingPickup: data.pendingPickup || 0,
      inTransit: data.inTransit || 0,
      delivering: data.delivering || 0,
      signed: data.signed || 0
    }
  } catch (error) {
    console.error('获取物流统计失败:', error)
  }
}

// 获取运单列表
const fetchWaybillList = async () => {
  loading.value = true
  try {
    const params: any = { page: page.value, size: pageSize.value }
    if (filterForm.waybillNo) params.waybillNo = filterForm.waybillNo
    if (filterForm.receiverName) params.receiverName = filterForm.receiverName
    if (filterForm.deliveryId !== null) params.deliveryId = filterForm.deliveryId
    if (filterForm.status !== null) params.status = filterForm.status

    const data = await request.get('/admin/logistics/waybills', { params }) as { list: any[], total: number }
    waybillList.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    console.error('获取运单列表失败:', error)
    ElMessage.error('获取运单列表失败')
  } finally {
    loading.value = false
  }
}

// 查询
const handleSearch = () => {
  page.value = 1
  fetchWaybillList()
}

// 重置筛选
const resetFilter = () => {
  filterForm.waybillNo = ''
  filterForm.receiverName = ''
  filterForm.deliveryId = null
  filterForm.status = null
  page.value = 1
  fetchWaybillList()
}

// 获取配送员列表
const fetchDeliveryStaffList = async () => {
  try {
    const data = await request.get('/admin/delivery-staff', {
      params: { page: 1, size: 100 }
    }) as { list: any[] }
    deliveryStaffList.value = (data.list || []).map((staff: any) => ({
      id: staff.id,
      name: staff.nickname || staff.username
    }))
  } catch (error) {
    console.error('获取配送员列表失败:', error)
  }
}

const getStatusType = (status: number) => {
  const map: Record<number, string> = {
    0: 'warning',
    1: 'primary',
    2: 'primary',
    3: 'success'
  }
  return map[status] || 'info'
}

const getStatusText = (status: number) => {
  const map: Record<number, string> = {
    0: '待揽件',
    1: '运输中',
    2: '派送中',
    3: '已签收'
  }
  return map[status] || '未知'
}

const getTrackType = (status: number) => {
  const map: Record<number, string> = {
    0: 'warning',
    1: 'primary',
    2: 'primary',
    3: 'success'
  }
  return map[status] || 'info'
}

const assignStaff = (row: any) => {
  currentWaybill.value = row
  showAssignDialog.value = true
}

const confirmAssign = async () => {
  if (!assignForm.staffId) {
    ElMessage.warning('请选择配送员')
    return
  }
  try {
    await request.post(`/admin/logistics/${currentWaybill.value.id}/assign`, {
      staffId: assignForm.staffId
    })
    ElMessage.success('分配成功')
    fetchWaybillList()
    showAssignDialog.value = false
    assignForm.staffId = null
  } catch (error) {
    console.error('分配失败:', error)
    ElMessage.error('分配失败')
  }
}

// 获取物流详情
const fetchLogisticsDetail = async (id: number) => {
  try {
    const data = await request.get(`/admin/logistics/${id}/detail`) as Record<string, any>
    currentDetail.value = data
  } catch (error) {
    console.error('获取物流详情失败:', error)
    ElMessage.error('获取物流详情失败')
  }
}

// 获取物流轨迹
const fetchLogisticsTracks = async (id: number) => {
  try {
    const data = await request.get(`/admin/logistics/${id}/tracks`) as any[]
    logisticsTracks.value = data || []
  } catch (error) {
    console.error('获取物流轨迹失败:', error)
  }
}

const viewDetail = async (row: any) => {
  await fetchLogisticsDetail(row.id)
  await fetchLogisticsTracks(row.id)
  showDetailDialog.value = true
}

const handlePageChange = (val: number) => {
  page.value = val
  fetchWaybillList()
}

onMounted(() => {
  fetchStats()
  fetchWaybillList()
  fetchDeliveryStaffList()
})
</script>

<style scoped>
.logistics-view {
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
  width: 140px;
}

.filter-select-sm {
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

.receiver-info .address {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.weight {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.detail-content h4 {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 20px 0 12px 0;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
}

.detail-content h4:first-child {
  margin-top: 0;
}

.detail-content p {
  margin: 8px 0;
  color: #606266;
  line-height: 1.6;
}

.detail-content strong {
  color: #303133;
  margin-right: 8px;
}

.track-location {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
