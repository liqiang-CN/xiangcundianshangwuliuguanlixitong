<template>
  <div class="order-manage-view">
    <h2 class="page-title">订单管理</h2>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <div class="stat-card">
          <p class="stat-label">待发货</p>
          <p class="stat-value">{{ stats.pending }}</p>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <p class="stat-label">待收货</p>
          <p class="stat-value">{{ stats.shipped }}</p>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <p class="stat-label">已完成</p>
          <p class="stat-value">{{ stats.completed }}</p>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <p class="stat-label">退款/售后</p>
          <p class="stat-value">{{ stats.refund }}</p>
        </div>
      </el-col>
    </el-row>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-input v-model="searchKeyword" placeholder="订单号/商品名称" style="width: 240px" clearable />
      <el-select v-model="filterStatus" placeholder="订单状态" clearable style="width: 140px">
        <el-option label="全部" value="" />
        <el-option label="待发货" value="1" />
        <el-option label="待收货" value="2" />
        <el-option label="已完成" value="3" />
      </el-select>
      <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" />
      <el-button type="primary" @click="handleSearch">查询</el-button>
    </div>

    <!-- 订单列表 -->
    <div class="order-list">
      <el-card v-for="order in orders" :key="order.id" class="order-card" shadow="never">
        <template #header>
          <div class="order-header">
            <div class="order-info-left">
              <span class="order-no">订单号：{{ order.orderNo }}</span>
              <span class="order-time">{{ order.createTime }}</span>
              <span v-if="order.status >= 2 && order.logisticsCompany" class="logistics-info">
                物流公司：{{ getLogisticsCompanyName(order.logisticsCompany) }}<span class="tracking-no">物流单号：{{ order.logisticsNo }}</span>
              </span>
            </div>
            <el-tag :type="getStatusType(order.status)">{{ getStatusText(order.status) }}</el-tag>
          </div>
        </template>
        <div class="order-content">
          <div v-for="item in order.items" :key="item.id" class="order-item">
            <img :src="getImageUrl(item.image)" class="item-image" />
            <div class="item-info">
              <h4 class="item-name">{{ item.name }}</h4>
              <p class="item-spec">{{ item.spec }}</p>
            </div>
            <div class="item-price">
              <p>¥{{ item.price }}<span v-if="item.unit" class="item-unit">/{{ item.unit }}</span></p>
              <p class="item-quantity">x{{ item.quantity }}</p>
            </div>
            <div class="receiver-info">
              <p class="receiver-label">收货信息</p>
              <p class="receiver-name">{{ order.receiverName }} {{ order.receiverPhone }}</p>
              <p class="address">{{ order.address }}</p>
            </div>
            <div class="sender-info" v-if="order.status >= 2">
              <p class="sender-label">发货信息</p>
              <p class="sender-name">{{ order.senderName }} {{ order.senderPhone }}</p>
              <p class="address">{{ order.senderAddress }}</p>
            </div>
            <div class="sender-info sender-info-empty" v-else>
              <p class="sender-label">发货信息</p>
              <p class="empty-text">待发货</p>
            </div>
            <div class="order-summary">
              <span class="total-text">共 {{ order.totalQuantity }} 件商品</span>
              <span class="total-amount">实付：<strong>¥{{ order.payAmount }}</strong></span>
              <div class="action-buttons">
                <el-button v-if="order.status === 1" type="primary" size="small" @click="shipOrder(order)">发货</el-button>
                <el-button v-if="order.status === 1" type="danger" size="small" @click="cancelOrder(order)">取消订单</el-button>
              </div>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 发货弹窗 -->
    <el-dialog v-model="showShipDialog" title="订单发货" width="550px">
      <el-form :model="shipForm" label-width="100px">
        <el-form-item label="物流公司">
          <el-select v-model="shipForm.company" placeholder="请选择物流公司" style="width: 100%">
            <el-option label="顺丰速运" value="sf" />
            <el-option label="中通快递" value="zt" />
            <el-option label="圆通快递" value="yt" />
            <el-option label="韵达快递" value="yd" />
          </el-select>
        </el-form-item>
        <el-form-item label="物流单号">
          <el-input v-model="shipForm.trackingNo" placeholder="请输入物流单号" />
        </el-form-item>
        <el-divider content-position="left">发货人信息</el-divider>
        <el-form-item label="发货联系人">
          <el-input v-model="shipForm.senderName" placeholder="请输入发货联系人姓名" />
        </el-form-item>
        <el-form-item label="发货人电话">
          <el-input v-model="shipForm.senderPhone" placeholder="请输入发货人联系电话" />
        </el-form-item>
        <el-form-item label="所在地区">
          <el-cascader
            v-model="shipForm.region"
            :options="regionOptions"
            placeholder="请选择省/市/区"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="详细地址">
          <el-input v-model="shipForm.senderAddress" type="textarea" :rows="2" placeholder="请输入详细发货地址" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" link @click="useDefaultAddress">
            <el-icon><Location /></el-icon>
            <span style="margin-left: 4px">使用默认发货地址</span>
          </el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showShipDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmShip">确认发货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Location } from '@element-plus/icons-vue'
import request from '@/api/request'
import { regionData, codeToText } from 'element-china-area-data'
import { wsService } from '@/utils/websocket'
import { useUserStore } from '@/stores/user'
import { getImageUrl } from '@/utils/image'

// 构建文本到编码的映射
const textToCode: { [key: string]: string } = {}
const buildTextToCode = (data: any[]) => {
  data.forEach(item => {
    textToCode[item.label] = item.value
    if (item.children) {
      buildTextToCode(item.children)
    }
  })
}
buildTextToCode(regionData)

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

const searchKeyword = ref('')
const filterStatus = ref('')
const dateRange = ref([])
const loading = ref(false)

const stats = ref({
  pending: 0,
  shipped: 0,
  completed: 0,
  refund: 0
})

const orders = ref<any[]>([])

const showShipDialog = ref(false)
const currentOrder = ref<any>(null)
const shipForm = ref<{
  company: string
  trackingNo: string
  senderName: string
  senderPhone: string
  senderAddress: string
  region: string[]
}>({
  company: '',
  trackingNo: '',
  senderName: '',
  senderPhone: '',
  senderAddress: '',
  region: []
})

// 地区选择器选项
const regionOptions = regionData

// 获取订单统计
const fetchStats = async () => {
  try {
    const res: any = await request.get('/farmer/stats')
    if (res) {
      stats.value = {
        pending: res.pendingOrders || 0,      // 待发货
        shipped: res.shippedOrders || 0,      // 待收货（已发货，等待用户确认）
        completed: res.completedOrders || 0,  // 已完成（用户确认收货）
        refund: 0
      }
    }
  } catch (error) {
    console.error('获取统计失败:', error)
  }
}

// 获取订单列表
const fetchOrders = async () => {
  loading.value = true
  try {
    const params: any = { page: 1, size: 20 }
    if (filterStatus.value) {
      params.status = parseInt(filterStatus.value)
    }
    if (searchKeyword.value) {
      params.keyword = searchKeyword.value
    }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const res: any = await request.get('/farmer/orders', { params })
    if (res && Array.isArray(res)) {
      orders.value = res.map((order: any) => ({
        id: order.id,
        orderNo: order.orderNo,
        createTime: order.createTime,
        status: order.status,
        receiverName: order.receiverName,
        receiverPhone: order.receiverPhone,
        address: order.receiverAddress,
        senderName: order.senderName,
        senderPhone: order.senderPhone,
        senderAddress: order.senderAddress,
        logisticsCompany: order.logisticsCompany,
        logisticsNo: order.logisticsNo,
        totalQuantity: order.items?.reduce((sum: number, item: any) => sum + item.quantity, 0) || 0,
        payAmount: order.payAmount,
        items: order.items?.map((item: any) => ({
          id: item.id,
          name: item.productName,
          spec: '',
          price: item.price,
          unit: item.unit,
          quantity: item.quantity,
          image: item.productImage
        })) || []
      }))
    }
  } catch (error) {
    console.error('获取订单失败:', error)
    ElMessage.error('获取订单列表失败')
  } finally {
    loading.value = false
  }
}

const getStatusType = (status: number) => {
  const map: Record<number, string> = { 0: 'danger', 1: 'warning', 2: 'primary', 3: 'success', 4: 'info' }
  return map[status] || 'info'
}

const getStatusText = (status: number) => {
  const map: Record<number, string> = { 0: '待付款', 1: '待发货', 2: '待收货', 3: '已完成', 4: '已取消' }
  return map[status] || '未知'
}

const handleSearch = () => {
  fetchOrders()
}

const shipOrder = (order: any) => {
  currentOrder.value = order
  showShipDialog.value = true
}

// 监听物流公司选择变化，调用后端生成物流单号
watch(() => shipForm.value.company, async (newCompany) => {
  if (newCompany) {
    try {
      const res: any = await request.get('/farmer/tracking-no', {
        params: { company: newCompany }
      })
      if (res) {
        shipForm.value.trackingNo = res
      }
    } catch (error) {
      console.error('生成物流单号失败:', error)
      ElMessage.error('生成物流单号失败')
    }
  }
})

// 取消订单
const cancelOrder = async (order: any) => {
  try {
    await ElMessageBox.confirm('确定要取消该订单吗？取消后用户将看到订单已被商家取消', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await request.put(`/farmer/order/${order.id}/cancel`)
    ElMessage.success('订单已取消')
    fetchOrders()
    fetchStats()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('取消订单失败:', error)
      ElMessage.error('取消订单失败')
    }
  }
}

// 辅助函数：查找地区编码
const findCode = (name: string): string | undefined => {
  // 直接匹配
  if (textToCode[name]) return textToCode[name]
  
  // 去掉"市"、"省"、"区"等后缀再匹配
  const nameWithoutSuffix = name.replace(/[省市自治区回族壮族维吾尔族特别行政区]$/, '')
  if (textToCode[nameWithoutSuffix]) return textToCode[nameWithoutSuffix]
  
  // 模糊匹配
  const matched = Object.keys(textToCode).find(key => {
    const keyWithoutSuffix = key.replace(/[省市自治区回族壮族维吾尔族特别行政区]$/, '')
    return name.includes(key) || key.includes(name) ||
           nameWithoutSuffix.includes(keyWithoutSuffix) || 
           keyWithoutSuffix.includes(nameWithoutSuffix)
  })
  return matched ? textToCode[matched] : undefined
}

// 使用默认发货地址
const useDefaultAddress = async () => {
  try {
    const res: any = await request.get('/farmer/address/default')
    if (res) {
      shipForm.value.senderName = res.senderName
      shipForm.value.senderPhone = res.senderPhone
      shipForm.value.senderAddress = res.detailAddress
      
      // 将文本名称转换为编码
      const provinceCode = findCode(res.province)
      const cityCode = findCode(res.city)
      const districtCode = findCode(res.district)
      
      if (!provinceCode) {
        ElMessage.warning('无法识别省份信息，请手动选择地区')
        return
      }
      
      // 判断是否为直辖市（省编码和市编码相同，或市名和省名相同）
      const isMunicipality = provinceCode === cityCode || res.province === res.city
      
      // 清空数组
      shipForm.value.region.splice(0, shipForm.value.region.length)
      
      // 添加省编码
      shipForm.value.region.push(provinceCode)
      
      if (districtCode) {
        if (isMunicipality) {
          // 直辖市：需要找到"市辖区"的编码
          const provinceData = regionData.find(p => p.value === provinceCode)
          const cityData = provinceData?.children?.[0] // 市辖区通常是第一个
          const municipalityCityCode = cityData?.value || cityCode || provinceCode + '01'
          shipForm.value.region.push(municipalityCityCode, districtCode)
        } else if (cityCode) {
          // 非直辖市，且成功获取到市编码
          shipForm.value.region.push(cityCode, districtCode)
        } else {
          // 非直辖市，但未获取到市编码，只添加区编码
          shipForm.value.region.push(districtCode)
        }
      }
      
      ElMessage.success('已填充默认发货地址')
    } else {
      ElMessage.warning('暂无默认发货地址，请先设置')
    }
  } catch (error) {
    console.error('获取默认地址失败:', error)
    ElMessage.error('获取默认地址失败')
  }
}

const confirmShip = async () => {
  if (!shipForm.value.company || !shipForm.value.trackingNo) {
    ElMessage.warning('请填写完整的物流信息')
    return
  }
  if (!shipForm.value.senderName || !shipForm.value.senderPhone) {
    ElMessage.warning('请填写发货联系人姓名和电话')
    return
  }
  if (!shipForm.value.region || shipForm.value.region.length === 0) {
    ElMessage.warning('请选择所在地区')
    return
  }
  if (!shipForm.value.senderAddress.trim()) {
    ElMessage.warning('请输入详细地址')
    return
  }
  
  // 将地区编码转换为文本
  const regionText = shipForm.value.region.map(code => codeToText[code as keyof typeof codeToText]).join('')
  const fullAddress = regionText + shipForm.value.senderAddress
  
  try {
    await request.post(`/farmer/order/${currentOrder.value.id}/ship`, {
      logisticsNo: shipForm.value.trackingNo,
      logisticsCompany: shipForm.value.company,
      senderName: shipForm.value.senderName,
      senderPhone: shipForm.value.senderPhone,
      senderAddress: fullAddress
    })
    ElMessage.success('发货成功')
    showShipDialog.value = false
    shipForm.value = { company: '', trackingNo: '', senderName: '', senderPhone: '', senderAddress: '', region: [] }
    fetchOrders()
    fetchStats()
  } catch (error) {
    console.error('发货失败:', error)
    ElMessage.error('发货失败')
  }
}

onMounted(() => {
  fetchStats()
  fetchOrders()

  // 获取当前农户ID并连接WebSocket
  const userStore = useUserStore()
  const farmerId = userStore.userInfo?.id
  if (farmerId) {
    console.log('正在连接WebSocket，农户ID:', farmerId)
    wsService.connect(farmerId)

    // 监听新订单通知
    wsService.on('NEW_ORDER', (data) => {
      console.log('收到新订单通知:', data)
      ElMessage.success(`收到新订单：${data.productName} x${data.quantity}`)
      // 刷新订单列表和统计
      fetchOrders()
      fetchStats()
    })
  } else {
    console.warn('未获取到农户ID，无法连接WebSocket')
  }
})

onUnmounted(() => {
  // 断开WebSocket连接
  wsService.disconnect()
})
</script>

<style scoped>
.order-manage-view {
  padding-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--apple-dark-gray);
  margin-bottom: 24px;
}

.stat-row {
  margin-bottom: 24px;
}

.stat-card {
  background: var(--apple-card-bg);
  border-radius: 12px;
  padding: 24px;
  text-align: center;
}

.stat-label {
  font-size: 14px;
  color: var(--apple-gray);
  margin-bottom: 8px;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--apple-dark-gray);
}

.filter-bar {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
  padding: 20px;
  background: var(--apple-card-bg);
  border-radius: 12px;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-card {
  border-radius: 12px;
}

.order-card :deep(.el-card__header) {
  padding: 0;
  border-bottom: none;
}

.order-card :deep(.el-card__body) {
  padding: 0;
}

.order-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 16px 20px;
  background: var(--apple-bg);
  font-size: 14px;
  background-color: #fff;
  border-bottom: 1px solid var(--apple-light-gray);
}

.order-info-left {
  display: flex;
  align-items: center;
  gap: 24px;
}

.order-no {
  font-weight: 500;
  color: var(--apple-dark-gray);
}

.order-time {
  color: var(--apple-gray);
}

.logistics-info {
  color: var(--apple-dark-gray);
  font-size: 13px;
  padding: 4px 0;
  margin-left: 100px;
}

.tracking-no {
  margin-left: 25px;
}

.order-content {
  padding: 20px;
}

.order-item {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 16px 0;
  border-bottom: 1px solid var(--apple-light-gray);
}

.order-item:last-child {
  border-bottom: none;
}

.item-image {
  width: 72px;
  height: 72px;
  object-fit: cover;
  border-radius: 8px;
  flex-shrink: 0;
}

.item-info {
  width: 180px;
  flex-shrink: 0;
}

.item-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--apple-dark-gray);
  margin-bottom: 6px;
  line-height: 1.4;
}

.item-spec {
  font-size: 12px;
  color: var(--apple-gray);
}

.item-price {
  width: 80px;
  text-align: right;
  flex-shrink: 0;
}

.item-price p:first-child {
  font-size: 14px;
  font-weight: 500;
  color: var(--apple-dark-gray);
}

.item-unit {
  margin-left: 2px;
}

.item-quantity {
  color: var(--apple-gray);
  font-size: 12px;
  margin-top: 4px;
}

.receiver-info {
  flex: 1;
  font-size: 13px;
  padding-left: 24px;
  border-left: 1px solid var(--apple-light-gray);
  margin-left: 16px;
  min-width: 140px;
}

.receiver-label,
.sender-label {
  font-size: 11px;
  color: var(--apple-gray);
  margin-bottom: 6px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.receiver-name,
.sender-name {
  font-weight: 500;
  color: var(--apple-dark-gray);
  margin-bottom: 6px;
}

.receiver-info .address,
.sender-info .address {
  color: var(--apple-gray);
  font-size: 12px;
  line-height: 1.4;
}

.sender-info {
  flex: 1;
  font-size: 13px;
  padding-left: 24px;
  border-left: 1px solid var(--apple-light-gray);
  margin-left: 16px;
  min-width: 140px;
}

.sender-info-empty .empty-text {
  color: #999;
  font-size: 12px;
  font-style: italic;
}

.order-summary {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-left: 24px;
  border-left: 1px solid var(--apple-light-gray);
  margin-left: 16px;
  flex-shrink: 0;
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
}

.action-buttons :deep(.el-button) {
  margin-left: 0 !important;
}

.total-text {
  color: var(--apple-gray);
  font-size: 14px;
}

.total-amount {
  font-size: 16px;
  color: var(--apple-dark-gray);
  margin-right: 4vw;
}

.total-amount strong {
  font-size: 18px;
  font-weight: 700;
  color: #ff6b6b;
}
</style>
