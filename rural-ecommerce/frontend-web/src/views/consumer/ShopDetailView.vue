<template>
  <div class="shop-detail-view">
    <!-- 店铺头部信息 -->
    <div class="shop-header" :style="shopHeaderStyle">
      <div class="shop-header-overlay"></div>
      <div class="shop-header-bg">
        <div class="shop-header-content">
          <div class="shop-info-main">
            <el-avatar :size="80" :src="shopInfo.avatar || '/images/default-avatar.jpg'" class="shop-avatar" />
            <div class="shop-info-text">
              <div class="shop-name-row">
                <h1 class="shop-name">{{ shopInfo.name }}</h1>
                <el-button 
                  :type="isFollowed ? 'warning' : 'primary'"
                  :loading="followLoading"
                  @click.stop="handleFollow"
                  class="follow-btn"
                >
                  <el-icon v-if="isFollowed"><StarFilled /></el-icon>
                  <el-icon v-else><Star /></el-icon>
                  {{ isFollowed ? '已关注' : '关注店铺' }}
                </el-button>
              </div>
              <p class="shop-location">
                <el-icon><Location /></el-icon>
                {{ shopInfo.location }}
              </p>
              <p class="shop-description">{{ shopInfo.description }}</p>
              <div class="shop-tags">
                <el-tag v-for="tag in shopInfo.tags" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
              </div>
            </div>
          </div>
          <div class="shop-stats">
            <div class="stat-box">
              <span class="stat-number">{{ shopInfo.productCount || 0 }}</span>
              <span class="stat-label">商品数</span>
            </div>
            <div class="stat-box">
              <span class="stat-number">{{ shopInfo.sales || 0 }}</span>
              <span class="stat-label">总销量</span>
            </div>
            <div class="stat-box">
              <span class="stat-number">{{ shopInfo.rating || 5.0 }}</span>
              <span class="stat-label">店铺评分</span>
            </div>
            <div class="stat-box">
              <span class="stat-number">{{ shopInfo.followers || 0 }}</span>
              <span class="stat-label">关注人数</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="shop-content">
      <!-- 左侧店铺信息 -->
      <div class="shop-sidebar">
        <el-card class="shop-info-card">
          <template #header>
            <span class="card-title">店铺信息</span>
          </template>
          <div class="info-list">
            <div class="info-item">
              <span class="info-label">店铺名称</span>
              <span class="info-value">{{ shopInfo.name }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">所在地区</span>
              <span class="info-value">{{ shopInfo.location }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">开店时间</span>
              <span class="info-value">{{ shopInfo.createTime || '2024-01-01' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">主营品类</span>
              <span class="info-value">{{ shopInfo.mainCategory || '农产品' }}</span>
            </div>
          </div>
        </el-card>

        <el-card class="shop-contact-card">
          <template #header>
            <span class="card-title">联系方式</span>
          </template>
          <div class="contact-list">
            <div class="contact-item">
              <el-icon><User /></el-icon>
              <span>店主：{{ shopInfo.ownerName || shopInfo.name }}</span>
            </div>
            <div class="contact-item">
              <el-icon><Phone /></el-icon>
              <span>电话：{{ shopInfo.phone || '暂无' }}</span>
            </div>
            <div class="contact-item">
              <el-icon><Message /></el-icon>
              <span>在线咨询</span>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 右侧商品列表 -->
      <div class="shop-products">
        <el-card class="products-card">
          <template #header>
            <div class="products-header">
              <span class="card-title">店铺商品</span>
              <div class="filter-bar">
                <el-radio-group v-model="sortBy" size="small">
                  <el-radio-button label="default">默认</el-radio-button>
                  <el-radio-button label="sales">销量</el-radio-button>
                  <el-radio-button label="price">价格</el-radio-button>
                  <el-radio-button label="new">新品</el-radio-button>
                </el-radio-group>
              </div>
            </div>
          </template>

          <!-- 商品列表 -->
          <div v-loading="loading" class="products-grid">
            <div 
              v-for="product in productList" 
              :key="product.id" 
              class="product-card"
              @click="goToProduct(product.id)"
            >
              <div class="product-image">
                <img :src="getImageUrl(product.mainImage || product.image)" :alt="product.name" />
                <div v-if="product.tag" class="product-tag">{{ product.tag }}</div>
              </div>
              <div class="product-info">
                <h4 class="product-name">{{ product.name }}</h4>
                <p class="product-origin">
                  <el-icon><Location /></el-icon>
                  {{ product.origin || shopInfo.location }}
                </p>
                <div class="product-footer">
                  <span class="product-price">¥{{ product.price }}</span>
                  <span class="product-sales">已售 {{ product.sales || 0 }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 空状态 -->
          <div v-if="!loading && productList.length === 0" class="empty-state">
            <el-empty description="暂无商品" />
          </div>

          <!-- 分页 -->
          <div v-if="productList.length > 0" class="pagination-wrapper">
            <el-pagination
              v-model:current-page="page"
              v-model:page-size="pageSize"
              :total="total"
              layout="prev, pager, next"
              @current-change="handlePageChange"
            />
          </div>
        </el-card>
      </div>
    </div>

    <!-- 右侧悬浮按钮 -->
    <div class="floating-actions">
      <div class="floating-btn" @click="contactService">
        <el-icon size="20">
          <Service />
        </el-icon>
        <span>客服</span>
      </div>
      <div class="floating-divider"></div>
      <div class="floating-btn" @click="openSurvey">
        <el-icon size="20">
          <Document />
        </el-icon>
        <span>调研</span>
      </div>
      <div class="floating-divider"></div>
      <div class="floating-btn more-btn" @click="showMore">
        <el-icon size="20">
          <More />
        </el-icon>
        <span>更多</span>
        <!-- 更多选项菜单 -->
        <div class="more-menu">
          <div class="more-menu-item" @click="copyLink">
            <el-icon>
              <DocumentCopy />
            </el-icon>
            <span>复制链接</span>
          </div>
          <div class="more-menu-item" @click="openFeedback">
            <el-icon>
              <EditPen />
            </el-icon>
            <span>反馈</span>
          </div>
          <div class="more-menu-item" @click="openReport">
            <el-icon>
              <Warning />
            </el-icon>
            <span>举报</span>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 聊天窗口 -->
  <ChatWindow
    v-if="shopInfo.farmerId"
    ref="chatWindowRef"
    v-model:visible="chatVisible"
    :target-id="shopInfo.farmerId"
    :target-name="shopInfo.name || '商家'"
    :target-avatar="shopInfo.avatar || ''"
    :shop-name="shopInfo.name"
    :is-buyer="true"
    :is-seller="false"
    @select-product="openProductSelect"
  />

  <!-- 商品选择弹窗 -->
  <ProductSelectDialog
    v-model="productSelectVisible"
    :products="productList"
    @select="sendProductCard"
  />
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Location, User, Phone, Message, Service, Document, More, DocumentCopy, EditPen, Warning, Star, StarFilled } from '@element-plus/icons-vue'
import { getImageUrl } from '@/utils/image'
import { getShopDetail, getShopProducts } from '@/api/shop'
import { shopFollowApi } from '@/api/shopFollow'
import { useUserStore } from '@/stores/user'
import ChatWindow from '@/components/ChatWindow.vue'
import ProductSelectDialog from '@/components/ProductSelectDialog.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const shopId = ref(Number(route.params.id))

// 店铺头部背景样式
const shopHeaderStyle = computed(() => {
  if (shopInfo.background) {
    return {
      backgroundImage: `url(${getImageUrl(shopInfo.background)})`,
      backgroundSize: 'cover',
      backgroundPosition: 'center'
    }
  }
  return {
    background: 'linear-gradient(135deg, #007aff 0%, #5ac8fa 100%)'
  }
})

const loading = ref(false)
const page = ref(1)
const pageSize = ref(12)
const total = ref(0)
const sortBy = ref('default')

// 关注相关
const isFollowed = ref(false)
const followLoading = ref(false)

// 店铺信息
const shopInfo = reactive({
  id: 0,
  name: '',
  avatar: '',
  location: '',
  description: '',
  tags: [] as string[],
  productCount: 0,
  sales: 0,
  rating: 5.0,
  followers: 0,
  createTime: '',
  mainCategory: '',
  ownerName: '',
  phone: '',
  background: '',
  farmerId: 0
})

// 商品列表
const productList = ref<any[]>([])

// 聊天相关
const chatVisible = ref(false)
const chatWindowRef = ref<InstanceType<typeof ChatWindow>>()
const productSelectVisible = ref(false)

// 获取店铺详情
const fetchShopDetail = async () => {
  try {
    const res: any = await getShopDetail(shopId.value)
    Object.assign(shopInfo, res)
    // 获取粉丝数
    const fansCount: any = await shopFollowApi.getShopFansCount(shopId.value)
    shopInfo.followers = fansCount || 0
  } catch (error) {
    console.error('获取店铺详情失败:', error)
    ElMessage.error('获取店铺详情失败')
  }
}

// 检查是否已关注
const checkFollowStatus = async () => {
  if (!userStore.isLoggedIn) return
  try {
    const followed: any = await shopFollowApi.checkFollow(shopId.value)
    isFollowed.value = followed
  } catch (error) {
    console.error('检查关注状态失败:', error)
  }
}

// 关注/取消关注店铺
const handleFollow = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  followLoading.value = true
  try {
    if (isFollowed.value) {
      await shopFollowApi.unfollowShop(shopId.value)
      ElMessage.success('已取消关注')
      shopInfo.followers--
    } else {
      await shopFollowApi.followShop(shopId.value)
      ElMessage.success('关注成功')
      shopInfo.followers++
    }
    isFollowed.value = !isFollowed.value
  } catch (error: any) {
    console.error('关注操作失败:', error)
    ElMessage.error(error.response?.data?.message || '操作失败')
  } finally {
    followLoading.value = false
  }
}

// 获取店铺商品列表
const fetchShopProducts = async () => {
  loading.value = true
  try {
    const res: any = await getShopProducts(shopId.value, {
      page: page.value,
      size: pageSize.value,
      sortBy: sortBy.value === 'default' ? '' : sortBy.value
    })
    productList.value = res.list || []
    total.value = res.total || 0
  } catch (error) {
    console.error('获取商品列表失败:', error)
    ElMessage.error('获取商品列表失败')
  } finally {
    loading.value = false
  }
}

// 跳转到商品详情
const goToProduct = (productId: number) => {
  router.push(`/product/${productId}`)
}

// 分页变化
const handlePageChange = (val: number) => {
  page.value = val
  fetchShopProducts()
}

// 联系客服
const contactService = () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  if (!shopInfo.farmerId) {
    ElMessage.warning('无法联系商家')
    return
  }
  chatVisible.value = true
}

// 选择商品
const openProductSelect = () => {
  productSelectVisible.value = true
}

// 发送商品卡片
const sendProductCard = (product: any) => {
  chatWindowRef.value?.sendProductCard({
    productId: product.id,
    productName: product.name,
    productImage: product.mainImage || product.image,
    price: product.price,
    unit: product.unit,
    sales: product.sales
  })
}

// 用户调研
const openSurvey = () => {
  ElMessage.info('用户调研功能即将上线')
}

// 显示更多
const showMore = () => {
  // 点击更多按钮，菜单通过 CSS hover 显示
}

// 复制链接
const copyLink = () => {
  const url = window.location.href
  navigator.clipboard.writeText(url).then(() => {
    ElMessage.success('链接已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

// 反馈
const openFeedback = () => {
  ElMessage.info('反馈功能即将上线')
}

// 举报
const openReport = () => {
  ElMessage.info('举报功能即将上线')
}

// 监听排序变化
watch(sortBy, () => {
  page.value = 1
  fetchShopProducts()
})

onMounted(() => {
  fetchShopDetail()
  fetchShopProducts()
  checkFollowStatus()
})
</script>

<style scoped>
.shop-detail-view {
  min-height: 100vh;
  background: #f5f5f7;
}

/* 店铺头部 */
.shop-header {
  position: relative;
  padding: 60px 0;
  margin-bottom: 24px;
}

.shop-header-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 1;
}

.shop-header-bg {
  position: relative;
  z-index: 2;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
}

.shop-header-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 40px;
}

.shop-info-main {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.shop-avatar {
  border: 4px solid rgba(255, 255, 255, 0.3);
}

.shop-info-text {
  color: #fff;
}

.shop-name-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 8px;
}

.shop-name {
  font-size: 32px;
  font-weight: 700;
  margin: 0;
}

.follow-btn {
  padding: 8px 20px;
  font-size: 14px;
  border-radius: 20px;
}

.follow-btn .el-icon {
  margin-right: 4px;
}

.shop-location {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.shop-description {
  font-size: 14px;
  opacity: 0.85;
  line-height: 1.6;
  margin-bottom: 16px;
  max-width: 500px;
}

.shop-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.shop-tags :deep(.el-tag) {
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.3);
  color: #fff;
}

/* 店铺统计 */
.shop-stats {
  display: flex;
  gap: 32px;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 24px 32px;
}

.stat-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  color: #fff;
}

.stat-number {
  font-size: 28px;
  font-weight: 700;
}

.stat-label {
  font-size: 13px;
  opacity: 0.8;
}

/* 内容区域 */
.shop-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px 48px;
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 24px;
}

/* 侧边栏 */
.shop-sidebar {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.shop-info-card,
.shop-contact-card {
  border-radius: 16px;
  border: none;
  box-shadow: none !important;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1d1d1f;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-label {
  font-size: 14px;
  color: #86868b;
}

.info-value {
  font-size: 14px;
  color: #1d1d1f;
  font-weight: 500;
}

.contact-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  color: #1d1d1f;
}

.contact-item .el-icon {
  color: #007aff;
  font-size: 18px;
}

/* 商品区域 */
.shop-products {
  min-width: 0;
}

.products-card {
  border-radius: 16px;
  border: none;
  box-shadow: none !important;
}

.products-card :deep(.el-card__header) {
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
}

.products-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-bar {
  display: flex;
  gap: 12px;
}

/* 商品网格 */
.products-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  padding: 24px;
}

.product-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid #f0f0f0;
}

.product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.product-image {
  position: relative;
  width: 100%;
  aspect-ratio: 1;
  overflow: hidden;
  background: #f5f5f7;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.product-tag {
  position: absolute;
  top: 8px;
  left: 8px;
  background: #ff6b6b;
  color: #fff;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
}

.product-info {
  padding: 12px;
}

.product-name {
  font-size: 14px;
  font-weight: 600;
  color: #1d1d1f;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-origin {
  font-size: 12px;
  color: #86868b;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.product-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.product-price {
  font-size: 16px;
  font-weight: 700;
  color: #ff6b6b;
}

.product-sales {
  font-size: 12px;
  color: #86868b;
}

/* 空状态 */
.empty-state {
  padding: 60px 0;
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 24px;
  border-top: 1px solid #f0f0f0;
}

/* 响应式 */
@media (max-width: 1200px) {
  .products-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 992px) {
  .shop-content {
    grid-template-columns: 1fr;
  }
  
  .shop-sidebar {
    flex-direction: row;
  }
  
  .shop-info-card,
  .shop-contact-card {
    flex: 1;
  }
}

@media (max-width: 768px) {
  .shop-header-content {
    flex-direction: column;
    gap: 24px;
  }
  
  .shop-stats {
    width: 100%;
    justify-content: space-around;
  }
  
  .products-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .products-header {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }
}

/* 右侧悬浮按钮 */
.floating-actions {
  position: fixed;
  right: 24px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e5e5e5;
  z-index: 100;
}

.floating-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  cursor: pointer;
  transition: all 0.3s ease;
  gap: 3px;
}

.floating-btn:hover {
  background: #f5f5f5;
}

.floating-divider {
  width: 36px;
  height: 1px;
  background: #e5e5e5;
  margin: 0 auto;
}

.floating-btn span {
  font-size: 10px;
  font-weight: 500;
  color: #666;
}

/* 更多按钮菜单 */
.more-btn {
  position: relative;
}

.more-menu {
  position: absolute;
  right: calc(100% + 8px);
  bottom: 0;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e5e5e5;
  padding: 8px 0;
  min-width: 120px;
  opacity: 0;
  visibility: hidden;
  transform: translateX(10px);
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.more-btn:hover .more-menu {
  opacity: 1;
  visibility: visible;
  transform: translateX(0);
}

.more-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  cursor: pointer;
  transition: background 0.3s;
  font-size: 14px;
  color: #333;
}

.more-menu-item:hover {
  background: #f5f5f5;
}

.more-menu-item .el-icon {
  font-size: 16px;
  color: #666;
}

/* 响应式 - 平板 */
@media (max-width: 1200px) {
  .floating-actions {
    right: 16px;
  }
}

/* 响应式 - 手机 */
@media (max-width: 768px) {
  .floating-actions {
    right: 12px;
    top: auto;
    bottom: 100px;
    transform: none;
  }
}
</style>
