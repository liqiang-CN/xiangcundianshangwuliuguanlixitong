<template>
  <div class="order-create-view">
    <div class="order-container">
      <h2 class="page-title">确认订单</h2>
      
      <!-- 收货地址 -->
      <div v-if="!existingOrderId" class="section">
        <h3 class="section-title">收货地址</h3>
        <div v-if="selectedAddress" class="address-card">
          <div class="address-info">
            <span class="name">{{ selectedAddress.receiverName }}</span>
            <span class="phone">{{ selectedAddress.receiverPhone }}</span>
            <el-tag v-if="selectedAddress.isDefault === 1" type="primary" size="small">默认</el-tag>
            <p class="detail">{{ selectedAddress.province }}{{ selectedAddress.city }}{{ selectedAddress.district }}{{ selectedAddress.detailAddress }}</p>
          </div>
          <el-button link type="primary" @click="showAddressDialog = true">更换地址</el-button>
        </div>
        <div v-else-if="addressLoading" class="address-loading">
          <el-skeleton :rows="2" animated />
        </div>
        <div v-else class="no-address">
          <p class="no-address-tip">暂无收货地址，请先添加</p>
          <el-button type="primary" @click="goToAddAddress">添加收货地址</el-button>
        </div>
      </div>

      <!-- 待付款订单的收货地址显示（只读） -->
      <div v-if="existingOrderId && existingOrder" class="section">
        <h3 class="section-title">收货地址</h3>
        <div class="address-card">
          <div class="address-info">
            <span class="name">{{ existingOrder.receiverName }}</span>
            <span class="phone">{{ existingOrder.receiverPhone }}</span>
            <p class="detail">{{ existingOrder.receiverAddress }}</p>
          </div>
        </div>
      </div>

      <!-- 商品清单 -->
      <div class="section">
        <h3 class="section-title">商品清单</h3>
        <div class="product-list">
          <div v-for="item in selectedItems" :key="item.productId" class="product-item">
            <img :src="item.productImage" class="product-image" />
            <div class="product-info">
              <h4 class="product-name">{{ item.productName }}</h4>
              <p class="product-farmer">{{ item.farmerName }}</p>
            </div>
            <span class="product-price">¥{{ item.price }}<span v-if="item.unit" class="product-unit"> / {{ item.unit }}</span></span>
            <span class="product-quantity">x{{ item.quantity }}</span>
            <span class="product-total">¥{{ (item.price * item.quantity).toFixed(2) }}</span>
          </div>
        </div>
      </div>

      <!-- 订单备注 -->
      <div class="section">
        <h3 class="section-title">订单备注</h3>
        <el-input
          v-model="remark"
          type="textarea"
          :rows="3"
          placeholder="请输入订单备注（选填）"
          :disabled="!!existingOrderId"
        />
      </div>

      <!-- 订单汇总 -->
      <div class="section summary-section">
        <div class="summary-row">
          <span>商品总额</span>
          <span>¥{{ totalAmount.toFixed(2) }}</span>
        </div>
        <div class="summary-row">
          <span>运费</span>
          <span>¥{{ freightAmount.toFixed(2) }}</span>
        </div>
        <div class="summary-row total">
          <span>应付总额</span>
          <span class="total-price">¥{{ payAmount.toFixed(2) }}</span>
        </div>
      </div>

      <!-- 提交订单 -->
      <div class="submit-section">
        <el-button size="large" @click="$router.back()">{{ isDirectBuy || existingOrderId ? '返回' : '返回购物车' }}</el-button>
        <el-button type="primary" size="large" :loading="submitting || paying" :disabled="!existingOrderId && !selectedAddress" @click="handleSubmit">
          {{ existingOrderId ? '立即支付' : '提交订单' }}
        </el-button>
      </div>
    </div>

    <!-- 地址选择弹窗 -->
    <el-dialog 
      v-model="showAddressDialog" 
      title="选择收货地址" 
      :width="dialogWidth"
      class="address-dialog"
    >
      <div v-if="addresses.length === 0" class="empty-address">
        <el-empty description="暂无收货地址">
          <el-button type="primary" @click="goToAddAddress">添加地址</el-button>
        </el-empty>
      </div>
      <div v-else class="address-list">
        <div
          v-for="addr in addresses"
          :key="addr.id"
          class="address-option"
          :class="{ active: selectedAddress?.id === addr.id }"
          @click="selectAddress(addr)"
        >
          <div class="address-option-info">
            <span class="name">{{ addr.receiverName }}</span>
            <span class="phone">{{ addr.receiverPhone }}</span>
            <el-tag v-if="addr.isDefault === 1" size="small" type="success">默认</el-tag>
            <p class="detail">{{ addr.province }}{{ addr.city }}{{ addr.district }}{{ addr.detailAddress }}</p>
          </div>
          <el-icon v-if="selectedAddress?.id === addr.id" class="check-icon"><CircleCheck /></el-icon>
        </div>
      </div>
    </el-dialog>

    <!-- 支付弹窗 -->
    <el-dialog v-model="showPayDialog" title="确认支付" width="400px" :close-on-click-modal="false">
      <div class="pay-content">
        <p class="pay-amount">应付总额：<strong>¥{{ orderPayAmount.toFixed(2) }}</strong></p>
        <p class="pay-tip">模拟支付，点击确认即可完成支付</p>
      </div>
      <template #footer>
        <el-button @click="showPayDialog = false">取消</el-button>
        <el-button type="primary" :loading="paying" @click="handlePay">确认支付</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CircleCheck } from '@element-plus/icons-vue'
import { useCartStore } from '@/stores/cart'
import { useUserStore } from '@/stores/user'
import { orderApi, type CreateOrderParams } from '@/api/order'
import { productApi } from '@/api/product'
import { addressApi, type Address } from '@/api/address'

const router = useRouter()
const route = useRoute()
const cartStore = useCartStore()
const userStore = useUserStore()

// 直接购买模式
const directBuyProduct = ref<any>(null)
const isDirectBuy = computed(() => !!route.query.productId)

// 待付款订单支付模式
const existingOrderId = computed(() => route.query.orderId ? Number(route.query.orderId) : null)
const existingOrder = ref<any>(null)

// 再买一单模式
const isRebuy = computed(() => route.query.rebuy === '1')
const rebuyItems = ref<any[]>([])

const remark = ref('')
const submitting = ref(false)
const paying = ref(false)
const showAddressDialog = ref(false)
const showPayDialog = ref(false)
const selectedAddress = ref<Address | null>(null)
const addresses = ref<Address[]>([])
const createdOrderId = ref<number | null>(null)
const addressLoading = ref(false)

// 弹窗宽度响应式 - 使用 clamp 实现平滑缩放
const dialogWidth = computed(() => {
  const windowWidth = window.innerWidth
  if (windowWidth <= 600) return '95%'
  // 在 600px 到 1200px 之间平滑过渡
  const width = Math.max(600, Math.min(1200, windowWidth))
  const ratio = (width - 600) / 600 // 0 到 1
  const pixelWidth = 570 + ratio * 30 // 570px 到 600px
  return `${pixelWidth}px`
})

// 保存订单金额（提交订单后购物车会被清空，需要保存金额）
const orderPayAmount = ref(0)

// 选中的商品
const selectedItems = computed(() => {
  // 待付款订单支付模式
  if (existingOrderId.value && existingOrder.value) {
    return existingOrder.value.items?.map((item: any) => ({
      id: item.id,
      productId: item.productId,
      productName: item.productName,
      productImage: item.productImage,
      price: item.price,
      quantity: item.quantity,
      farmerName: item.farmerName || '农户店铺',
      farmerId: item.farmerId || 0
    })) || []
  }
  // 直接购买模式
  if (isDirectBuy.value && directBuyProduct.value) {
    return [{
      id: Date.now(),
      productId: directBuyProduct.value.id,
      productName: directBuyProduct.value.name,
      productImage: directBuyProduct.value.mainImage || directBuyProduct.value.image,
      price: directBuyProduct.value.price,
      unit: directBuyProduct.value.unit,
      quantity: Number(route.query.quantity) || 1,
      farmerName: directBuyProduct.value.farmerName || '农户店铺',
      farmerId: directBuyProduct.value.farmerId || 0
    }]
  }
  // 再买一单模式
  if (isRebuy.value && rebuyItems.value.length > 0) {
    return rebuyItems.value
  }
  // 购物车模式
  return cartStore.items.filter(item => item.selected)
})

// 计算金额
const totalAmount = computed(() => {
  return selectedItems.value.reduce((sum: number, item: any) => sum + item.price * item.quantity, 0)
})

const freightAmount = computed(() => {
  // 满99免运费，否则10元运费
  return totalAmount.value >= 99 ? 0 : 10
})

const payAmount = computed(() => {
  return totalAmount.value + freightAmount.value
})

// 获取地址列表
const fetchAddresses = async () => {
  addressLoading.value = true
  try {
    const res: any = await addressApi.getAddressList()
    addresses.value = res || []
    // 默认选中默认地址
    if (addresses.value.length > 0) {
      selectedAddress.value = addresses.value.find(addr => addr.isDefault === 1) || addresses.value[0]
    }
  } catch (error) {
    console.error('获取地址列表失败:', error)
    ElMessage.error('获取地址列表失败')
  } finally {
    addressLoading.value = false
  }
}

const selectAddress = (addr: Address) => {
  selectedAddress.value = addr
  showAddressDialog.value = false
}

const goToAddAddress = () => {
  router.push('/address')
}

// 加载再买一单的商品数据
const fetchRebuyItems = async () => {
  const itemsStr = route.query.items as string
  if (!itemsStr) return
  
  try {
    const items = JSON.parse(decodeURIComponent(itemsStr))
    if (!Array.isArray(items) || items.length === 0) return
    
    // 直接使用订单中的商品信息，不再查询商品详情
    rebuyItems.value = items.map((item: any) => ({
      id: Date.now() + Math.random(),
      productId: item.productId,
      productName: item.productName || '商品',
      productImage: item.productImage || '',
      price: item.price || 0,
      unit: item.unit || '',
      quantity: item.quantity || 1,
      farmerName: item.farmerName || '农户店铺',
      farmerId: item.farmerId || 0
    }))
  } catch (error) {
    console.error('解析再买一单数据失败:', error)
    ElMessage.error('加载商品信息失败')
  }
}

// 提交订单
const handleSubmit = async () => {
  // 待付款订单支付模式：直接显示支付弹窗
  if (existingOrderId.value) {
    createdOrderId.value = existingOrderId.value
    orderPayAmount.value = payAmount.value
    showPayDialog.value = true
    return
  }
  
  if (!selectedAddress.value) {
    ElMessage.warning('请选择收货地址')
    return
  }
  
  if (selectedItems.value.length === 0) {
    ElMessage.warning('请选择要购买的商品')
    return
  }
  
  submitting.value = true
  try {
    const address = selectedAddress.value!
    const params: CreateOrderParams = {
      addressId: address.id!,
      items: selectedItems.value.map((item: any) => ({
        productId: item.productId,
        quantity: item.quantity
      })),
      remark: remark.value,
      receiverName: address.receiverName,
      receiverPhone: address.receiverPhone,
      receiverAddress: `${address.province}${address.city}${address.district}${address.detailAddress}`,
      totalAmount: totalAmount.value,
      freightAmount: freightAmount.value,
      payAmount: payAmount.value
    }
    
    const res: any = await orderApi.createOrder(params)
    createdOrderId.value = res.id
    
    // 保存支付金额（清空购物车前保存）
    orderPayAmount.value = payAmount.value
    
    // 从购物车中移除已购买的商品（仅购物车模式）
    if (!isDirectBuy.value) {
      selectedItems.value.forEach((item: any) => {
        cartStore.removeItem(item.productId)
      })
    }
    
    // 显示支付弹窗
    showPayDialog.value = true
  } catch (error) {
    console.error('创建订单失败:', error)
    ElMessage.error('创建订单失败')
  } finally {
    submitting.value = false
  }
}

// 模拟支付
const handlePay = async () => {
  if (!createdOrderId.value) return
  
  paying.value = true
  try {
    // 调用后端支付接口
    await orderApi.payOrder(createdOrderId.value, 'online')
    ElMessage.success('支付成功')
    showPayDialog.value = false
    // 跳转到订单列表页的待发货标签
    router.push('/orders?status=1')
  } catch (error) {
    console.error('支付失败:', error)
    ElMessage.error('支付失败')
  } finally {
    paying.value = false
  }
}

// 获取直接购买的商品信息
const fetchDirectBuyProduct = async () => {
  const productId = route.query.productId as string
  if (!productId) return

  try {
    const res: any = await productApi.getProductDetail(Number(productId))
    directBuyProduct.value = res
  } catch (error) {
    console.error('获取商品信息失败:', error)
    ElMessage.error('获取商品信息失败')
  }
}

// 获取待付款订单信息
const fetchExistingOrder = async () => {
  if (!existingOrderId.value) return

  try {
    const res: any = await orderApi.getOrderDetail(existingOrderId.value)
    existingOrder.value = res
    // 设置订单金额
    orderPayAmount.value = res.payAmount || 0
  } catch (error) {
    console.error('获取订单信息失败:', error)
    ElMessage.error('获取订单信息失败')
  }
}

onMounted(async () => {
  await fetchAddresses()

  // 待付款订单支付模式
  if (existingOrderId.value) {
    await fetchExistingOrder()
  }

  // 直接购买模式
  if (isDirectBuy.value) {
    await fetchDirectBuyProduct()
  }

  // 再买一单模式
  if (isRebuy.value) {
    await fetchRebuyItems()
  }

  // 如果没有选中商品，返回
  if (selectedItems.value.length === 0) {
    ElMessage.warning('请先选择商品')
    router.back()
  }
})
</script>

<style scoped>
.order-create-view {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.order-container {
  background: var(--apple-card-bg);
  border-radius: var(--apple-radius-lg);
  padding: 32px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--apple-dark-gray);
  margin-bottom: 24px;
}

.section {
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--apple-light-gray);
}

.section:last-child {
  border-bottom: none;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--apple-dark-gray);
  margin-bottom: 16px;
}

.address-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: var(--apple-bg);
  border-radius: 10px;
}

.address-info .name {
  font-weight: 600;
  margin-right: 12px;
}

.address-info .phone {
  color: var(--apple-gray);
  margin-right: 8px;
}

.address-info .detail {
  margin-top: 8px;
  color: var(--apple-dark-gray);
}

.address-loading {
  padding: 16px;
}

.no-address {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32px;
  background: var(--apple-bg);
  border-radius: 10px;
}

.no-address-tip {
  color: var(--apple-gray);
  margin-bottom: 16px;
}

.product-list {
  display: flex;
  flex-direction: column;
  gap: clamp(12px, 2vw, 16px);
}

.product-item {
  display: grid;
  grid-template-columns: clamp(60px, 8vw, 80px) 2fr clamp(80px, 10vw, 120px) clamp(60px, 8vw, 80px) clamp(80px, 10vw, 120px);
  align-items: center;
  gap: clamp(8px, 2vw, 16px);
  padding: clamp(12px, 2vw, 16px);
  background: var(--apple-bg);
  border-radius: clamp(8px, 1.5vw, 10px);
  transition: gap 0.3s ease, padding 0.3s ease, border-radius 0.3s ease;
}

.product-image {
  width: clamp(60px, 8vw, 80px);
  height: clamp(60px, 8vw, 80px);
  object-fit: cover;
  border-radius: clamp(6px, 1vw, 8px);
  transition: width 0.3s ease, height 0.3s ease, border-radius 0.3s ease;
}

.product-name {
  font-size: clamp(13px, 1.2vw, 14px);
  font-weight: 500;
  color: var(--apple-dark-gray);
  transition: font-size 0.3s ease;
}

.product-farmer {
  font-size: clamp(11px, 1vw, 12px);
  color: var(--apple-gray);
  margin-top: 4px;
  transition: font-size 0.3s ease;
}

.product-price,
.product-quantity {
  color: var(--apple-dark-gray);
  font-size: clamp(13px, 1.2vw, 14px);
  transition: font-size 0.3s ease;
  padding: 0 clamp(4px, 1vw, 8px);
}

.product-unit {
  font-size: 12px;
  color: #909399;
  margin-left: 4px;
}

.product-total {
  font-weight: 600;
  color: var(--apple-blue);
  text-align: right;
  font-size: clamp(14px, 1.3vw, 16px);
  transition: font-size 0.3s ease;
}

/* 小屏幕优化 */
@media (max-width: 768px) {
  .product-item {
    grid-template-columns: clamp(50px, 12vw, 60px) 1fr auto;
    gap: clamp(8px, 2vw, 12px);
  }

  .product-image {
    width: clamp(50px, 12vw, 60px);
    height: clamp(50px, 12vw, 60px);
  }

  .product-info {
    min-width: 0;
  }

  .product-name {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .product-price {
    display: none;
  }

  .product-quantity {
    grid-column: 3;
    text-align: right;
  }

  .product-total {
    grid-column: 3;
    text-align: right;
    font-size: clamp(13px, 1.2vw, 14px);
  }
}

.summary-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: var(--apple-gray);
}

.summary-row.total {
  font-size: 16px;
  font-weight: 600;
  color: var(--apple-dark-gray);
  padding-top: 12px;
  border-top: 1px solid var(--apple-light-gray);
}

.total-price {
  font-size: 24px;
  color: var(--apple-blue);
}

.submit-section {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  padding-top: 24px;
  border-top: 1px solid var(--apple-light-gray);
}

.address-list {
  display: flex;
  flex-direction: column;
  gap: clamp(8px, 2vw, 12px);
}

.address-option {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: clamp(12px, 3vw, 16px);
  border: 2px solid var(--apple-light-gray);
  border-radius: clamp(8px, 2vw, 10px);
  cursor: pointer;
  transition: all 0.3s ease, padding 0.3s ease, border-radius 0.3s ease;
}

.address-option:hover,
.address-option.active {
  border-color: var(--apple-blue);
}

.address-option-info {
  flex: 1;
  min-width: 0;
}

.address-option-info .name {
  font-weight: 600;
  margin-right: clamp(8px, 2vw, 12px);
  font-size: clamp(13px, 1.2vw, 14px);
  transition: font-size 0.3s ease;
}

.address-option-info .phone {
  color: var(--apple-gray);
  margin-right: clamp(4px, 1vw, 8px);
  font-size: clamp(12px, 1.1vw, 14px);
  transition: font-size 0.3s ease;
}

.address-option-info .detail {
  margin-top: clamp(6px, 1.5vw, 8px);
  color: var(--apple-dark-gray);
  font-size: clamp(12px, 1.1vw, 14px);
  line-height: 1.5;
  word-break: break-all;
  transition: font-size 0.3s ease, margin-top 0.3s ease;
}

.address-option :deep(.el-tag) {
  font-size: clamp(10px, 1vw, 12px);
  transition: font-size 0.3s ease;
}

.check-icon {
  font-size: clamp(20px, 2vw, 24px);
  color: var(--apple-blue);
  flex-shrink: 0;
  margin-left: 8px;
  transition: font-size 0.3s ease;
}

.empty-address {
  padding: 40px 0;
}

.pay-content {
  text-align: center;
  padding: 20px 0;
}

.pay-amount {
  font-size: 18px;
  margin-bottom: 12px;
}

.pay-amount strong {
  font-size: 28px;
  color: var(--apple-blue);
}

.pay-tip {
  color: var(--apple-gray);
  font-size: 14px;
}

/* 地址弹窗响应式样式 - 使用 clamp 实现平滑等比缩放 */
:deep(.address-dialog) {
  --dialog-scale: clamp(0.85, 0.9 + 0.1 * (100vw - 600px) / 600, 1);
}

:deep(.address-dialog .el-dialog) {
  border-radius: clamp(8px, 1.5vw, 12px);
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

:deep(.address-dialog .el-dialog__header) {
  padding: clamp(12px, 2.5vw, 20px);
  padding-bottom: clamp(10px, 2vw, 16px);
  margin-right: 0;
}

:deep(.address-dialog .el-dialog__title) {
  font-size: clamp(14px, 1.8vw, 18px);
  transition: font-size 0.3s ease;
}

:deep(.address-dialog .el-dialog__body) {
  padding: clamp(12px, 2.5vw, 20px);
  transition: padding 0.3s ease;
}

:deep(.address-dialog .el-dialog__headerbtn) {
  top: clamp(12px, 2.5vw, 20px);
  right: clamp(12px, 2.5vw, 20px);
  width: clamp(28px, 3vw, 32px);
  height: clamp(28px, 3vw, 32px);
}

:deep(.address-dialog .el-dialog__close) {
  font-size: clamp(16px, 2vw, 20px);
  transition: font-size 0.3s ease;
}

/* 超小屏幕优化 */
@media (max-width: 600px) {
  :deep(.address-dialog) {
    width: 95% !important;
  }

  :deep(.address-dialog .el-dialog__header) {
    padding: 12px 16px;
  }

  :deep(.address-dialog .el-dialog__body) {
    padding: 12px 16px;
  }

  .address-option {
    flex-direction: column;
    gap: 8px;
  }

  .check-icon {
    align-self: flex-end;
    margin-left: 0;
  }
}
</style>
