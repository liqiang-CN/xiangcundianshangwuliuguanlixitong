<template>
  <el-dialog
    v-model="dialogVisible"
    title="选择商品"
    width="600px"
    :close-on-click-modal="false"
  >
    <div class="product-list">
      <div
        v-for="product in products"
        :key="product.id"
        class="product-item"
        :class="{ selected: selectedId === product.id }"
        @click="selectProduct(product)"
      >
        <img :src="getImageUrl(product.mainImage || product.image)" class="product-image" />
        <div class="product-info">
          <div class="product-name">{{ product.name }}</div>
          <div class="product-desc">{{ product.description }}</div>
          <div class="product-meta">
            <span class="product-price">¥{{ product.price }}/{{ product.unit }}</span>
            <span class="product-sales">已售{{ product.sales }}</span>
          </div>
        </div>
      </div>
    </div>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="confirm" :disabled="!selectedProduct">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { getImageUrl } from '@/utils/image'

const props = defineProps<{
  modelValue: boolean
  products: any[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'select', product: any): void
}>()

const dialogVisible = ref(props.modelValue)
const selectedId = ref<number | null>(null)
const selectedProduct = ref<any>(null)

watch(() => props.modelValue, (val) => {
  dialogVisible.value = val
  if (val) {
    selectedId.value = null
    selectedProduct.value = null
  }
})

watch(dialogVisible, (val) => {
  emit('update:modelValue', val)
})

const selectProduct = (product: any) => {
  selectedId.value = product.id
  selectedProduct.value = product
}

const confirm = () => {
  if (selectedProduct.value) {
    emit('select', selectedProduct.value)
    dialogVisible.value = false
  }
}
</script>

<style scoped>
.product-list {
  max-height: 400px;
  overflow-y: auto;
}

.product-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.product-item:hover {
  border-color: #007aff;
  background: #f5f9ff;
}

.product-item.selected {
  border-color: #007aff;
  background: #e6f2ff;
}

.product-image {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 4px;
}

.product-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.product-name {
  font-weight: 500;
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-desc {
  font-size: 12px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.product-price {
  font-weight: 600;
  color: #ff6b6b;
  font-size: 14px;
}

.product-sales {
  font-size: 12px;
  color: #999;
}
</style>
