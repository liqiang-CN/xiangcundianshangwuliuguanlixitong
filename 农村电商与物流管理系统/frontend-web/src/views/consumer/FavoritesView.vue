<template>
  <div class="favorites-view">
    <div class="favorites-container">
      <h2 class="page-title">我的收藏</h2>

      <div class="search-box">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索收藏商品"
          clearable
          size="large"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
          <template #append>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
          </template>
        </el-input>
      </div>

      <div v-if="filteredFavorites.length === 0" class="empty-state">
        <el-empty description="暂无收藏商品">
          <el-button type="primary" @click="$router.push('/consumer/products')">去逛逛</el-button>
        </el-empty>
      </div>

      <div v-else class="favorites-grid">
        <div v-for="item in filteredFavorites" :key="item.id" class="favorite-card">
          <div class="product-image" @click="goToDetail(item.productId)">
            <img :src="getImageUrl(item.mainImage || item.image)" :alt="item.name" />
            <div class="delete-btn" @click.stop="removeFavorite(item)">
              <el-icon><Close /></el-icon>
            </div>
          </div>
          <div class="product-info">
            <h4 class="product-name" @click="goToDetail(item.productId)">{{ item.name }}</h4>
            <p class="product-origin">
              <el-icon><Location /></el-icon>
              {{ item.origin }}
            </p>
            <div class="product-footer">
              <span class="product-price">¥{{ item.price }}</span>
              <el-button type="primary" size="small" @click="addToCart(item)">加入购物车</el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Close, Location, Search } from '@element-plus/icons-vue'
import { getImageUrl } from '@/utils/image'
import { useCartStore } from '@/stores/cart'
import request from '@/api/request'

const router = useRouter()
const cartStore = useCartStore()
const loading = ref(false)

const favorites = ref<any[]>([])
const searchKeyword = ref('') // 搜索关键词

// 过滤后的收藏列表
const filteredFavorites = computed(() => {
  if (!searchKeyword.value) {
    return favorites.value
  }
  const keyword = searchKeyword.value.toLowerCase()
  return favorites.value.filter(item => 
    item.name?.toLowerCase().includes(keyword) ||
    item.origin?.toLowerCase().includes(keyword)
  )
})

// 处理搜索
const handleSearch = () => {
  // 搜索通过 computed 自动处理，这里可以添加额外的逻辑
}

// 获取收藏列表
const fetchFavorites = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/favorite/list')
    favorites.value = res || []
  } catch (error) {
    console.error('获取收藏列表失败:', error)
    ElMessage.error('获取收藏列表失败')
  } finally {
    loading.value = false
  }
}

const goToDetail = (productId: number) => {
  router.push(`/consumer/product/${productId}`)
}

const removeFavorite = async (item: any) => {
  try {
    await ElMessageBox.confirm('确定要取消收藏该商品吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await request.delete(`/favorite/remove/${item.productId}`)
    
    const index = favorites.value.findIndex(f => f.id === item.id)
    if (index > -1) {
      favorites.value.splice(index, 1)
    }
    ElMessage.success('已取消收藏')
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('取消收藏失败:', error)
      ElMessage.error('取消收藏失败')
    }
  }
}

const addToCart = (item: any) => {
  cartStore.addItem({
    id: Date.now(),
    productId: item.productId,
    productName: item.name,
    productImage: item.mainImage || item.image,
    price: item.price,
    unit: item.unit,
    quantity: 1,
    stock: 100,
    farmerId: item.farmerId || 1,
    farmerName: item.farmerName || ''
  })
  ElMessage.success('已加入购物车')
}

onMounted(() => {
  fetchFavorites()
})
</script>

<style scoped>
.favorites-view {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.favorites-container {
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

.search-box {
  margin-bottom: 24px;
}

.search-box :deep(.el-input__wrapper) {
  border-radius: 24px 0 0 24px;
  box-shadow: none;
  border: 1px solid var(--apple-light-gray);
  border-right: none;
  padding-left: 16px;
}

.search-box :deep(.el-input__inner) {
  font-size: 15px;
}

.search-box :deep(.el-input-group__append) {
  border-radius: 0 24px 24px 0;
  background: var(--apple-blue);
  border-color: var(--apple-blue);
}

.search-box :deep(.el-input-group__append .el-button) {
  color: white;
  font-weight: 500;
}

.empty-state {
  padding: 80px 0;
}

.favorites-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.favorite-card {
  background: var(--apple-bg);
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s ease;
}

.favorite-card:hover {
  box-shadow: var(--apple-shadow);
}

.product-image {
  position: relative;
  height: 200px;
  overflow: hidden;
  cursor: pointer;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.delete-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 28px;
  height: 28px;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.3s;
}

.favorite-card:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  background: rgba(255, 0, 0, 0.7);
}

.product-info {
  padding: 16px;
}

.product-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--apple-dark-gray);
  margin-bottom: 8px;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-name:hover {
  color: var(--apple-blue);
}

.product-origin {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--apple-gray);
  margin-bottom: 12px;
}

.product-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.product-price {
  font-size: 18px;
  font-weight: 700;
  color: #ff6b6b;
}
</style>
