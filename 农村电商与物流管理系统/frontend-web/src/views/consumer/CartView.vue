<template>
  <div class="cart-view">
    <div v-if="cartStore.items.length === 0" class="cart-empty-wrap">
      <el-empty description="购物车是空的">
        <el-button type="primary" class="cart-btn-primary" @click="$router.push('/consumer/products')">
          去购物
        </el-button>
      </el-empty>
    </div>

    <template v-else>
      <div class="cart-page-title">
        <h1 class="cart-title">购物车</h1>
        <p class="cart-subtitle">结算前请确认已选商品与数量。</p>
      </div>

      <div class="cart-grid">
        <section class="cart-items-section">
          <div class="cart-select-row">
            <label class="cart-select-all">
              <el-checkbox v-model="selectAll" class="cart-checkbox" @change="handleSelectAll" />
              <span class="cart-label-upper">全选</span>
            </label>
            <div class="cart-select-row-end">
              <span class="cart-item-count">{{ cartStore.items.length }} 件商品</span>
              <button type="button" class="cart-link-muted" @click="handleClear">清空购物车</button>
            </div>
          </div>

          <div class="cart-item-list">
            <div v-for="item in cartStore.items" :key="item.id" class="cart-item-card">
              <el-checkbox v-model="item.selected" class="cart-checkbox cart-item-check" />
              <div class="cart-item-thumb">
                <img :src="item.productImage" :alt="item.productName" />
              </div>
              <div class="cart-item-body">
                <h3 class="cart-item-name">{{ item.productName }}</h3>
                <p v-if="item.farmerName && item.farmerName !== '未知农户'" class="cart-item-desc">{{ item.farmerName }}</p>
                <p class="cart-item-unit-line">
                  单价 ¥{{ item.price.toFixed(2) }}<span v-if="item.unit" class="cart-item-unit"> / {{ item.unit }}</span>
                </p>
                <div class="cart-item-qty-row">
                  <div class="cart-qty-pill">
                    <el-input-number
                      v-model="item.quantity"
                      class="cart-qty-input"
                      :min="1"
                      :max="item.stock"
                      @change="(val: number) => handleQuantityChange(item.productId, val)"
                    />
                  </div>
                </div>
              </div>
              <div class="cart-item-aside">
                <button
                  type="button"
                  class="cart-remove-btn"
                  aria-label="删除"
                  @click="handleRemove(item.productId)"
                >
                  <el-icon><Close /></el-icon>
                </button>
                <div class="cart-item-price">
                  ¥{{ (item.price * item.quantity).toFixed(2) }}
                </div>
              </div>
            </div>
          </div>
        </section>

        <aside class="cart-summary-aside">
          <div class="cart-summary-card">
            <h2 class="cart-summary-title">订单汇总</h2>
            <div class="cart-summary-lines">
              <div class="cart-summary-line">
                <span class="cart-summary-label">商品小计（已选 {{ cartStore.selectedCount }} 件）</span>
                <span class="cart-summary-val">¥{{ cartStore.totalPrice.toFixed(2) }}</span>
              </div>
              <div class="cart-summary-line">
                <span class="cart-summary-label">预估运费</span>
                <span class="cart-summary-val cart-summary-accent">免运费</span>
              </div>
              <div class="cart-summary-line">
                <span class="cart-summary-label">预估税费</span>
                <span class="cart-summary-val">¥0.00</span>
              </div>
            </div>
            <div class="cart-summary-total-block">
              <span class="cart-total-label">应付总额</span>
              <span class="cart-total-amount">¥{{ cartStore.totalPrice.toFixed(2) }}</span>
            </div>
            <el-button
              type="primary"
              class="cart-checkout-btn"
              :disabled="cartStore.selectedCount === 0"
              @click="handleCheckout"
            >
              去结算
            </el-button>
            <p class="cart-secure-note">安全支付 · 平台担保交易</p>
          </div>
          <div class="cart-shipping-hint">
            <el-icon class="cart-shipping-icon"><Van /></el-icon>
            <p>本单可享普通快递包邮（以结算页为准）。</p>
          </div>
        </aside>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Close, Van } from '@element-plus/icons-vue'
import { useCartStore } from '@/stores/cart'

const router = useRouter()
const cartStore = useCartStore()

const selectAll = computed({
  get: () => cartStore.items.length > 0 && cartStore.items.every((item) => item.selected),
  set: (val) => cartStore.selectAll(val)
})

const handleSelectAll = (val: boolean) => {
  cartStore.selectAll(val)
}

const handleQuantityChange = (productId: number, quantity: number) => {
  cartStore.updateQuantity(productId, quantity)
}

const handleRemove = (productId: number) => {
  ElMessageBox.confirm('确定要删除该商品吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    cartStore.removeItem(productId)
    ElMessage.success('删除成功')
  })
}

const handleClear = () => {
  if (cartStore.items.length === 0) return
  ElMessageBox.confirm('确定要清空购物车吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    cartStore.clearCart()
    ElMessage.success('购物车已清空')
  })
}

const handleCheckout = () => {
  router.push('/orders/create')
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;600;700;800&family=Inter:wght@400;500;600&display=swap');

.cart-view {
  --cart-primary: #007aff;
  --cart-primary-dim: #0062cc;
  --cart-on-surface: #2d3335;
  --cart-secondary: #576068;
  --cart-outline-variant: #adb3b5;
  --cart-surface: #f8f9fa;
  --cart-surface-low: #f1f4f5;
  --cart-surface-high: #e5e9eb;
  --cart-card: #ffffff;
  --cart-error: #a83836;
  --cart-primary-container: #cce5ff;

  width: 100%;
  max-width: none;
  margin: 0;
  padding: 2.5rem clamp(1rem, 3vw, 2.5rem) 4rem;
  font-family: 'Inter', system-ui, sans-serif;
  color: var(--cart-on-surface);
  background: var(--cart-surface);
  box-sizing: border-box;
}

.cart-empty-wrap {
  width: 100%;
  padding: 4rem clamp(0.5rem, 2vw, 1rem);
  background: var(--cart-card);
  border: 1px solid rgba(173, 179, 181, 0.2);
  border-radius: 0;
  box-sizing: border-box;
}

.cart-btn-primary {
  border-radius: 9999px !important;
  padding: 0.75rem 1.5rem !important;
  font-weight: 600 !important;
  background: linear-gradient(90deg, var(--cart-primary), var(--cart-primary-dim)) !important;
  border: none !important;
}

.cart-page-title {
  margin-bottom: 3rem;
}

.cart-title {
  font-family: 'Manrope', 'Inter', sans-serif;
  font-size: clamp(2rem, 4vw, 3rem);
  font-weight: 800;
  letter-spacing: -0.03em;
  color: var(--cart-on-surface);
  margin: 0 0 0.5rem;
}

.cart-subtitle {
  margin: 0;
  font-size: 0.95rem;
  color: var(--cart-secondary);
}

.cart-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 3rem;
  align-items: flex-start;
}

@media (min-width: 1024px) {
  .cart-grid {
    grid-template-columns: minmax(0, 2fr) minmax(280px, 1fr);
  }
}

.cart-select-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.75rem 1rem;
  margin-bottom: 2rem;
  padding: 0 0.25rem;
}

.cart-select-all {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  cursor: pointer;
}

.cart-select-row-end {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.cart-label-upper {
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--cart-secondary);
}

.cart-select-all:hover .cart-label-upper {
  color: var(--cart-primary);
}

.cart-link-muted {
  background: none;
  border: none;
  padding: 0.35rem 0.65rem;
  font-size: 0.8125rem;
  color: var(--cart-secondary);
  cursor: pointer;
  border-radius: 0.375rem;
  transition:
    color 0.15s ease,
    background 0.15s ease;
}

.cart-link-muted:hover {
  color: var(--cart-error);
  background: rgba(168, 56, 54, 0.06);
}

.cart-item-count {
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.15em;
  text-transform: uppercase;
  color: var(--cart-secondary);
}

.cart-item-list {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.cart-item-card {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 1.25rem;
  padding: 1.5rem;
  background: var(--cart-card);
  border-radius: 0.75rem;
  box-shadow: 0 1px 2px rgba(45, 51, 53, 0.06);
  transition: box-shadow 0.2s ease;
}

.cart-item-card:hover {
  box-shadow: 0 4px 12px rgba(45, 51, 53, 0.08);
}

@media (min-width: 768px) {
  .cart-item-card {
    flex-direction: row;
    align-items: center;
    gap: 1.5rem;
  }
}

.cart-item-check {
  flex-shrink: 0;
  align-self: flex-start;
}

@media (min-width: 768px) {
  .cart-item-check {
    align-self: center;
  }
}

.cart-item-thumb {
  width: 100%;
  max-width: 8rem;
  height: 8rem;
  border-radius: 0.5rem;
  overflow: hidden;
  background: var(--cart-surface-low);
  flex-shrink: 0;
}

.cart-item-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cart-item-body {
  flex: 1;
  min-width: 0;
}

.cart-item-name {
  font-family: 'Manrope', 'Inter', sans-serif;
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--cart-on-surface);
  margin: 0 0 0.25rem;
}

.cart-item-desc {
  font-size: 0.875rem;
  color: var(--cart-secondary);
  margin: 0 0 0.35rem;
}

.cart-item-unit-line {
  font-size: 0.875rem;
  color: var(--cart-secondary);
  margin: 0 0 1rem;
}

.cart-item-unit {
  color: #909399;
}

.cart-item-qty-row {
  display: flex;
  align-items: center;
  gap: 1rem;
}

/* EP 加减为左右绝对定位，外层用圆角 + overflow 包一层，避免与圆角/透明背景冲突导致错位 */
.cart-qty-pill {
  display: inline-flex;
  align-items: center;
  vertical-align: middle;
  border-radius: 0.5rem;
  overflow: hidden;
  border: 1px solid rgba(45, 51, 53, 0.1);
  background: #fff;
  box-sizing: border-box;
}

.cart-qty-pill :deep(.cart-qty-input.el-input-number) {
  width: 132px;
  line-height: 30px;
}

.cart-qty-pill :deep(.cart-qty-input .el-input__wrapper) {
  background: linear-gradient(180deg, #fff 0%, #fafbfc 100%) !important;
  box-shadow: none !important;
  border: none !important;
  border-radius: 0;
  padding-left: 42px !important;
  padding-right: 42px !important;
}

.cart-qty-pill :deep(.cart-qty-input .el-input__wrapper.is-focus) {
  box-shadow: none !important;
}

.cart-qty-pill :deep(.cart-qty-input .el-input__inner) {
  text-align: center;
  font-family: 'Manrope', 'Inter', system-ui, sans-serif;
  font-weight: 600;
  font-size: 15px;
  font-variant-numeric: tabular-nums;
  color: var(--cart-on-surface);
}

.cart-qty-pill :deep(.cart-qty-input .el-input-number__decrease),
.cart-qty-pill :deep(.cart-qty-input .el-input-number__increase) {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  top: 0;
  bottom: 0;
  height: 100%;
  margin: 0;
  background: rgba(0, 122, 255, 0.06);
  color: var(--cart-primary);
  border: none !important;
  border-radius: 0 !important;
  box-shadow: none !important;
  transition:
    background 0.15s ease,
    color 0.15s ease;
}

.cart-qty-pill :deep(.cart-qty-input .el-input-number__decrease) {
  left: 0;
  border-right: 1px solid rgba(45, 51, 53, 0.08) !important;
}

.cart-qty-pill :deep(.cart-qty-input .el-input-number__increase) {
  right: 0;
  border-left: 1px solid rgba(45, 51, 53, 0.08) !important;
}

.cart-qty-pill :deep(.cart-qty-input .el-input-number__decrease:hover),
.cart-qty-pill :deep(.cart-qty-input .el-input-number__increase:hover) {
  background: rgba(0, 122, 255, 0.14);
  color: var(--cart-primary-dim);
}

.cart-qty-pill :deep(.cart-qty-input .el-input-number__decrease.is-disabled),
.cart-qty-pill :deep(.cart-qty-input .el-input-number__increase.is-disabled) {
  background: var(--cart-surface-low);
  color: var(--cart-outline-variant);
  cursor: not-allowed;
}

.cart-item-aside {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  gap: 0.5rem;
}

@media (min-width: 768px) {
  .cart-item-aside {
    flex-direction: column;
    align-items: flex-end;
    align-self: stretch;
    justify-content: space-between;
    min-width: 5rem;
  }
}

.cart-remove-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.5rem;
  border: none;
  background: none;
  color: var(--cart-secondary);
  cursor: pointer;
  border-radius: 0.25rem;
  transition: color 0.2s;
}

.cart-remove-btn:hover {
  color: var(--cart-error);
}

.cart-item-price {
  font-family: 'Manrope', 'Inter', sans-serif;
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--cart-primary);
}

.cart-summary-aside {
  position: sticky;
  top: 6rem;
}

.cart-summary-card {
  background: var(--cart-card);
  border-radius: 0.75rem;
  padding: 2rem;
  border: 1px solid rgba(173, 179, 181, 0.15);
  box-shadow: 0 1px 3px rgba(45, 51, 53, 0.06);
}

.cart-summary-title {
  font-family: 'Manrope', 'Inter', sans-serif;
  font-size: 1.25rem;
  font-weight: 700;
  margin: 0 0 1.5rem;
  color: var(--cart-on-surface);
}

.cart-summary-lines {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-bottom: 2rem;
}

.cart-summary-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.cart-summary-label {
  font-size: 0.9375rem;
  color: var(--cart-secondary);
}

.cart-summary-val {
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--cart-on-surface);
}

.cart-summary-accent {
  color: var(--cart-primary);
}

.cart-summary-total-block {
  padding-top: 1.5rem;
  border-top: 1px solid var(--cart-surface-low);
  margin-bottom: 2rem;
}

.cart-total-label {
  display: block;
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--cart-secondary);
  margin-bottom: 0.25rem;
}

.cart-total-amount {
  font-family: 'Manrope', 'Inter', sans-serif;
  font-size: 2rem;
  font-weight: 800;
  color: var(--cart-primary);
}

.cart-checkout-btn {
  width: 100% !important;
  height: auto !important;
  padding: 1rem !important;
  border-radius: 9999px !important;
  font-size: 1rem !important;
  font-weight: 700 !important;
  letter-spacing: 0.02em !important;
  border: none !important;
  background: linear-gradient(90deg, var(--cart-primary), var(--cart-primary-dim)) !important;
  transition:
    box-shadow 0.2s,
    transform 0.15s,
    opacity 0.2s;
}

.cart-checkout-btn:hover:not(:disabled) {
  box-shadow: 0 8px 24px rgba(0, 122, 255, 0.35);
  opacity: 0.98;
  transform: scale(1.02);
}

.cart-checkout-btn:active:not(:disabled) {
  transform: scale(0.98);
}

.cart-secure-note {
  margin: 1.5rem 0 0;
  text-align: center;
  font-size: 0.65rem;
  font-weight: 600;
  letter-spacing: 0.15em;
  text-transform: uppercase;
  color: #767c7e;
}

.cart-shipping-hint {
  margin-top: 1.5rem;
  padding: 1rem;
  border-radius: 0.75rem;
  background: rgba(0, 122, 255, 0.12);
  display: flex;
  align-items: flex-start;
  gap: 1rem;
}

.cart-shipping-hint p {
  margin: 0;
  font-size: 0.75rem;
  line-height: 1.5;
  color: #003d82;
}

.cart-shipping-icon {
  font-size: 1.5rem;
  color: var(--cart-primary);
  flex-shrink: 0;
}

/* Element Plus checkbox 主色贴近参考稿 */
.cart-checkbox :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: var(--cart-primary);
  border-color: var(--cart-primary);
}

.cart-checkbox :deep(.el-checkbox__input.is-checked + .el-checkbox__label) {
  color: var(--cart-on-surface);
}

.cart-checkbox :deep(.el-checkbox__inner:hover) {
  border-color: var(--cart-primary);
}
</style>
