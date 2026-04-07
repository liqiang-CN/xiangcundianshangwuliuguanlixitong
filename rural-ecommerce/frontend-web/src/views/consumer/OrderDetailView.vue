<template>
  <div class="order-detail-view">
    <div v-loading="loading" class="detail-container">
      <el-breadcrumb class="breadcrumb">
        <el-breadcrumb-item :to="{ path: '/consumer/orders' }">我的订单</el-breadcrumb-item>
        <el-breadcrumb-item>订单详情</el-breadcrumb-item>
      </el-breadcrumb>

      <!-- 订单状态 -->
      <div class="status-section">
        <div class="status-header">
          <h2 class="status-title">{{ getStatusText(order) }}</h2>
          <p class="status-desc">{{ order.statusDesc }}</p>
        </div>
        <el-steps :active="order.status + 1" finish-status="success" class="status-steps">
          <el-step title="提交订单" />
          <el-step title="付款成功" />
          <el-step title="商品发货" />
          <el-step title="确认收货" />
          <el-step title="订单完成" />
        </el-steps>
      </div>

      <!-- 物流信息 -->
      <div v-if="order.status >= 2" class="logistics-section">
        <h3 class="section-title">物流信息</h3>
        <div v-if="logistics.tracks && logistics.tracks.length > 0" class="logistics-info">
          <div class="logistics-header">
            <p class="logistics-company">{{ getLogisticsCompanyName(logistics.company) }} {{ logistics.trackingNo }}</p>
            <div v-if="logistics.deliveryName" class="delivery-info">
              <el-tag type="success" size="small">配送员</el-tag>
              <span class="delivery-name">{{ logistics.deliveryName }}</span>
              <span class="delivery-phone">{{ logistics.deliveryPhone }}</span>
            </div>
          </div>
          <el-timeline class="logistics-timeline">
            <el-timeline-item
              v-for="(track, index) in logistics.tracks"
              :key="index"
              :timestamp="track.time"
              :type="index === 0 ? 'primary' : ''"
            >
              {{ track.content }}
            </el-timeline-item>
          </el-timeline>
        </div>
        <div v-else class="logistics-empty">
          <p>暂无物流信息</p>
        </div>
      </div>

      <!-- 订单信息 -->
      <div class="info-section">
        <h3 class="section-title">订单信息</h3>
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">订单编号</span>
            <span class="info-value">{{ order.orderNo }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">创建时间</span>
            <span class="info-value">{{ order.createTime }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">支付方式</span>
            <span class="info-value">微信支付</span>
          </div>
          <div class="info-item">
            <span class="info-label">支付时间</span>
            <span class="info-value">{{ order.payTime || '-' }}</span>
          </div>
        </div>
      </div>

      <!-- 收货信息和发货信息 -->
      <div class="address-section">
        <div class="address-grid">
          <!-- 收货信息 -->
          <div class="address-box">
            <div class="address-label-vertical">我</div>
            <div class="address-content">
              <h3 class="section-title">收货信息</h3>
              <div class="address-info">
                <p class="address-name">{{ order.receiverName }} {{ order.receiverPhone }}</p>
                <p class="address-detail">{{ order.receiverAddress }}</p>
              </div>
            </div>
          </div>
          <!-- 发货信息 -->
          <div class="address-box" v-if="order.senderName">
            <div class="address-label-vertical">商家</div>
            <div class="address-content">
              <h3 class="section-title">发货信息</h3>
              <div class="address-info">
                <p class="address-name">{{ order.senderName }} {{ order.senderPhone }}</p>
                <p class="address-detail">{{ order.senderAddress }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 商品信息 -->
      <div class="products-section">
        <h3 class="section-title">商品信息</h3>
        <div class="product-list">
          <div v-for="item in order.items" :key="item.id" class="product-item">
            <img :src="getImageUrl(item.productImage || item.image)" class="product-image" />
            <div class="product-info">
              <h4 class="product-name">{{ item.productName || item.name }}</h4>
              <p class="product-price">¥{{ item.price }} x {{ item.quantity }}</p>
            </div>
            <span class="product-total">¥{{ (item.price * item.quantity).toFixed(2) }}</span>
          </div>
        </div>
        <div class="price-summary">
          <div class="price-row">
            <span>商品总额</span>
            <span>¥{{ order.totalAmount }}</span>
          </div>
          <div class="price-row">
            <span>运费</span>
            <span>¥{{ order.freightAmount }}</span>
          </div>
          <div class="price-row">
            <span>优惠</span>
            <span>-¥{{ order.discountAmount }}</span>
          </div>
          <div class="price-row total">
            <span>实付金额</span>
            <span class="total-amount">¥{{ order.payAmount }}</span>
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="action-section">
        <el-button v-if="order.status === 0" type="primary" size="large" @click="payOrder">立即付款</el-button>
        <el-button v-if="order.status === 2" type="primary" size="large" @click="confirmReceive">确认收货</el-button>
        <el-button v-if="order.status === 3" size="large" @click="reviewOrder">评价</el-button>
        <el-button size="large" @click="$router.push('/consumer/orders')">返回订单列表</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orderApi } from '@/api/order'
import request from '@/api/request'
import { getImageUrl } from '@/utils/image'

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

// 物流公司代码转中文名称
const getLogisticsCompanyName = (code: string): string => {
  const companyMap: Record<string, string> = {
    'sf': '顺丰速运',
    'zt': '中通快递',
    'yt': '圆通快递',
    'yd': '韵达快递'
  }
  return companyMap[code] || code
}

const route = useRoute()
const router = useRouter()
const orderId = Number(route.params.id)
const loading = ref(false)

// 订单数据
const order = ref<any>({
  id: 0,
  orderNo: '',
  status: 0,
  statusText: '',
  statusDesc: '',
  createTime: '',
  payType: '',
  payTime: '',
  receiverName: '',
  receiverPhone: '',
  receiverAddress: '',
  totalAmount: 0,
  freightAmount: 0,
  discountAmount: 0,
  payAmount: 0,
  items: []
})

// 物流数据
const logistics = ref<any>({
  company: '',
  trackingNo: '',
  tracks: []
})

// 获取订单详情
const fetchOrderDetail = async () => {
  if (!orderId) {
    ElMessage.error('订单ID无效')
    return
  }
  
  loading.value = true
  try {
    const res = await orderApi.getOrderDetail(orderId)
    if (res) {
      order.value = res
      // 如果有物流信息，获取物流详情
      if (order.value.status >= 2) {
        fetchLogistics()
      }
    }
  } catch (error) {
    console.error('获取订单详情失败:', error)
    ElMessage.error('获取订单详情失败')
  } finally {
    loading.value = false
  }
}

// 获取物流信息
const fetchLogistics = async () => {
  try {
    // 先获取物流记录
    const logisticsRes: any = await request.get(`/logistics/order/${orderId}`)
    if (logisticsRes) {
      logistics.value.company = order.value.logisticsCompany || ''
      logistics.value.trackingNo = logisticsRes.trackingNo || ''
      logistics.value.deliveryName = logisticsRes.deliveryName || ''
      logistics.value.deliveryPhone = logisticsRes.deliveryPhone || ''

      // 获取物流轨迹
      const tracksRes: any = await request.get(`/logistics/${logisticsRes.id}/tracks`)
      if (tracksRes && Array.isArray(tracksRes)) {
        logistics.value.tracks = tracksRes.map((track: any) => ({
          time: track.createTime,
          content: track.description
        }))
      }
    }
  } catch (error) {
    console.error('获取物流信息失败:', error)
  }
}

onMounted(() => {
  fetchOrderDetail()
})

const payOrder = () => {
  router.push(`/consumer/order/${orderId}/pay`)
}

const confirmReceive = async () => {
  try {
    await ElMessageBox.confirm('确认已收到商品？', '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await orderApi.confirmReceive(orderId)
    ElMessage.success('确认收货成功')
    // 刷新订单详情
    fetchOrderDetail()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('确认收货失败:', error)
      ElMessage.error('确认收货失败')
    }
  }
}

const reviewOrder = () => {
  router.push(`/consumer/order/${orderId}/review`)
}
</script>

<style scoped>
.order-detail-view {
  max-width: 1000px;
  margin: 0 auto;
  padding: 24px;
}

.breadcrumb {
  margin-bottom: 24px;
}

.detail-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.status-section,
.logistics-section,
.info-section,
.address-section,
.products-section {
  background: var(--apple-card-bg);
  border-radius: var(--apple-radius-lg);
  padding: 32px;
}

.status-header {
  margin-bottom: 32px;
}

.status-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--apple-dark-gray);
  margin-bottom: 8px;
}

.status-desc {
  font-size: 14px;
  color: var(--apple-gray);
}

.status-steps {
  margin-top: 24px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--apple-dark-gray);
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--apple-light-gray);
}

.logistics-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.logistics-company {
  font-size: 14px;
  color: var(--apple-dark-gray);
}

.delivery-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.delivery-name {
  font-size: 14px;
  color: var(--apple-dark-gray);
  font-weight: 500;
}

.delivery-phone {
  font-size: 13px;
  color: var(--apple-gray);
}

.logistics-timeline {
  padding-left: 8px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.info-item {
  display: flex;
  gap: 16px;
}

.info-label {
  width: 80px;
  font-size: 14px;
  color: var(--apple-gray);
}

.info-value {
  font-size: 14px;
  color: var(--apple-dark-gray);
}

.address-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 32px;
}

.address-box {
  display: flex;
  align-items: center;
  gap: 16px;
}

.address-label-vertical {
  writing-mode: vertical-rl;
  text-orientation: upright;
  font-size: 14px;
  font-weight: 600;
  color: var(--apple-dark-gray);
  padding: 8px 4px;
  background: var(--apple-light-gray);
  border-radius: 4px;
  letter-spacing: 2px;
}

.address-content {
  flex: 1;
}

.address-content .section-title {
  margin-bottom: 12px;
  padding-bottom: 8px;
  font-size: 16px;
}

.address-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--apple-dark-gray);
  margin-bottom: 8px;
}

.address-detail {
  font-size: 14px;
  color: var(--apple-gray);
}

.product-list {
  margin-bottom: 24px;
}

.product-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 0;
  border-bottom: 1px solid var(--apple-light-gray);
}

.product-item:last-child {
  border-bottom: none;
}

.product-image {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
}

.product-info {
  flex: 1;
}

.product-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--apple-dark-gray);
  margin-bottom: 8px;
}

.product-price {
  font-size: 14px;
  color: var(--apple-gray);
}

.product-total {
  font-size: 16px;
  font-weight: 600;
  color: var(--apple-dark-gray);
}

.price-summary {
  border-top: 1px solid var(--apple-light-gray);
  padding-top: 20px;
}

.price-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 14px;
  color: var(--apple-gray);
}

.price-row.total {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--apple-light-gray);
  font-size: 16px;
  font-weight: 600;
  color: var(--apple-dark-gray);
}

.total-amount {
  font-size: 24px;
  color: #ff6b6b;
}

.action-section {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 24px;
  background: var(--apple-card-bg);
  border-radius: var(--apple-radius-lg);
}

.action-section .el-button {
  min-width: 120px;
  height: 44px;
  border-radius: 22px;
}
</style>
