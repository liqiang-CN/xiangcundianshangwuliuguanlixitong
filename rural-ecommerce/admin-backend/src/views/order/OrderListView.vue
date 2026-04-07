<template>
  <div class="order-list-view">
    <div class="page-header">
      <h2 class="page-title">订单管理</h2>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <p class="stat-label">今日订单</p>
          <p class="stat-value">{{ stats.today }}</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <p class="stat-label">待发货</p>
          <p class="stat-value">{{ stats.pending }}</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <p class="stat-label">待收货</p>
          <p class="stat-value">{{ stats.shipped }}</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <p class="stat-label">已完成</p>
          <p class="stat-value">{{ stats.completed }}</p>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选栏 -->
    <el-card class="filter-card">
      <el-form :model="filterForm" inline class="filter-form">
        <el-form-item label="订单号" class="filter-item">
          <el-input v-model="filterForm.orderNo" placeholder="请输入订单号" clearable class="filter-input" />
        </el-form-item>
        <el-form-item label="订单状态" class="filter-item">
          <el-select v-model="filterForm.status" placeholder="请选择状态" clearable class="filter-select">
            <el-option label="待发货" :value="1" />
            <el-option label="待收货" :value="2" />
            <el-option label="已完成" :value="3" />
            <el-option label="已取消" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="下单时间" class="filter-item">
          <el-date-picker v-model="filterForm.dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" class="filter-date-range" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item class="filter-item filter-buttons">
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 订单列表 -->
    <el-card>
      <el-table :data="orderList" v-loading="loading">
        <el-table-column prop="orderNo" label="订单号" width="160" align="center" />
        <el-table-column label="商品信息" min-width="250">
          <template #default="{ row }">
            <div v-if="row.items && row.items.length > 0" class="goods-list">
              <div v-for="(item, index) in row.items.slice(0, 2)" :key="index" class="goods-info">
                <img :src="getImageUrl(item.productImage)" class="goods-image" />
                <div class="goods-detail">
                  <p class="goods-name">{{ item.productName }}</p>
                  <p class="goods-spec">¥{{ item.price }}<span v-if="item.unit" class="item-unit">/{{ item.unit }}</span> x {{ item.quantity }}</p>
                </div>
              </div>
              <div v-if="row.items.length > 2" class="more-goods">+{{ row.items.length - 2 }}件商品</div>
            </div>
            <div v-else class="goods-info">
              <img :src="getImageUrl(row.goodsImage)" class="goods-image" />
              <div class="goods-detail">
                <p class="goods-name">{{ row.goodsName }}</p>
                <p class="goods-spec">{{ row.goodsSpec }}</p>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="买家信息" min-width="150">
          <template #default="{ row }">
            <div class="buyer-info">
              <p>{{ row.receiverName || row.buyerName }}</p>
              <p class="buyer-phone">{{ row.receiverPhone || row.buyerPhone }}</p>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="payAmount" label="订单金额" width="120" align="center">
          <template #default="{ row }">
            <span class="amount">¥{{ row.payAmount || row.totalAmount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="160" align="center" />
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewDetail(row)">详情</el-button>
            <el-button v-if="row.status === 1" link type="primary" @click="shipOrder(row)">发货</el-button>
            <el-button v-if="row.status !== 3 && row.status !== 4" link type="danger" @click="cancelOrder(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total" layout="total, prev, pager, next" @current-change="handlePageChange" />
      </div>
    </el-card>

    <!-- 发货弹窗 -->
    <el-dialog v-model="showShipDialog" title="订单发货" width="500px">
      <el-form :model="shipForm" label-width="100px">
        <el-form-item label="订单号">
          <span>{{ shipForm.orderNo }}</span>
        </el-form-item>
        <el-form-item label="物流公司">
          <el-select v-model="shipForm.logisticsCompany" placeholder="请选择物流公司" style="width: 100%">
            <el-option label="顺丰速运" value="顺丰速运" />
            <el-option label="中通快递" value="中通快递" />
            <el-option label="圆通速递" value="圆通速递" />
            <el-option label="韵达快递" value="韵达快递" />
            <el-option label="申通快递" value="申通快递" />
            <el-option label="邮政EMS" value="邮政EMS" />
          </el-select>
        </el-form-item>
        <el-form-item label="物流单号">
          <el-input v-model="shipForm.logisticsNo" placeholder="请输入物流单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showShipDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmShip" :loading="shipLoading">确认发货</el-button>
      </template>
    </el-dialog>

    <!-- 订单详情弹窗 -->
    <el-dialog v-model="showDetailDialog" title="订单详情" width="700px">
      <div v-if="currentOrder" class="order-detail">
        <div class="detail-section">
          <h4>订单信息</h4>
          <p><label>订单号：</label>{{ currentOrder.orderNo }}</p>
          <p><label>订单状态：</label><el-tag :type="getStatusType(currentOrder.status)">{{ getStatusText(currentOrder) }}</el-tag></p>
          <p><label>下单时间：</label>{{ currentOrder.createTime }}</p>
          <p v-if="currentOrder.payTime"><label>支付时间：</label>{{ currentOrder.payTime }}</p>
          <p v-if="currentOrder.shipTime"><label>发货时间：</label>{{ currentOrder.shipTime }}</p>
        </div>
        <div class="detail-section">
          <h4>收货信息</h4>
          <p><label>收货人：</label>{{ currentOrder.receiverName }}</p>
          <p><label>联系电话：</label>{{ currentOrder.receiverPhone }}</p>
          <p><label>收货地址：</label>{{ currentOrder.receiverAddress }}</p>
        </div>
        <div class="detail-section">
          <h4>商品信息</h4>
          <div v-for="item in currentOrder.items" :key="item.id" class="detail-item">
            <img :src="getImageUrl(item.productImage)" class="detail-item-image" />
            <div class="detail-item-info">
              <p>{{ item.productName }}</p>
              <p class="detail-item-price">¥{{ item.price }}<span v-if="item.unit" class="item-unit">/{{ item.unit }}</span> x {{ item.quantity }}</p>
            </div>
          </div>
        </div>
        <div class="detail-section">
          <h4>金额信息</h4>
          <p><label>商品总额：</label>¥{{ currentOrder.totalAmount }}</p>
          <p><label>运费：</label>¥{{ currentOrder.freightAmount || 0 }}</p>
          <p v-if="currentOrder.discountAmount"><label>优惠：</label>-¥{{ currentOrder.discountAmount }}</p>
          <p class="detail-total"><label>实付金额：</label><span class="amount">¥{{ currentOrder.payAmount }}</span></p>
        </div>
        <div v-if="currentOrder.remark" class="detail-section">
          <h4>订单备注</h4>
          <p>{{ currentOrder.remark }}</p>
        </div>
        <div v-if="currentLogistics" class="detail-section">
          <h4>物流信息</h4>
          <p><label>物流公司：</label>{{ currentLogistics.deliveryName || '-' }}</p>
          <p><label>物流单号：</label>{{ currentLogistics.trackingNo || '-' }}</p>
          <p><label>配送员：</label>{{ currentLogistics.deliveryName || '-' }} {{ currentLogistics.deliveryPhone ? '(' + currentLogistics.deliveryPhone + ')' : '' }}</p>
          <p v-if="currentLogistics.pickupTime"><label>取件时间：</label>{{ currentLogistics.pickupTime }}</p>
          <p v-if="currentLogistics.deliveryTime"><label>配送时间：</label>{{ currentLogistics.deliveryTime }}</p>
          <p v-if="currentLogistics.signTime"><label>签收时间：</label>{{ currentLogistics.signTime }}</p>
        </div>
        <div v-if="currentLogisticsTracks && currentLogisticsTracks.length > 0" class="detail-section">
          <h4>物流轨迹</h4>
          <el-timeline>
            <el-timeline-item
              v-for="(track, index) in currentLogisticsTracks"
              :key="index"
              :timestamp="track.createTime"
              :type="index === 0 ? 'primary' : ''"
            >
              {{ track.description }}
              <span v-if="track.location" class="track-location">{{ track.location }}</span>
            </el-timeline-item>
          </el-timeline>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'
import { getImageUrl } from '@/utils/image'

const loading = ref(false)
const filterForm = reactive({
  orderNo: '',
  status: null as number | null,
  dateRange: [] as string[]
})
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const stats = ref({
  today: 0,
  pending: 0,
  shipped: 0,
  completed: 0
})

const orderList = ref<any[]>([])

// 发货弹窗
const showShipDialog = ref(false)
const shipLoading = ref(false)
const shipForm = reactive({
  orderId: null as number | null,
  orderNo: '',
  logisticsCompany: '',
  logisticsNo: ''
})

// 详情弹窗
const showDetailDialog = ref(false)
const currentOrder = ref<any>(null)

const getStatusType = (status: number) => {
  const map: Record<number, string> = { 0: 'warning', 1: 'danger', 2: 'primary', 3: 'success', 4: 'info' }
  return map[status] || 'info'
}

const getStatusText = (order: any) => {
  const map: Record<number, string> = { 0: '待付款', 1: '待发货', 2: '待收货', 3: '已完成', 4: '已取消' }
  
  if (order.status === 4) {
    if (order.cancelType === 2) {
      return '商家取消'
    } else if (order.cancelType === 1) {
      return '用户取消'
    }
  }
  
  return map[order.status] || '未知'
}

// 获取订单统计
const fetchStats = async () => {
  try {
    const data = await request.get('/admin/orders/stats') as any
    stats.value = {
      today: data.today || 0,
      pending: data.pending || 0,
      shipped: data.shipped || 0,
      completed: data.completed || 0
    }
  } catch (error) {
    console.error('获取订单统计失败:', error)
  }
}

// 获取订单列表
const fetchOrders = async () => {
  loading.value = true
  try {
    const params: any = {
      page: page.value,
      size: pageSize.value
    }
    
    if (filterForm.orderNo) {
      params.orderNo = filterForm.orderNo
    }
    if (filterForm.status !== null) {
      params.status = filterForm.status
    }
    if (filterForm.dateRange && filterForm.dateRange.length === 2) {
      params.startDate = filterForm.dateRange[0]
      params.endDate = filterForm.dateRange[1]
    }
    
    const data = await request.get('/admin/orders', { params }) as any
    orderList.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    console.error('获取订单列表失败:', error)
    ElMessage.error('获取订单列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  fetchOrders()
}

const resetFilter = () => {
  filterForm.orderNo = ''
  filterForm.status = null
  filterForm.dateRange = []
  page.value = 1
  fetchOrders()
}

const currentLogistics = ref<any>(null)
const currentLogisticsTracks = ref<any[]>([])

const viewDetail = async (row: any) => {
  try {
    const data = await request.get(`/admin/orders/${row.id}`) as any
    currentOrder.value = data.order
    currentLogistics.value = data.logistics || null
    currentLogisticsTracks.value = data.logisticsTracks || []
    showDetailDialog.value = true
  } catch (error) {
    ElMessage.error('获取订单详情失败')
  }
}

const shipOrder = (row: any) => {
  shipForm.orderId = row.id
  shipForm.orderNo = row.orderNo
  shipForm.logisticsCompany = ''
  shipForm.logisticsNo = ''
  showShipDialog.value = true
}

const confirmShip = async () => {
  if (!shipForm.logisticsNo) {
    ElMessage.warning('请输入物流单号')
    return
  }
  
  shipLoading.value = true
  try {
    await request.put(`/admin/orders/${shipForm.orderId}/ship`, {
      logisticsCompany: shipForm.logisticsCompany,
      logisticsNo: shipForm.logisticsNo
    })
    ElMessage.success('发货成功')
    showShipDialog.value = false
    fetchOrders()
    fetchStats()
  } catch (error: any) {
    ElMessage.error(error.message || '发货失败')
  } finally {
    shipLoading.value = false
  }
}

const cancelOrder = (row: any) => {
  ElMessageBox.confirm(`确定要取消订单"${row.orderNo}"吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await request.put(`/admin/orders/${row.id}/cancel`)
      ElMessage.success('订单已取消')
      fetchOrders()
      fetchStats()
    } catch (error: any) {
      ElMessage.error(error.message || '取消订单失败')
    }
  }).catch(() => {})
}

const handlePageChange = (val: number) => {
  page.value = val
  fetchOrders()
}

onMounted(() => {
  fetchStats()
  fetchOrders()
})
</script>

<style scoped>
.order-list-view {
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
  width: 140px;
}

.filter-date-range {
  width: 240px;
}

.filter-buttons {
  margin-left: auto;
}

.goods-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.goods-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.goods-image {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
}

.goods-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.goods-spec {
  font-size: 12px;
  color: #909399;
}

.more-goods {
  font-size: 12px;
  color: #909399;
  padding: 4px 0;
}

.buyer-info .buyer-phone {
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

.order-detail {
  max-height: 500px;
  overflow-y: auto;
}

.detail-section {
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #ebeef5;
}

.detail-section:last-child {
  border-bottom: none;
  margin-bottom: 0;
}

.detail-section h4 {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.detail-section p {
  margin: 8px 0;
  color: #606266;
}

.detail-section label {
  color: #909399;
  display: inline-block;
  width: 80px;
}

.detail-total {
  font-size: 16px;
  margin-top: 12px;
}

.detail-total .amount {
  font-size: 18px;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.detail-item-image {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
}

.detail-item-info {
  flex: 1;
}

.detail-item-price {
  color: #f56c6c;
  margin-top: 4px;
}

.item-unit {
  margin-left: 2px;
}
</style>
