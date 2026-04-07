<template>
  <div class="followed-shops-page">
    <div class="followed-shops-container">
      <!-- 页面标题 -->
      <div class="page-header">
        <h1 class="page-title">我关注的店铺</h1>
        <p class="page-subtitle">探索您最喜爱的策展人带来的最新动态和热门作品。</p>
      </div>

      <!-- 分类标签 -->
      <div class="category-tabs">
        <div 
          v-for="tab in categoryTabs" 
          :key="tab.value"
          class="tab-item"
          :class="{ active: currentCategory === tab.value }"
          @click="currentCategory = tab.value"
        >
          {{ tab.label }}
        </div>
      </div>

      <!-- 店铺列表 -->
      <div class="shops-list" v-if="filteredShopList.length > 0">
        <div 
          v-for="shop in filteredShopList" 
          :key="shop.shopId"
          class="shop-card"
        >
          <!-- 店铺头部信息 -->
          <div class="shop-header">
            <div class="shop-avatar-wrapper">
              <el-avatar :size="56" :src="shop.shopAvatar || '/images/default-avatar.jpg'" class="shop-avatar" />
              <div class="online-status"></div>
            </div>
            <div class="shop-info">
              <h3 class="shop-name">{{ shop.shopName }}</h3>
              <div class="shop-stats">
                <span class="rating">
                  <el-icon><StarFilled /></el-icon>
                  {{ shop.rating || '4.9' }}
                </span>
                <span class="followers">{{ formatNumber(shop.followers || 12500) }} 名粉丝</span>
              </div>
            </div>
            <div class="shop-actions">
              <el-button 
                type="primary" 
                class="enter-shop-btn"
                @click="goToShop(shop.shopId)"
              >
                进入店铺
              </el-button>
              <el-button 
                class="icon-btn"
                circle
                @click="handleChat(shop)"
              >
                <el-icon><ChatDotRound /></el-icon>
              </el-button>
              <el-button 
                class="icon-btn"
                circle
                @click="handleMore(shop)"
              >
                <el-icon><MoreFilled /></el-icon>
              </el-button>
            </div>
          </div>

          <!-- 店内热销 -->
          <div class="shop-products">
            <div class="products-header">
              <span class="section-title">店内热销</span>
              <span class="section-subtitle">BESTSELLERS</span>
              <span class="view-all" @click="goToShop(shop.shopId)">查看全部</span>
            </div>
            <div class="products-grid">
              <div 
                v-for="product in shop.products || []" 
                :key="product.id"
                class="product-item"
                @click="goToProduct(product.id)"
              >
                <div class="product-image-wrapper">
                  <img :src="product.image || '/images/default-product.jpg'" :alt="product.name" class="product-image" />
                </div>
                <div class="product-info">
                  <p class="product-name">{{ product.name }}</p>
                  <div class="product-price">
                    <span class="price-symbol">¥</span>
                    <span class="price-value">{{ product.price?.toLocaleString() }}</span>
                    <span v-if="product.unit" class="price-unit">/{{ product.unit }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="empty-state">
        <el-icon :size="64" class="empty-icon"><Shop /></el-icon>
        <p class="empty-text">还没有关注任何店铺</p>
        <p class="empty-subtext">关注喜欢的店铺，第一时间获取上新信息</p>
        <el-button type="primary" class="discover-btn" @click="goToShopList">
          去发现好店
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Shop, StarFilled, ChatDotRound, MoreFilled } from '@element-plus/icons-vue'
import { shopFollowApi } from '@/api/shopFollow'
import { getShopProducts } from '@/api/shop'
import { categoryApi, type Category } from '@/api/category'

const router = useRouter()

interface Product {
  id: number
  name: string
  price: number
  image: string
  unit?: string
}

interface ShopItem {
  shopId: number
  shopName: string
  shopAvatar: string
  followTime: string
  mainCategory: string
  rating?: number
  followers?: number
  products?: Product[]
}

const shopList = ref<ShopItem[]>([])
const loading = ref(false)
const currentCategory = ref('all')
const categoryList = ref<Category[]>([])

// 分类标签（从后端获取）
const categoryTabs = computed(() => {
  const tabs = [{ label: '所有', value: 'all' }]
  categoryList.value.forEach(cat => {
    tabs.push({
      label: cat.name,
      value: cat.id.toString()
    })
  })
  return tabs
})

// 过滤后的店铺列表
const filteredShopList = computed(() => {
  if (currentCategory.value === 'all') {
    return shopList.value
  }
  // 根据分类名称过滤店铺
  const selectedCategory = categoryList.value.find(cat => cat.id.toString() === currentCategory.value)
  if (!selectedCategory) {
    return shopList.value
  }
  return shopList.value.filter(shop => {
    return shop.mainCategory === selectedCategory.name
  })
})

// 获取关注的店铺列表
const fetchFollowedShops = async () => {
  loading.value = true
  try {
    const res: any = await shopFollowApi.getMyFollowedShops()
    console.log('获取关注店铺列表响应:', res)
    const shops = res || []
    
    // 为每个店铺获取热销商品
    for (const shop of shops) {
      try {
        const productRes: any = await getShopProducts(shop.shopId, { page: 1, size: 4 })
        shop.products = (productRes?.list || productRes?.records || []).slice(0, 4).map((p: any) => ({
          id: p.id,
          name: p.name,
          price: p.price,
          image: p.mainImage || p.image,
          unit: p.unit
        }))
        // 添加模拟数据
        shop.rating = (4.5 + Math.random() * 0.5).toFixed(1)
        shop.followers = Math.floor(5000 + Math.random() * 20000)
      } catch (e) {
        console.error('获取店铺商品失败:', e)
        shop.products = []
      }
    }
    
    shopList.value = shops
    console.log('店铺列表数据:', shopList.value)
  } catch (error) {
    console.error('获取关注店铺列表失败:', error)
    ElMessage.error('获取关注店铺列表失败')
  } finally {
    loading.value = false
  }
}

// 取消关注
const handleUnfollow = async (shop: ShopItem) => {
  try {
    await ElMessageBox.confirm(
      `确定要取消关注「${shop.shopName}」吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await shopFollowApi.unfollowShop(shop.shopId)
    shopList.value = shopList.value.filter(s => s.shopId !== shop.shopId)
    ElMessage.success('已取消关注')
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('取消关注失败:', error)
      ElMessage.error('取消关注失败')
    }
  }
}

// 进入店铺
const goToShop = (shopId: number) => {
  router.push(`/consumer/shop/${shopId}`)
}

// 进入商品详情
const goToProduct = (productId: number) => {
  router.push(`/consumer/product/${productId}`)
}

// 去店铺列表页
const goToShopList = () => {
  router.push('/consumer/shops')
}

// 聊天
const handleChat = (shop: ShopItem) => {
  router.push(`/consumer/chat/${shop.shopId}`)
}

// 更多选项
const handleMore = (shop: ShopItem) => {
  ElMessageBox.confirm(
    `确定要取消关注「${shop.shopName}」吗？`,
    '取消关注',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    handleUnfollow(shop)
  }).catch(() => {})
}

// 格式化数字
const formatNumber = (num: number) => {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + '万'
  }
  return num.toString()
}

// 获取分类列表
const fetchCategories = async () => {
  try {
    const res = await categoryApi.getCategoryList()
    categoryList.value = res || []
  } catch (error) {
    console.error('获取分类列表失败:', error)
  }
}

onMounted(() => {
  fetchCategories()
  fetchFollowedShops()
})
</script>

<style scoped>
.followed-shops-page {
  min-height: 100vh;
  background: #f8f9fa;
  padding: 40px 0;
}

.followed-shops-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 24px;
}

/* 页面标题 */
.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 30px;
  font-weight: 700;
  color: #191c1e;
  margin: 0 0 8px;
  font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
  letter-spacing: -0.5px;
}

.page-subtitle {
  font-size: 14px;
  color: #707785;
  margin: 0;
}

/* 分类标签 */
.category-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 32px;
  flex-wrap: wrap;
}

.tab-item {
  padding: 8px 20px;
  border-radius: 9999px;
  font-size: 14px;
  font-weight: 500;
  color: #707785;
  background: #fff;
  border: none;
  cursor: pointer;
  transition: all 0.3s;
  white-space: nowrap;
}

.tab-item:hover {
  background: #e6e8eb;
  color: #191c1e;
}

.tab-item.active {
  background: #007aff;
  color: #fff;
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.2);
}

/* 店铺列表 */
.shops-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.shop-card {
  background: white;
  border-radius: 32px;
  padding: 32px;
  box-shadow: 0 12px 32px rgba(25,28,30,0.04);
  transition: all 0.3s;
  border: 1px solid transparent;
}

.shop-card:hover {
  box-shadow: 0 20px 48px rgba(25,28,30,0.08);
  border-color: rgba(112, 119, 133, 0.1);
}

/* 店铺头部 */
.shop-header {
  display: flex;
  align-items: center;
  margin-bottom: 32px;
  flex-wrap: wrap;
  gap: 16px;
}

@media (min-width: 768px) {
  .shop-header {
    flex-wrap: nowrap;
    gap: 0;
  }
}

.shop-avatar-wrapper {
  position: relative;
  margin-right: 20px;
}

.shop-avatar {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  border: none;
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.1);
}

@media (min-width: 768px) {
  .shop-avatar {
    width: 80px;
    height: 80px;
  }
}

.online-status {
  position: absolute;
  bottom: -2px;
  right: -2px;
  width: 20px;
  height: 20px;
  background: #85fa51;
  border: 2px solid #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.online-status::after {
  content: '';
  width: 10px;
  height: 10px;
  background: #1a5200;
  border-radius: 50%;
}

.shop-info {
  flex: 1;
}

.shop-name {
  font-size: 20px;
  font-weight: 700;
  color: #191c1e;
  margin: 0 0 4px;
  font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
}

@media (min-width: 768px) {
  .shop-name {
    font-size: 24px;
  }
}

.shop-stats {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  color: #707785;
}

.rating {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #f59e0b;
}

.rating .el-icon {
  font-size: 14px;
}

.followers {
  color: #707785;
}

.followers::before {
  content: '';
  display: inline-block;
  width: 4px;
  height: 4px;
  background: #c0c7d6;
  border-radius: 50%;
  margin-right: 12px;
  vertical-align: middle;
}

.shop-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

.enter-shop-btn {
  background: linear-gradient(135deg, #007aff 0%, #5ac8fa 100%);
  border: none;
  border-radius: 9999px;
  padding: 10px 24px;
  font-size: 14px;
  font-weight: 600;
  color: white;
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.2);
  transition: all 0.2s;
}

.enter-shop-btn:hover {
  background: linear-gradient(135deg, #0056b3 0%, #007aff 100%);
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(0, 122, 255, 0.3);
}

.enter-shop-btn:active {
  transform: scale(0.95);
}

.icon-btn {
  width: 40px;
  height: 40px;
  padding: 0;
  border: none;
  background: #eceef1;
  color: #707785;
  transition: all 0.2s;
}

.icon-btn:hover {
  background: #e0e3e6;
  color: #007aff;
}

/* 店内热销 */
.shop-products {
  margin-top: 24px;
}

.products-header {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  gap: 8px;
}

.section-title {
  font-size: 13px;
  font-weight: 700;
  color: #404753;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.section-subtitle {
  font-size: 12px;
  color: #707785;
  font-weight: 400;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.view-all {
  margin-left: auto;
  font-size: 12px;
  font-weight: 700;
  color: #007aff;
  cursor: pointer;
}

.view-all:hover {
  text-decoration: underline;
}

/* 商品网格 */
.products-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

@media (min-width: 768px) {
  .products-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

.product-item {
  cursor: pointer;
  transition: all 0.3s;
}

.product-image-wrapper {
  width: 120px;
  height: 120px;
  border-radius: 16px;
  overflow: hidden;
  background: #eceef1;
  margin-bottom: 12px;
  flex-shrink: 0;
}

@media (min-width: 768px) {
  .product-image-wrapper {
    width: 275px;
    height: 275px;
  }
}

.product-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s;
}

.product-item:hover .product-image {
  transform: scale(1.1);
}

.product-info {
  padding: 0 4px;
}

.product-name {
  font-size: 14px;
  font-weight: 600;
  color: #191c1e;
  margin: 0 0 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-price {
  display: flex;
  align-items: baseline;
  gap: 2px;
  margin: 0;
}

.price-symbol {
  font-size: 12px;
  font-weight: 700;
  color: #007aff;
}

.price-value {
  font-size: 18px;
  font-weight: 700;
  color: #007aff;
  letter-spacing: -0.5px;
}

.price-unit {
  font-size: 12px;
  font-weight: 500;
  color: #707785;
  margin-left: 2px;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100px 20px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.empty-icon {
  color: #d9d9d9;
  margin-bottom: 20px;
}

.empty-text {
  font-size: 18px;
  color: #333;
  margin: 0 0 8px;
}

.empty-subtext {
  font-size: 14px;
  color: #999;
  margin: 0 0 24px;
}

.discover-btn {
  background: #007aff;
  border-color: #007aff;
  border-radius: 20px;
  padding: 10px 32px;
  font-size: 14px;
}

/* 响应式 */
@media (max-width: 768px) {
  .followed-shops-page {
    padding: 24px 0;
  }
  
  .followed-shops-container {
    padding: 0 16px;
  }
  
  .page-title {
    font-size: 24px;
  }
  
  .category-tabs {
    gap: 8px;
    overflow-x: auto;
    padding-bottom: 4px;
  }
  
  .tab-item {
    padding: 6px 16px;
    font-size: 13px;
  }
  
  .shop-card {
    padding: 24px;
    border-radius: 24px;
  }
  
  .shop-header {
    margin-bottom: 24px;
  }
  
  .shop-actions {
    width: 100%;
    justify-content: flex-end;
    margin-left: 0;
    margin-top: 8px;
  }
  
  .enter-shop-btn {
    flex: 1;
  }
  
  .products-grid {
    gap: 12px;
  }
}
</style>
