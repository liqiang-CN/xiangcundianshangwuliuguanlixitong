<template>
  <div class="order-list-view">
    <div class="order-container">
      <h2 class="page-title">我的订单</h2>

      <div class="order-layout">
        <!-- 左侧订单状态标签 -->
        <div class="status-tabs-vertical">
          <div
            v-for="tab in statusTabs"
            :key="tab.value"
            class="tab-item-vertical"
            :class="{ active: currentStatus === tab.value }"
            @click="currentStatus = tab.value"
          >
            <span class="tab-label">{{ tab.label }}</span>
            <el-badge 
              v-if="getStatusCount(tab.value) > 0" 
              :value="getStatusCount(tab.value)" 
              class="tab-badge-vertical" 
            />
          </div>
        </div>

        <!-- 右侧订单列表 -->
        <div v-loading="loading" class="order-list-scroll">
          <div class="order-list">
        <div v-for="order in orders" :key="order.id" class="order-card">
          <div class="order-header">
            <span class="order-time">{{ order.createTime?.split('T')[0] }}</span>
            <span class="order-no">订单号：{{ order.orderNo }}</span>
            <span class="order-shop" v-if="order.shopName" @click.stop="goToShop(order.shopId)">
              <el-icon><Shop /></el-icon>
              {{ order.shopName }}
            </span>
            <span class="order-status" :class="'status-' + order.status">{{ order.statusText }}</span>
          </div>

          <div class="order-items">
            <div v-for="item in order.items" :key="item.id" class="order-item">
              <img :src="getImageUrl(item.productImage)" class="item-image" />
              <div class="item-info">
                <h4 class="item-name">{{ item.productName }}</h4>
                <p class="item-price">¥{{ item.price }}<span v-if="item.unit" class="item-unit">/{{ item.unit }}</span><span class="item-quantity">x{{ item.quantity }}</span></p>
              </div>
            </div>
          </div>

          <div class="order-footer">
            <div class="order-total">
              <span class="total-amount">
                实付：<strong>¥{{ order.payAmount }}</strong>
              </span>
            </div>
            <div class="order-actions">
              <el-button v-if="order.status === 0" type="primary" @click="payOrder(order.id)">立即付款</el-button>
              <el-button v-if="order.status === 2" type="primary" @click="confirmReceive(order.id)">确认收货</el-button>
              <el-button v-if="order.status === 3" @click="reviewOrder(order.id)">评价</el-button>
              <el-button link @click="viewDetail(order.id)">查看详情</el-button>
              <el-button v-if="order.status !== 0 && order.status !== 2" type="primary" @click="rebuyOrder(order)">再买一单</el-button>
              <el-button v-if="order.status === 0" link type="danger" @click="cancelOrder(order.id)">取消订单</el-button>
            </div>
          </div>
        </div>
          </div>

          <!-- 空状态 -->
          <el-empty v-if="orders.length === 0" description="暂无订单" />

          <!-- 分页 -->
          <div v-if="orders.length > 0" class="pagination-wrapper">
            <el-pagination
              v-model:current-page="page"
              v-model:page-size="pageSize"
              :total="total"
              layout="prev, pager, next, jumper"
              @current-change="handlePageChange"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Shop } from '@element-plus/icons-vue'
import { orderApi } from '@/api/order'
import { getImageUrl } from '@/utils/image'

const router = useRouter()
const route = useRoute()

// 从URL参数获取状态
const getStatusFromQuery = () => {
  const status = route.query.status
  if (status === '0') return 'unpaid'
  if (status === '1') return 'unshipped'
  if (status === '2') return 'unreceived'
  if (status === '3') return 'completed'
  if (status === '4') return 'cancelled'
  return 'all'
}

const currentStatus = ref(getStatusFromQuery())
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)

// 状态映射
const statusMap: Record<string, number | null> = {
  'all': null,
  'unpaid': 0,      // 待付款
  'unshipped': 1,   // 待发货
  'unreceived': 2,  // 待收货
  'completed': 3,   // 已完成
  'cancelled': 4    // 已取消
}

// 获取订单状态文本
const getStatusText = (order: any): string => {
  const statusMap: Record<number, string> = {
    0: '待付款',
    1: '待发货',
    2: '待收货',
    3: '已完成',
    4: '已取消'
  }

  if (order.status === 4) {
    // 已取消状态，根据 cancelType 显示不同文本
    if (order.cancelType === 2) {
      return '订单已被商家取消'
    } else if (order.cancelType === 1) {
      return '订单已取消'
    }
  }

  return statusMap[order.status] || '未知状态'
}

// 各状态订单数量
const statusCounts = ref({
  unpaid: 0,
  unshipped: 0,
  unreceived: 0,
  completed: 0
})

const statusTabs = ref([
  { label: '全部', value: 'all' },
  { label: '待付款', value: 'unpaid' },
  { label: '待发货', value: 'unshipped' },
  { label: '待收货', value: 'unreceived' },
  { label: '已完成', value: 'completed' },
  { label: '已取消', value: 'cancelled' }
])

const orders = ref<any[]>([])

// 获取订单列表
const fetchOrders = async () => {
  loading.value = true
  try {
    const params: any = {
      page: page.value,
      size: pageSize.value
    }
    // 添加状态筛选
    const status = statusMap[currentStatus.value]
    if (status !== null) {
      params.status = status
    }
    
    const res: any = await orderApi.getOrderList(params)
    
    // 适配后端返回格式
    if (Array.isArray(res)) {
      orders.value = res.map((order: any) => ({
        ...order,
        statusText: getStatusText(order)
      }))
      total.value = res.length
    } else {
      orders.value = (res.list || []).map((order: any) => ({
        ...order,
        statusText: getStatusText(order)
      }))
      total.value = res.total || 0
    }
    
    // 获取各状态订单数量
    fetchStatusCounts()
  } catch (error) {
    console.error('获取订单列表失败:', error)
    ElMessage.error('获取订单列表失败')
    orders.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}



// 获取指定状态的订单数量
const getStatusCount = (statusValue: string): number => {
  if (statusValue === 'all' || statusValue === 'cancelled') return 0
  const count = statusCounts.value[statusValue as keyof typeof statusCounts.value]
  return count || 0
}

// 获取各状态订单数量（从后端查询）
const fetchStatusCounts = async () => {
  try {
    // 并行获取各状态订单列表
    const [unpaidRes, unshippedRes, unreceivedRes] = await Promise.all([
      orderApi.getOrderList({ page: 1, size: 100, status: 0 }),
      orderApi.getOrderList({ page: 1, size: 100, status: 1 }),
      orderApi.getOrderList({ page: 1, size: 100, status: 2 })
    ])

    // 计算各状态数量
    const getCount = (res: any) => {
      if (Array.isArray(res)) return res.length
      return res.list?.length || 0
    }

    statusCounts.value = {
      unpaid: getCount(unpaidRes),
      unshipped: getCount(unshippedRes),
      unreceived: getCount(unreceivedRes),
      completed: 0 // 已完成不需要显示角标
    }
  } catch (error) {
    console.error('获取订单数量失败:', error)
  }
}

// 监听状态变化
watch(currentStatus, () => {
  page.value = 1
  fetchOrders()
  // 回到页面顶部
  window.scrollTo(0, 0)
})

const payOrder = (id: number) => {
  // 跳转到确认订单页面，传入订单ID进行支付
  router.push({
    path: '/consumer/orders/create',
    query: {
      orderId: id
    }
  })
}

const confirmReceive = async (id: number) => {
  try {
    await ElMessageBox.confirm('确认已收到商品？', '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await orderApi.confirmReceive(id)
    ElMessage.success('确认收货成功')
    fetchOrders()
  } catch (error) {
    console.error(error)
  }
}

const reviewOrder = (id: number) => {
  router.push(`/order/${id}/review`)
}

const viewDetail = (id: number) => {
  router.push(`/order/${id}`)
}

const goToShop = (shopId: number) => {
  if (shopId) {
    router.push(`/consumer/shop/${shopId}`)
  }
}

// 再买一单
const rebuyOrder = (order: any) => {
  if (!order.items || order.items.length === 0) {
    ElMessage.warning('订单商品信息不完整')
    return
  }
  
  // 构建商品数据，包含完整信息
  const cartItems = order.items.map((item: any) => ({
    productId: item.productId,
    productName: item.productName,
    productImage: item.productImage,
    price: item.price,
    unit: item.unit,
    quantity: item.quantity,
    farmerName: item.farmerName,
    farmerId: item.farmerId
  }))
  
  // 跳转到确认订单页面
  router.push({
    path: '/consumer/orders/create',
    query: {
      rebuy: '1',
      items: encodeURIComponent(JSON.stringify(cartItems))
    }
  })
}

const cancelOrder = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要取消该订单吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await orderApi.cancelOrder(id)
    ElMessage.success('订单已取消')
    fetchOrders()
  } catch (error) {
    console.error(error)
  }
}

const handlePageChange = (val: number) => {
  page.value = val
  fetchOrders()
}

// 页面加载时获取订单
onMounted(() => {
  fetchOrders()
  fetchStatusCounts()
})
</script>

<style scoped>
.order-list-view {
  width: 100%;
  margin: 0 auto;
}

.order-container {
  width: 100%;
  background: var(--apple-card-bg);
  padding: 32px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--apple-dark-gray);
  margin-bottom: 24px;
}

.status-tabs {
  display: flex;
  gap: clamp(8px, 3vw, 32px);
  padding-bottom: clamp(12px, 2vw, 16px);
  border-bottom: 1px solid var(--apple-light-gray);
  margin-bottom: clamp(16px, 3vw, 24px);
  flex-wrap: wrap;
  transition: gap 0.3s ease;
}

.tab-item {
  font-size: clamp(12px, 1.2vw, 14px);
  color: var(--apple-gray);
  cursor: pointer;
  padding: clamp(6px, 1vw, 8px) clamp(8px, 1.5vw, 12px);
  position: relative;
  transition: color 0.3s, font-size 0.3s ease, padding 0.3s ease;
  white-space: nowrap;
}

.tab-item.active {
  color: var(--apple-blue);
  font-weight: 600;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: clamp(-12px, -2vw, -16px);
  left: 0;
  right: 0;
  height: 2px;
  background: var(--apple-blue);
  transition: bottom 0.3s ease;
}

.tab-badge {
  position: absolute;
  top: clamp(-6px, -1vw, -8px);
  right: clamp(-8px, -2vw, -16px);
  transition: top 0.3s ease, right 0.3s ease;
}

.tab-badge :deep(.el-badge__content) {
  position: absolute;
  top: 0;
  right: 0;
  font-size: clamp(10px, 1vw, 12px);
  transition: font-size 0.3s ease;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-layout {
  display: flex;
  gap: 20px;
}

.status-tabs-vertical {
  width: 120px;
  flex-shrink: 0;
  background: var(--apple-card-bg);
  border-radius: var(--apple-radius-lg);
  padding: 0px 12px;
  height: fit-content;
  position: sticky;
  top: 80px;
  align-self: flex-start;
  z-index: 10;
}

.tab-item-vertical {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  margin-bottom: 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
  color: var(--apple-gray);
}

.tab-item-vertical:last-child {
  margin-bottom: 0;
}

.tab-item-vertical:hover {
  background: var(--apple-light-gray);
}

.tab-item-vertical.active {
  background: var(--apple-blue);
  color: #fff;
}

.tab-label {
  font-weight: 500;
}

.tab-badge-vertical :deep(.el-badge__content) {
  position: relative;
  top: 0;
  right: 0;
  transform: none;
}

.order-list-scroll {
  flex: 1;
}

.order-card {
  border: 1px solid var(--apple-light-gray);
  border-radius: 12px;
  overflow: hidden;
}

.order-header {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 16px 20px;
  background: var(--apple-bg);
  font-size: 14px;
}

.order-no {
  color: var(--apple-dark-gray);
  font-weight: 500;
}

.order-time {
  color: var(--apple-gray);
}

.order-shop {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--apple-blue);
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.2s;
}

.order-shop:hover {
  opacity: 0.8;
}

.order-status {
  margin-left: auto;
  font-weight: 600;
}

.status-0 { color: #ff9500; }
.status-1 { color: #007aff; }
.status-2 { color: #5856d6; }
.status-3 { color: #34c759; }
.status-4 { color: #8e8e93; }

.order-items {
  padding: 20px;
}

.order-item {
  display: flex;
  gap: 16px;
  padding: 12px 0;
}

.item-image {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
}

.item-info {
  flex: 1;
}

.item-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--apple-dark-gray);
  margin-bottom: 8px;
}

.item-price {
  font-size: 14px;
  color: var(--apple-gray);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.item-unit {
  margin-left: 2px;
}

.item-quantity {
  margin-left: auto;
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-top: 1px solid var(--apple-light-gray);
}

.order-total {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 14px;
  color: var(--apple-gray);
}

.total-amount strong {
  font-size: 20px;
  color: #ff6b6b;
}

.order-actions {
  display: flex;
  gap: 12px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}
</style>
