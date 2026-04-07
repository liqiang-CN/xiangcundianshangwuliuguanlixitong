<template>
  <el-dialog
    v-model="dialogVisible"
    title="选择订单"
    width="600px"
    :close-on-click-modal="false"
  >
    <div class="order-list">
      <div
        v-for="order in orders"
        :key="order.id"
        class="order-item"
        :class="{ selected: selectedId === order.id }"
        @click="selectOrder(order)"
      >
        <img :src="getImageUrl(order.productImage)" class="order-image" />
        <div class="order-info">
          <div class="order-name">{{ order.productName }}</div>
          <div class="order-no">订单号：{{ order.orderNo }}</div>
          <div class="order-meta">
            <span class="order-price">¥{{ order.price }}/{{ order.unit }}</span>
            <span class="order-status" :class="`status-${order.status}`">{{ order.statusText }}</span>
          </div>
        </div>
      </div>
      <div v-if="orders.length === 0" class="empty-state">
        <el-empty description="暂无订单" />
      </div>
    </div>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="confirm" :disabled="!selectedOrder">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { getImageUrl } from '@/utils/image'

const props = defineProps<{
  modelValue: boolean
  orders: any[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'select', order: any): void
}>()

const dialogVisible = ref(props.modelValue)
const selectedId = ref<number | null>(null)
const selectedOrder = ref<any>(null)

watch(() => props.modelValue, (val) => {
  dialogVisible.value = val
  if (val) {
    selectedId.value = null
    selectedOrder.value = null
  }
})

watch(dialogVisible, (val) => {
  emit('update:modelValue', val)
})

const selectOrder = (order: any) => {
  selectedId.value = order.id
  selectedOrder.value = order
}

const confirm = () => {
  if (selectedOrder.value) {
    emit('select', selectedOrder.value)
    dialogVisible.value = false
  }
}
</script>

<style scoped>
.order-list {
  max-height: 400px;
  overflow-y: auto;
}

.order-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.order-item:hover {
  border-color: #007aff;
  background: #f5f9ff;
}

.order-item.selected {
  border-color: #007aff;
  background: #e6f2ff;
}

.order-image {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 4px;
}

.order-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.order-name {
  font-weight: 500;
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-no {
  font-size: 12px;
  color: #999;
}

.order-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-price {
  font-weight: 600;
  color: #ff6b6b;
  font-size: 14px;
}

.order-status {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
}

.status-0 { background: #fff3cd; color: #856404; }
.status-1 { background: #d1ecf1; color: #0c5460; }
.status-2 { background: #d4edda; color: #155724; }
.status-3 { background: #f8d7da; color: #721c24; }
.status-4 { background: #e2e3e5; color: #383d41; }

.empty-state {
  padding: 40px 0;
}
</style>
