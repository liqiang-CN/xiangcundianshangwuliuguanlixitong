<template>
  <div class="print-view">
    <h2 class="page-title">打印面单</h2>

    <div class="print-container">
      <!-- 左侧：待打印列表 -->
      <div class="waybill-list">
        <div class="list-header">
          <el-checkbox v-model="selectAll" @change="handleSelectAll">全选</el-checkbox>
          <span class="selected-count">已选 {{ selectedWaybills.length }} 个</span>
        </div>
        <div class="list-content" v-loading="loading">
          <div
            v-for="waybill in pendingWaybills"
            :key="waybill.id"
            class="waybill-item"
            :class="{ selected: selectedWaybills.includes(waybill.id) }"
            @click="toggleSelect(waybill.id)"
          >
            <el-checkbox v-model="waybill.selected" @click.stop />
            <div class="waybill-info">
              <p class="waybill-no">{{ waybill.trackingNo }}</p>
              <p class="receiver">{{ waybill.receiverName }} {{ waybill.receiverPhone }}</p>
              <p class="address">{{ waybill.receiverAddress }}</p>
            </div>
          </div>
          <el-empty v-if="pendingWaybills.length === 0" description="暂无待打印面单" />
        </div>
      </div>

      <!-- 右侧：打印预览 -->
      <div class="print-preview">
        <div class="preview-header">
          <h3>打印预览</h3>
          <el-button type="primary" :disabled="selectedWaybills.length === 0" @click="handlePrint">
            <el-icon><Printer /></el-icon>
            批量打印
          </el-button>
        </div>
        <div class="preview-content">
          <div v-for="waybill in selectedWaybillList" :key="waybill.id" class="waybill-preview">
            <div class="preview-card">
              <div class="preview-header-bar">
                <span class="company">{{ getLogisticsCompanyName(waybill.logisticsCompany) }}</span>
                <span class="waybill-no">{{ waybill.trackingNo }}</span>
              </div>
              <div class="preview-body">
                <div class="address-section">
                  <div class="sender">
                    <span class="label">寄</span>
                    <div class="info">
                      <p class="name">{{ waybill.senderName }} {{ waybill.senderPhone }}</p>
                      <p class="address">{{ waybill.senderAddress }}</p>
                    </div>
                  </div>
                  <div class="receiver">
                    <span class="label">收</span>
                    <div class="info">
                      <p class="name">{{ waybill.receiverName }} {{ waybill.receiverPhone }}</p>
                      <p class="address">{{ waybill.receiverAddress }}</p>
                    </div>
                  </div>
                </div>
                <div class="goods-section">
                  <p class="goods-name">{{ waybill.goodsName }}</p>
                  <p class="goods-weight">重量：{{ waybill.goodsWeight }}kg</p>
                </div>
                <div class="barcode-section">
                  <div class="barcode"></div>
                  <p class="barcode-text">{{ waybill.trackingNo }}</p>
                </div>
              </div>
            </div>
          </div>
          <el-empty v-if="selectedWaybillList.length === 0" description="请选择要打印的面单" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Printer } from '@element-plus/icons-vue'
import request from '@/api/request'

const loading = ref(false)
const selectAll = ref(false)

const pendingWaybills = ref<any[]>([])

// 获取待打印的运单列表（已揽件/运输中的）
const fetchWaybills = async () => {
  loading.value = true
  try {
    // 获取运输中列表（已揽件）
    const deliveringRes: any = await request.get('/logistics/delivering', {
      params: { page: 1, size: 100 }
    })

    const allWaybills = [...(deliveringRes || [])]
    
    // 处理数据，补充订单信息
    const processedList = await Promise.all(allWaybills.map(async (item: any) => {
      try {
        const orderRes: any = await request.get(`/logistics/order/${item.orderId}/detail`)
        if (orderRes) {
          return {
            ...item,
            selected: false,
            receiverName: orderRes.receiverName,
            receiverPhone: orderRes.receiverPhone,
            receiverAddress: orderRes.receiverAddress,
            senderName: orderRes.senderName,
            senderPhone: orderRes.senderPhone,
            senderAddress: orderRes.senderAddress,
            goodsName: orderRes.items?.[0]?.productName || '商品',
            goodsWeight: orderRes.items?.[0]?.quantity || 1,
            logisticsCompany: orderRes.logisticsCompany
          }
        }
      } catch (e) {
        console.error('获取订单详情失败:', e)
      }
      return { ...item, selected: false }
    }))
    
    pendingWaybills.value = processedList
  } catch (error) {
    console.error('获取运单列表失败:', error)
    ElMessage.error('获取运单列表失败')
  } finally {
    loading.value = false
  }
}

const selectedWaybills = computed(() => {
  return pendingWaybills.value.filter(w => w.selected).map(w => w.id)
})

const selectedWaybillList = computed(() => {
  return pendingWaybills.value.filter(w => w.selected)
})

const handleSelectAll = (val: boolean) => {
  pendingWaybills.value.forEach(w => w.selected = val)
}

const toggleSelect = (id: number) => {
  const waybill = pendingWaybills.value.find(w => w.id === id)
  if (waybill) {
    waybill.selected = !waybill.selected
  }
}

const getLogisticsCompanyName = (code: string): string => {
  const companyMap: Record<string, string> = {
    'sf': '顺丰速运',
    'zt': '中通快递',
    'yt': '圆通快递',
    'yd': '韵达快递'
  }
  return companyMap[code] || code
}

const handlePrint = () => {
  ElMessage.success(`开始打印 ${selectedWaybills.value.length} 个面单`)
  // 实际打印逻辑
  window.print()
}

onMounted(() => {
  fetchWaybills()
})
</script>

<style scoped>
.print-view {
  padding-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--apple-dark-gray);
  margin-bottom: 24px;
}

.print-container {
  display: flex;
  gap: 24px;
  height: calc(100vh - 200px);
}

.waybill-list {
  width: 360px;
  background: var(--apple-card-bg);
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--apple-light-gray);
}

.selected-count {
  font-size: 14px;
  color: var(--apple-gray);
}

.list-content {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.waybill-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  margin-bottom: 8px;
}

.waybill-item:hover {
  background: var(--apple-bg);
}

.waybill-item.selected {
  background: rgba(0, 122, 255, 0.1);
}

.waybill-info .waybill-no {
  font-weight: 600;
  color: var(--apple-dark-gray);
  margin-bottom: 4px;
}

.waybill-info .receiver {
  font-size: 13px;
  color: var(--apple-dark-gray);
  margin-bottom: 4px;
}

.waybill-info .address {
  font-size: 12px;
  color: var(--apple-gray);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.print-preview {
  flex: 1;
  background: var(--apple-card-bg);
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--apple-light-gray);
}

.preview-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--apple-dark-gray);
}

.preview-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: var(--apple-bg);
}

.waybill-preview {
  margin-bottom: 20px;
}

.preview-card {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.preview-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #000;
  color: white;
}

.preview-header-bar .company {
  font-weight: 600;
}

.preview-header-bar .waybill-no {
  font-family: monospace;
  font-size: 14px;
}

.preview-body {
  padding: 16px;
}

.address-section {
  margin-bottom: 16px;
}

.address-section .sender,
.address-section .receiver {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.address-section .label {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--apple-blue);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  flex-shrink: 0;
}

.address-section .receiver .label {
  background: var(--apple-green);
}

.address-section .info {
  flex: 1;
}

.address-section .name {
  font-weight: 500;
  color: var(--apple-dark-gray);
  margin-bottom: 4px;
}

.address-section .address {
  font-size: 13px;
  color: var(--apple-gray);
}

.goods-section {
  padding: 12px;
  background: var(--apple-bg);
  border-radius: 8px;
  margin-bottom: 16px;
}

.goods-section .goods-name {
  font-weight: 500;
  color: var(--apple-dark-gray);
  margin-bottom: 4px;
}

.goods-section .goods-weight {
  font-size: 13px;
  color: var(--apple-gray);
}

.barcode-section {
  text-align: center;
  padding: 16px;
  border-top: 1px dashed var(--apple-border);
}

.barcode {
  height: 60px;
  background: repeating-linear-gradient(
    90deg,
    #000 0px,
    #000 2px,
    transparent 2px,
    transparent 4px
  );
  margin-bottom: 8px;
}

.barcode-text {
  font-family: monospace;
  font-size: 14px;
  color: var(--apple-dark-gray);
}

@media print {
  .print-view .page-title,
  .print-view .waybill-list,
  .print-view .preview-header {
    display: none;
  }
  
  .print-view .print-preview {
    width: 100%;
    height: auto;
  }
  
  .print-view .preview-content {
    background: white;
    padding: 0;
  }
  
  .waybill-preview {
    page-break-after: always;
  }
  
  .waybill-preview:last-child {
    page-break-after: auto;
  }
}
</style>
