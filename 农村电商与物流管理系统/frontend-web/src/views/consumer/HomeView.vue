<template>
  <div class="home-view">
    <!-- Banner轮播 -->
    <section class="banner-section">
      <el-carousel :height="bannerHeight" indicator-position="outside">
        <el-carousel-item v-for="(banner, index) in banners" :key="banner.id || index">
          <div class="banner-item" @click="banner.link && $router.push(banner.link)">
            <img :src="getImageUrl(banner.image)" class="banner-image" />
          </div>
        </el-carousel-item>
      </el-carousel>
    </section>

    <!-- 分类导航 -->
    <section class="category-section">
      <div class="section-container">
        <div class="section-header">
          <h3 class="section-title">商品分类</h3>
          <el-button link type="primary" @click="$router.push('/consumer/products')">
            查看更多 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
        <div class="category-grid">
          <div v-for="category in categories" :key="category.id" class="category-card" @click="goToCategory(category.id)">
            <div class="category-icon" :style="{ background: category.color }">
              <el-icon size="32"><component :is="getIconComponent(category.icon)" /></el-icon>
            </div>
            <span class="category-name">{{ category.name }}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 热销排行 -->
    <section class="hot-section">
      <div class="section-container">
        <h3 class="section-title">热销排行</h3>
        <div v-loading="loading.hot" class="hot-grid">
          <template v-if="hotProducts.length > 0">
            <div v-for="(product, index) in hotProducts" :key="product.id" class="hot-card" @click="goToDetail(product.id)">
              <div class="hot-rank" :class="{ 'top-three': index < 3 }">{{ index + 1 }}</div>
              <img :src="getImageUrl(product.mainImage || product.image)" class="hot-image" />
              <div class="hot-info">
                <h4 class="hot-name">{{ product.name }}</h4>
                <p class="hot-price">¥{{ product.price }}<span class="hot-unit">{{ product.unit ? '/' + product.unit : '' }}</span></p>
                <div class="hot-meta">
                  <span class="hot-sales">已售 {{ product.sales || 0 }} 件</span>
                  <span class="hot-farmer">{{ product.farmerName || siteName + '自营' }}</span>
                </div>
              </div>
            </div>
          </template>
          <div v-else class="empty-hot">
            <el-empty description="暂无排行" />
          </div>
        </div>
      </div>
    </section>

    <!-- 特色店铺 -->
    <section class="farmer-section">
      <div class="section-container">
        <div class="section-header">
          <h3 class="section-title">特色店铺</h3>
          <el-button link type="primary" @click="$router.push('/consumer/shops')">
            查看更多 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
        <div v-loading="loading.farmers" class="farmer-grid">
          <div v-for="farmer in featuredFarmers" :key="farmer.id" class="farmer-card" @click="goToShop(farmer.id)">
            <div class="farmer-header">
              <el-avatar :size="64" :src="farmer.avatar || '/images/default-avatar.jpg'" />
              <div class="farmer-info">
                <h4 class="farmer-name">{{ farmer.name }}</h4>
                <p class="farmer-location">
                  <el-icon><Location /></el-icon>
                  {{ farmer.location }}
                </p>
              </div>
            </div>
            <p class="farmer-desc">{{ farmer.description }}</p>
            <div class="farmer-stats">
              <div class="stat-item">
                <span class="stat-value">{{ farmer.productCount || 0 }}</span>
                <span class="stat-label">商品</span>
              </div>
              <div class="stat-item">
                <span class="stat-value">{{ farmer.sales || 0 }}</span>
                <span class="stat-label">销量</span>
              </div>
              <div class="stat-item">
                <span class="stat-value">{{ farmer.rating || 5.0 }}</span>
                <span class="stat-label">评分</span>
              </div>
            </div>
          </div>
        </div>
        <!-- 空状态 -->
        <div v-if="!loading.farmers && featuredFarmers.length === 0" class="empty-state">
          <el-empty description="暂无店铺" />
        </div>
      </div>
    </section>

    <!-- 推荐商品 -->
    <section class="recommend-section">
      <div class="section-container">
        <div class="section-header">
          <h3 class="section-title">精选推荐</h3>
          <el-button link type="primary" @click="$router.push('/consumer/products')">
            查看更多 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
        <div v-loading="loading.recommend" class="product-grid">
          <div v-for="product in recommendProducts" :key="product.id" class="product-card" @click="goToDetail(product.id)">
            <div class="product-image">
              <img :src="getImageUrl(product.mainImage || product.image)" :alt="product.name" />
              <div v-if="product.tag" class="product-tag">{{ product.tag }}</div>
            </div>
            <div class="product-info">
              <h4 class="product-name">{{ product.name }}</h4>
              <p class="product-origin">
                <el-icon><Location /></el-icon>
                {{ product.origin }}
              </p>
              <div class="product-footer">
                <span class="product-price">¥{{ product.price }}<span class="product-unit">{{ product.unit ? '/' + product.unit : '' }}</span></span>
                <span class="product-sales">已售 {{ product.sales || 0 }}</span>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 暂无商品 -->
        <div v-if="!loading.recommend && recommendProducts.length === 0" class="empty-state">
          <el-empty description="暂无商品" />
        </div>
        
        <!-- 加载更多提示 -->
        <div v-else class="load-more-wrapper">
          <div v-if="recommendLoading" class="loading-text">
            <el-icon class="is-loading"><Loading /></el-icon> 加载中...
          </div>
          <div v-else-if="recommendHasMore" class="load-more-hint">
            向下滚动加载更多
          </div>
          <div v-else-if="recommendProducts.length > 0" class="no-more-text">
            没有更多商品了
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, markRaw, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Location, Apple, Food, Chicken, Coffee, Cherry, Grape, Orange, Loading } from '@element-plus/icons-vue'
import { getImageUrl } from '@/utils/image'
import { productApi } from '@/api/product'
import { getFeaturedShops } from '@/api/shop'
import request from '@/api/request'

const router = useRouter()

// 站点名称
const siteName = ref('农鲜达')

// 获取站点配置
const fetchSiteConfig = async () => {
  try {
    const config = await request.get('/admin/config') as Record<string, string>
    siteName.value = config['site.name'] || '农鲜达'
  } catch (error) {
    console.error('获取站点配置失败:', error)
  }
}

// 图标映射
const iconMap: Record<string, any> = {
  Apple: markRaw(Apple),
  Food: markRaw(Food),
  Chicken: markRaw(Chicken),
  Coffee: markRaw(Coffee),
  Cherry: markRaw(Cherry),
  Grape: markRaw(Grape),
  Orange: markRaw(Orange)
}

const getIconComponent = (iconName: string) => {
  return iconMap[iconName] || Apple
}

// 加载状态
const loading = ref({
  recommend: false,
  hot: false,
  farmers: false,
  categories: false
})

// 窗口宽度
const windowWidth = ref(window.innerWidth)

// 响应式轮播图高度
const bannerHeight = computed(() => {
  if (windowWidth.value <= 576) {
    return '200px'
  } else if (windowWidth.value <= 768) {
    return '280px'
  } else if (windowWidth.value <= 992) {
    return '350px'
  } else if (windowWidth.value <= 1200) {
    return '420px'
  }
  return '550px'
})

// 监听窗口大小变化
const handleResize = () => {
  windowWidth.value = window.innerWidth
}

// Banner数据
const banners = ref<any[]>([])

// 获取轮播图列表
const fetchBanners = async () => {
  try {
    const data = await request.get('/product/banners') as any[]
    // 只显示状态为启用的轮播图
    banners.value = (data || []).filter((banner: any) => banner.status === 1)
  } catch (error) {
    console.error('获取轮播图失败:', error)
    // 使用默认轮播图
    banners.value = [
      { image: '/images/banner1.jpg', title: '默认轮播图1' },
      { image: '/images/banner2.jpg', title: '默认轮播图2' }
    ]
  }
}

// 分类数据
const categories = ref<any[]>([])

// 推荐商品
const recommendProducts = ref<any[]>([])
const recommendPage = ref(1)
const recommendPageSize = 16
const recommendTotal = ref(0)
const recommendLoading = ref(false)
const recommendHasMore = ref(true)

// 热销商品
const hotProducts = ref<any[]>([])

// 特色店铺
const featuredFarmers = ref<any[]>([])

// 获取分类列表
const fetchCategories = async () => {
  loading.value.categories = true
  try {
    const res: any = await productApi.getCategories()
    // 为分类添加颜色和图标，只取前8条
    const colors = ['#ff6b6b', '#51cf66', '#fcc419', '#ff8787', '#9775fa', '#20c997', '#74c0fc', '#ff922b']
    const icons = ['Apple', 'Food', 'Coffee', 'Chicken', 'Cherry', 'Grape', 'Orange', 'Apple']
    categories.value = (res || []).slice(0, 8).map((cat: any, index: number) => ({
      ...cat,
      color: colors[index % colors.length],
      icon: icons[index % icons.length]
    }))
  } catch (error) {
    console.error('获取分类失败:', error)
    // 使用默认分类
    categories.value = [
      { id: 1, name: '新鲜水果', icon: 'Apple', color: '#ff6b6b' },
      { id: 2, name: '蔬菜菌菇', icon: 'Food', color: '#51cf66' },
      { id: 3, name: '粮油米面', icon: 'Coffee', color: '#fcc419' },
      { id: 4, name: '肉禽蛋品', icon: 'Chicken', color: '#ff8787' },
      { id: 5, name: '干货特产', icon: 'Cherry', color: '#9775fa' },
      { id: 6, name: '农副加工', icon: 'Grape', color: '#20c997' },
      { id: 7, name: '农资农具', icon: 'Orange', color: '#74c0fc' },
      { id: 8, name: '苗木花卉', icon: 'Apple', color: '#ff922b' }
    ]
  } finally {
    loading.value.categories = false
  }
}

// 获取推荐商品
const fetchRecommendProducts = async (isLoadMore = false) => {
  if (recommendLoading.value) return
  recommendLoading.value = true
  
  if (!isLoadMore) {
    recommendPage.value = 1
    recommendProducts.value = []
  }
  
  try {
    const res: any = await productApi.getProductList({
      page: recommendPage.value,
      size: recommendPageSize
    })
    
    const list = res.list || res || []
    const total = res.total || list.length
    
    if (isLoadMore) {
      recommendProducts.value.push(...list)
    } else {
      recommendProducts.value = list
    }
    
    recommendTotal.value = total
    recommendHasMore.value = recommendProducts.value.length < total
    
    // 没有数据时显示空状态
    if (recommendProducts.value.length === 0) {
      recommendHasMore.value = false
    }
  } catch (error) {
    console.error('获取推荐商品失败:', error)
    // 出错时显示空状态
    if (!isLoadMore) {
      recommendProducts.value = []
      recommendHasMore.value = false
    }
  } finally {
    recommendLoading.value = false
    loading.value.recommend = false
  }
}

// 加载更多推荐商品
const loadMoreRecommend = async () => {
  if (!recommendHasMore.value || recommendLoading.value) return
  recommendPage.value++
  await fetchRecommendProducts(true)
}

// 触底检测
const handleScroll = () => {
  const scrollTop = window.scrollY || document.documentElement.scrollTop
  const windowHeight = window.innerHeight
  const documentHeight = document.documentElement.scrollHeight
  
  // 距离底部100px时触发加载
  if (scrollTop + windowHeight >= documentHeight - 100) {
    loadMoreRecommend()
  }
}

// 获取热销商品
const fetchHotProducts = async () => {
  loading.value.hot = true
  try {
    const res: any = await productApi.getHotProducts()
    hotProducts.value = res || []
  } catch (error) {
    console.error('获取热销商品失败:', error)
    hotProducts.value = []
  } finally {
    loading.value.hot = false
  }
}

// 获取特色店铺
const fetchFeaturedFarmers = async () => {
  loading.value.farmers = true
  try {
    const res: any = await getFeaturedShops(4)
    featuredFarmers.value = res || []
  } catch (error) {
    console.error('获取特色店铺失败:', error)
    featuredFarmers.value = []
  } finally {
    loading.value.farmers = false
  }
}

const goToCategory = (id: number) => {
  router.push(`/consumer/products?category=${id}`)
}

const goToDetail = (id: number) => {
  router.push(`/product/${id}`)
}

const goToShop = (id: number) => {
  router.push(`/shop/${id}`)
}

onMounted(() => {
  fetchSiteConfig()
  fetchBanners()
  fetchCategories()
  fetchRecommendProducts()
  fetchHotProducts()
  fetchFeaturedFarmers()
  
  // 添加滚动监听
  window.addEventListener('scroll', handleScroll)
  // 添加窗口大小监听
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.home-view {
  padding-bottom: 48px;
}

.section-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 24px;
}

.section-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--apple-dark-gray);
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

/* Banner */
.banner-section {
  margin-bottom: 0;
}

.banner-section :deep(.el-carousel),
.banner-section :deep(.el-carousel__container) {
  transition: height 0.5s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.banner-section :deep(.el-carousel__indicators) {
  margin-top: 16px;
  transition: margin-top 0.4s ease;
}

.banner-section :deep(.el-carousel__indicator) {
  padding: 6px 4px;
  transition: padding 0.4s ease;
}

.banner-section :deep(.el-carousel__button) {
  width: 30px;
  height: 3px;
  border-radius: 2px;
  transition: width 0.4s ease, height 0.4s ease;
}

@media (max-width: 768px) {
  .banner-section :deep(.el-carousel__indicators) {
    margin-top: 10px;
  }
  
  .banner-section :deep(.el-carousel__indicator) {
    padding: 4px 3px;
  }
  
  .banner-section :deep(.el-carousel__button) {
    width: 20px;
    height: 2px;
  }
}

/* 搜索区域 */
.banner-item {
  overflow: hidden;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
}

.banner-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 分类 */
.category-section {
  margin-bottom: 48px;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 16px;
}

.category-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 20px;
  background: var(--apple-card-bg);
  border-radius: var(--apple-radius-lg);
  cursor: pointer;
  transition: all 0.3s ease;
}

.category-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--apple-shadow);
}

.category-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.category-name {
  font-size: 14px;
  color: var(--apple-dark-gray);
  font-weight: 500;
}

/* 推荐商品 */
.recommend-section {
  margin-bottom: 48px;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.product-card {
  background: var(--apple-card-bg);
  border-radius: var(--apple-radius-lg);
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
}

.product-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--apple-shadow);
}

.product-image {
  position: relative;
  height: 280px;
  overflow: hidden;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.product-tag {
  position: absolute;
  top: 12px;
  left: 12px;
  background: linear-gradient(135deg, #ff6b6b, #ee5a6f);
  color: white;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.product-info {
  padding: 16px;
}

.product-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--apple-dark-gray);
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
  font-size: 20px;
  font-weight: 700;
  color: #ff6b6b;
}

.product-unit {
  font-size: 14px;
  font-weight: 400;
  color: var(--apple-gray);
  margin-left: 5px;
}

.product-sales {
  font-size: 13px;
  color: var(--apple-gray);
}

/* 热销排行 */
.hot-section {
  margin-bottom: 48px;
}

.hot-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.hot-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: var(--apple-card-bg);
  padding: 16px;
  border-radius: var(--apple-radius-lg);
  cursor: pointer;
  transition: all 0.3s ease;
}

.hot-card:hover {
  box-shadow: var(--apple-shadow);
}

.hot-rank {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--apple-light-gray);
  border-radius: 50%;
  font-weight: 700;
  color: var(--apple-gray);
}

.hot-rank.top-three {
  background: linear-gradient(135deg, #ffd700, #ffb700);
  color: white;
}

.hot-image {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 10px;
}

.hot-info {
  flex: 1;
}

.hot-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--apple-dark-gray);
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hot-price {
  font-size: 18px;
  font-weight: 700;
  color: #ff6b6b;
  margin-bottom: 8px;
}

.hot-unit {
  font-size: 12px;
  font-weight: 400;
  color: #909399;
  margin-left: 5px;
}

.hot-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #909399;
}

.hot-sales {
  color: #909399;
}

.hot-farmer {
  color: #666;
}

/* 特色店铺 */
.farmer-section {
  margin-bottom: 48px;
}

.farmer-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.farmer-card {
  background: var(--apple-card-bg);
  border-radius: var(--apple-radius-lg);
  padding: 24px;
  transition: all 0.3s ease;
  cursor: pointer;
  border: 1.5px solid transparent;
}

.farmer-card:hover {
  border-color: #409eff;
}

.farmer-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.farmer-info {
  flex: 1;
}

.farmer-name {
  font-size: 18px;
  font-weight: 600;
  color: var(--apple-dark-gray);
  margin-bottom: 4px;
}

.farmer-location {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--apple-gray);
}

.farmer-desc {
  font-size: 14px;
  color: var(--apple-gray);
  line-height: 1.6;
  margin-bottom: 16px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.farmer-stats {
  display: flex;
  justify-content: space-around;
  padding-top: 16px;
  border-top: 1px solid var(--apple-border);
}

.stat-item {
  text-align: center;
}

.stat-value {
  display: block;
  font-size: 20px;
  font-weight: 700;
  color: var(--apple-dark-gray);
}

.stat-label {
  font-size: 12px;
  color: var(--apple-gray);
}

/* 响应式 - 大屏幕 */
@media (max-width: 1400px) {
  .section-container {
    padding: 0 20px;
  }
}

/* 响应式 - 平板 */
@media (max-width: 1200px) {
  .section-container {
    padding: 0 16px;
  }
  
  .section-title {
    font-size: 24px;
  }
  
  .category-grid {
    grid-template-columns: repeat(6, 1fr);
    gap: 12px;
  }
  
  .category-card {
    padding: 16px;
  }
  
  .category-icon {
    width: 56px;
    height: 56px;
  }
  
  .product-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 16px;
  }
  
  .product-image {
    height: 220px;
  }
  
  .farmer-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }
  
  .hot-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }
}

/* 响应式 - 小平板 */
@media (max-width: 992px) {
  .category-grid {
    grid-template-columns: repeat(4, 1fr);
  }
  
  .product-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .farmer-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .hot-grid {
    grid-template-columns: 1fr;
  }
}

/* 响应式 - 手机 */
@media (max-width: 768px) {
  .home-view {
    padding-bottom: 32px;
  }
  
  .section-container {
    padding: 0 12px;
  }
  
  .section-title {
    font-size: 20px;
    margin-bottom: 16px;
  }
  
  .section-header {
    margin-bottom: 16px;
  }
  
  /* 分类移动端 */
  .category-section {
    margin-bottom: 32px;
  }
  
  .category-grid {
    grid-template-columns: repeat(4, 1fr);
    gap: 8px;
  }
  
  .category-card {
    padding: 12px 8px;
    gap: 8px;
  }
  
  .category-icon {
    width: 48px;
    height: 48px;
  }
  
  .category-icon .el-icon {
    font-size: 24px !important;
  }
  
  .category-name {
    font-size: 12px;
  }
  
  /* 精选推荐卡片移动端 */
  .recommend-section {
    margin-bottom: 32px;
  }
  
  .product-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
  
  .product-card {
    border-radius: 12px;
  }
  
  .product-image {
    height: 180px;
  }
  
  .product-tag {
    top: 8px;
    left: 8px;
    padding: 3px 8px;
    font-size: 10px;
    border-radius: 12px;
  }
  
  .product-info {
    padding: 12px;
  }
  
  .product-name {
    font-size: 14px;
    margin-bottom: 6px;
  }
  
  .product-origin {
    font-size: 12px;
    margin-bottom: 8px;
  }
  
  .product-price {
    font-size: 16px;
  }
  
  .product-sales {
    font-size: 12px;
  }
  
  /* 热销排行移动端 */
  .hot-section {
    margin-bottom: 32px;
  }
  
  .hot-card {
    padding: 12px;
    gap: 12px;
  }
  
  .hot-rank {
    width: 28px;
    height: 28px;
    font-size: 14px;
  }
  
  .hot-image {
    width: 60px;
    height: 60px;
  }
  
  .hot-name {
    font-size: 14px;
    margin-bottom: 6px;
  }
  
  .hot-price {
    font-size: 16px;
    margin-bottom: 6px;
  }
  
  .hot-meta {
    font-size: 11px;
  }
  
  /* 店铺卡片移动端 */
  .farmer-section {
    margin-bottom: 32px;
  }
  
  .farmer-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  
  .farmer-card {
    padding: 16px;
  }
  
  .farmer-header {
    gap: 12px;
    margin-bottom: 12px;
  }
  
  .farmer-name {
    font-size: 16px;
  }
  
  .farmer-desc {
    font-size: 13px;
    margin-bottom: 12px;
  }
  
  .farmer-stats {
    padding-top: 12px;
  }
  
  .stat-value {
    font-size: 18px;
  }
  
  .stat-label {
    font-size: 11px;
  }
}

/* 小屏幕手机 */
@media (max-width: 576px) {
  .product-grid {
    gap: 8px;
  }
  
  .product-image {
    height: 160px;
  }

  .product-info {
    padding: 10px;
  }
  
  .product-name {
    font-size: 13px;
  }
  
  .product-price {
    font-size: 14px;
  }
}

/* 空状态样式 */
.empty-state {
  padding: 60px 0;
}

/* 热销排行空状态 */
.empty-hot {
  grid-column: 1 / -1;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 60px 0;
}

/* 加载更多样式 */
.load-more-wrapper {
  text-align: center;
  padding: 32px 0;
}

.loading-text {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--apple-gray);
  font-size: 14px;
}

.load-more-hint {
  color: var(--apple-gray);
  font-size: 14px;
}

.no-more-text {
  color: var(--apple-light-gray);
  font-size: 14px;
}
</style>
