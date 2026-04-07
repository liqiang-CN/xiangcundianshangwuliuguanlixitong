<template>
  <div class="product-detail-view">
    <div class="detail-container">
      <!-- 面包屑 -->
      <el-breadcrumb class="breadcrumb">
        <el-breadcrumb-item :to="{ path: '/consumer' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/consumer/products' }">商品</el-breadcrumb-item>
        <el-breadcrumb-item>{{ product.name }}</el-breadcrumb-item>
      </el-breadcrumb>

      <!-- 商品主信息 -->
      <div class="product-main" v-loading="loading">
        <div class="product-gallery">
          <div class="main-image">
            <el-image :src="currentImage || getImageUrl(product.mainImage || product.image)" :alt="product.name"
              :preview-src-list="previewImages" fit="cover" class="main-image-el" preview-teleported />
          </div>
          <div class="thumbnail-list" v-if="allGalleryImages.length > 1">
            <div v-for="(img, index) in allGalleryImages" :key="index" class="thumbnail"
              :class="{ active: currentImage === getImageUrl(img) }" @click="currentImage = getImageUrl(img)">
              <img :src="getImageUrl(img)" />
            </div>
          </div>
        </div>

        <div class="product-info">
          <h1 class="product-name">{{ product.name }}</h1>
          
          <!-- 店铺信息 -->
          <div class="shop-info-row" v-if="product.farmerName || product.shopName">
            <span class="shop-label">店铺</span>
            <span class="shop-name" @click="goToShop">
              <el-icon><Shop /></el-icon>
              {{ product.shopName || product.farmerName }}
            </span>
          </div>

          <div class="info-row location-sales">
            <span class="info-value">
              <el-icon><Location /></el-icon>
              {{ product.origin }}
            </span>
            <span class="divider">|</span>
            <span class="info-value">销量 {{ product.sales || 0 }}件</span>
            <span class="divider">|</span>
            <span class="info-value">库存 {{ product.stock || 0 }}件</span>
          </div>

          <p class="product-desc">{{ product.description }}</p>

          <div class="price-box">
            <span class="price-value"><small class="price-symbol">¥</small>{{ product.price }}<span v-if="product.unit" class="price-unit"> / {{ product.unit }}</span></span>
          </div>

          <div class="purchase-box">
            <div class="quantity-row">
              <span class="quantity-label">数量</span>
              <el-input-number v-model="quantity" :min="1" :max="product.stock || 99" />
            </div>

            <div class="action-buttons">
              <el-button class="btn-cart" size="large" @click="addToCart">
                <el-icon><ShoppingCart /></el-icon>加入购物车
              </el-button>
              <el-button class="btn-buy" size="large" @click="buyNow">立即购买</el-button>
            </div>

            <div class="service-tags">
              <div class="service-item">
                <el-icon><Van /></el-icon>
                <span>满99元包邮</span>
              </div>
              <div class="service-item">
                <el-icon><CircleCheck /></el-icon>
                <span>品质保证</span>
              </div>
              <div class="service-item">
                <el-icon><CircleCheck /></el-icon>
                <span>绿色无公害</span>
              </div>
              <div class="service-item">
                <el-icon><Clock /></el-icon>
                <span>7天无理由退换</span>
              </div>
            </div>
          </div>

          <div class="favorite-button" @click="toggleFavorite">
            <el-icon class="favorite-icon" :class="{ 'is-filled': isFavorite }">
              <StarFilled v-if="isFavorite" />
              <Star v-else />
            </el-icon>
            <span class="favorite-text" :class="{ 'is-filled': isFavorite }">{{ isFavorite ? '已收藏' : '收藏' }}</span>
          </div>
        </div>
      </div>

      <!-- 农户信息 -->
      <div class="farmer-section" v-if="farmer.name">
        <div class="farmer-info">
          <el-avatar :size="64" :src="farmer.avatar || '/images/default-avatar.jpg'" />
          <div class="farmer-detail">
            <h3>{{ farmer.name }}</h3>
            <p><el-icon>
                <Location />
              </el-icon>{{ farmer.location }}</p>
          </div>
          <el-button type="primary" plain>进入店铺</el-button>
        </div>
      </div>

      <!-- 商品详情和评价 -->
      <el-tabs v-model="activeTab" class="detail-tabs">
        <el-tab-pane label="商品详情" name="detail">
          <!-- 有商品介绍时 -->
          <template v-if="product.introductionList && product.introductionList.length > 0">
            <div v-for="(item, index) in product.introductionList" :key="index" class="detail-wrapper" style="margin-bottom: 24px;">
              <!-- 文本+图片模式 -->
              <template v-if="item.type === 'text_image'">
                <div class="detail-content" v-if="item.content">
                  <div class="intro-title" v-if="item.title">{{ item.title }}</div>
                  {{ item.content }}
                </div>
                <div class="detail-image-box-wrapper" v-if="item.imageUrl">
                  <div class="detail-image-box-bg">
                    <el-image :src="getImageUrl(item.imageUrl)" fit="cover" style="width: 100%; height: 100%; border-radius: 34px;" :preview-src-list="[getImageUrl(item.imageUrl)]" preview-teleported />
                  </div>
                  <div class="detail-image-box"></div>
                </div>
              </template>
              <!-- 仅图片模式 -->
              <template v-else-if="item.type === 'image' && item.imageUrl">
                <div class="detail-image-only">
                  <el-image :src="getImageUrl(item.imageUrl)" fit="contain" style="max-width: 100%; height: auto;" :preview-src-list="[getImageUrl(item.imageUrl)]" preview-teleported />
                </div>
              </template>
            </div>
          </template>
          <!-- 无商品介绍时显示暂无介绍 -->
          <template v-else>
            <div style="display: flex; justify-content: center; align-items: center; padding: 60px 0;">
              <span style="font-size: 16px; color: #999;">暂无介绍</span>
            </div>
          </template>
        </el-tab-pane>
        <el-tab-pane label="用户评价" name="reviews">
          <!-- 评论输入框 -->
          <div class="review-input-section">
            <h4 class="review-input-title">发表评价</h4>
            <div class="review-input-box">
              <div class="review-rating-row">
                <span class="rating-label">评分</span>
                <el-rate v-model="newReview.rating" :colors="['#99A9BF', '#F7BA2A', '#FF9900']" />
              </div>
              <el-input
                v-model="newReview.content"
                type="textarea"
                :rows="4"
                placeholder="分享您的使用体验，帮助其他买家做出选择..."
                class="review-textarea"
              />
              <div class="review-actions">
                <el-upload
                  action="/api/file/upload"
                  list-type="picture-card"
                  :on-success="handleReviewImageSuccess"
                  :on-remove="handleReviewImageRemove"
                  :file-list="newReview.images"
                  class="review-upload"
                >
                  <el-icon><Plus /></el-icon>
                </el-upload>
                <el-button type="primary" class="submit-review-btn" @click="handleSubmitReview">
                  发布评价
                </el-button>
              </div>
            </div>
          </div>

          <!-- 评价筛选 -->
          <div class="review-filter-section">
            <div class="review-summary">
              <div class="rating-score">
                <span class="score">{{ averageRating }}</span>
                <el-rate :model-value="averageRating" disabled />
                <span class="total">{{ reviewStats.total }}条评价</span>
              </div>
            </div>
            <div class="filter-tabs">
              <button 
                v-for="tab in filterTabs" 
                :key="tab.value"
                :class="['filter-tab', { active: activeFilter === tab.value }]"
                @click="activeFilter = tab.value"
              >
                {{ tab.label }}
              </button>
            </div>
          </div>

          <!-- 评价列表 -->
          <div class="reviews-list" v-loading="reviewLoading">
            <div v-if="reviews.length === 0 && !reviewLoading" class="no-reviews">
              <el-empty description="暂无评价" />
            </div>
            <div v-for="review in reviews" :key="review.id" class="review-item">
              <div class="review-header">
                <el-avatar :size="40" :src="review.userAvatar || '/images/default-avatar.jpg'" />
                <span class="user-name">{{ review.userName }}</span>
                <el-rate :model-value="review.rating" disabled />
                <span class="review-time">{{ review.time }}</span>
              </div>
              <p class="review-content">{{ review.content }}</p>
              <div v-if="review.images && review.images.length > 0" class="review-images">
                <img v-for="img in review.images" :key="img" :src="getImageUrl(img)" />
              </div>
              <div class="review-footer">
                <span class="report-btn" @click="handleReportReview(review)">举报</span>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>

      <!-- 猜你喜欢 -->
      <div class="recommend-section">
        <div class="recommend-header">
          <div class="recommend-title">
            <span class="recommend-subtitle">精选推荐</span>
            <h3>猜你喜欢</h3>
          </div>
          <div class="recommend-nav">
            <button class="nav-btn" @click="scrollRecommend('left')">
              <el-icon><ArrowLeft /></el-icon>
            </button>
            <button class="nav-btn" @click="scrollRecommend('right')">
              <el-icon><ArrowRight /></el-icon>
            </button>
          </div>
        </div>
        <div class="recommend-list" ref="recommendListRef">
          <div class="recommend-item" v-for="item in displayedRecommendProducts" :key="item.id" @click="goToProduct(item.id)">
            <div class="recommend-image">
              <img :src="getImageUrl(item.mainImage || item.image)" :alt="item.name" />
            </div>
            <div class="recommend-info">
              <h4 class="recommend-name">{{ item.name }}</h4>
              <p class="recommend-price">¥{{ item.price?.toFixed(2) }}<span v-if="item.unit"> / {{ item.unit }}</span></p>
            </div>
          </div>
        </div>
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
    v-if="product.farmerId"
    ref="chatWindowRef"
    v-model:visible="chatVisible"
    :target-id="product.farmerId"
    :target-name="product.farmerName || '商家'"
    :target-avatar="product.farmerAvatar || ''"
    :shop-name="product.shopName"
    :is-buyer="true"
    :is-seller="false"
    @select-order="openOrderSelect"
  />
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Location, ShoppingCart, Star, StarFilled, Service, More, Document, DocumentCopy, EditPen, Warning, Van, CircleCheck, Clock, Plus, Shop } from '@element-plus/icons-vue'
import { getImageUrl } from '@/utils/image'
import { useCartStore } from '@/stores/cart'
import { useUserStore } from '@/stores/user'
import { productApi } from '@/api/product'
import { addFavorite, removeFavorite, checkFavorite } from '@/api/favorite'
import { getProductReviews, getReviewStats, addReview as submitReviewApi, reportReview } from '@/api/review'
import ChatWindow from '@/components/ChatWindow.vue'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()
const userStore = useUserStore()

const productId = route.params.id as string
const currentImage = ref('')
const quantity = ref(1)
const isFavorite = ref(false)
const activeTab = ref('detail')
const loading = ref(false)

const product = ref<any>({
  id: 0,
  name: '',
  description: '',
  price: 0,
  stock: 0,
  sales: 0,
  origin: '',
  mainImage: '',
  image: '',
  images: [],
  detail: '',
  farmerId: 0,
  farmerName: '',
  farmerAvatar: ''
})

// 聊天相关
const chatVisible = ref(false)
const chatWindowRef = ref<InstanceType<typeof ChatWindow>>()

// 解析图片列表（后端返回的是JSON字符串）
const parsedImages = computed(() => {
  if (!product.value.images) return []
  if (Array.isArray(product.value.images)) return product.value.images
  try {
    const parsed = JSON.parse(product.value.images)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
})

// 所有图片（主图+其他图片）用于缩略图展示
const allGalleryImages = computed(() => {
  const images: string[] = []
  const mainImg = product.value.mainImage || product.value.image
  if (mainImg) images.push(mainImg)
  parsedImages.value.forEach((img: string) => {
    if (img !== mainImg) images.push(img)
  })
  return images
})

// 预览图片列表（当前选中的图片放在第一位）
const previewImages = computed(() => {
  const allImages: string[] = []

  // 当前选中的图片放第一个
  if (currentImage.value) {
    allImages.push(currentImage.value)
  }

  // 其他图片（排除当前选中的）
  allGalleryImages.value.forEach((img: string) => {
    const imgUrl = getImageUrl(img)
    if (imgUrl !== currentImage.value) {
      allImages.push(imgUrl)
    }
  })

  return allImages.length > 0 ? allImages : ['/images/default-product.jpg']
})

const farmer = ref<any>({
  name: '',
  avatar: '',
  location: ''
})

// 新评价数据
const newReview = ref({
  rating: 5,
  content: '',
  images: [] as any[]
})

// 处理评价图片上传成功
const handleReviewImageSuccess = (response: any) => {
  console.log('上传响应:', response)
  // el-upload 返回的是原始响应，结构为 { code: 200, data: {...}, message: 'success' }
  if (response && response.code === 200 && response.data && response.data.url) {
    newReview.value.images.push({
      name: response.data.filename,
      url: response.data.url
    })
  }
}

// 处理评价图片移除
const handleReviewImageRemove = (file: any) => {
  const index = newReview.value.images.findIndex(img => img.url === file.url)
  if (index > -1) {
    newReview.value.images.splice(index, 1)
  }
}

// 提交评价
const handleSubmitReview = async () => {
  if (!newReview.value.content.trim()) {
    ElMessage.warning('请输入评价内容')
    return
  }

  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/consumer/login')
    return
  }

  try {
    await submitReviewApi({
      productId: productId,
      rating: newReview.value.rating,
      content: newReview.value.content,
      images: newReview.value.images.map((img: any) => img.url)
    })

    ElMessage.success('评价发布成功！')
    // 重置表单
    newReview.value.rating = 5
    newReview.value.content = ''
    newReview.value.images = []
    // 重新加载评价列表
    loadReviews()
    loadReviewStats()
  } catch (error) {
    console.error('发布评价失败:', error)
    ElMessage.error('评价发布失败')
  }
}

// 举报评价
const handleReportReview = async (review: any) => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/consumer/login')
    return
  }

  try {
    await reportReview(review.id, 4, '') // 4-其他类型
    ElMessage.success('举报成功，我们会尽快处理')
  } catch (error) {
    console.error('举报失败:', error)
    ElMessage.error('举报失败')
  }
}

// 评价筛选
const activeFilter = ref<string>('all')
const filterTabs = [
  { label: '全部', value: 'all' },
  { label: '好评', value: 'good' },
  { label: '中评', value: 'medium' },
  { label: '差评', value: 'bad' },
  { label: '图片/视频', value: 'media' }
]

// 平均评分
const averageRating = computed(() => {
  return reviewStats.value.average || '0.0'
})

// 监听筛选变化
watch(activeFilter, () => {
  reviewPage.value = 1
  loadReviews()
})

// 用户评价
const reviews = ref<any[]>([])
const reviewLoading = ref(false)
const reviewPage = ref(1)
const reviewSize = ref(10)
const reviewStats = ref<any>({
  average: '0.0',
  total: 0,
  distribution: {}
})

// 加载评价列表
const loadReviews = async () => {
  reviewLoading.value = true
  try {
    const data = await getProductReviews(productId, reviewPage.value, reviewSize.value, activeFilter.value)
    reviews.value = data || []
  } catch (error) {
    console.error('加载评价失败:', error)
  } finally {
    reviewLoading.value = false
  }
}

// 加载评价统计
const loadReviewStats = async () => {
  try {
    const data = await getReviewStats(productId)
    if (data) {
      reviewStats.value = data
    }
  } catch (error) {
    console.error('加载评价统计失败:', error)
  }
}

// 猜你喜欢 - 从API获取
const recommendProducts = ref<any[]>([])

const recommendListRef = ref<HTMLElement | null>(null)

// 滚动推荐列表
const scrollRecommend = (direction: 'left' | 'right') => {
  if (recommendListRef.value) {
    const scrollAmount = 300
    const currentScroll = recommendListRef.value.scrollLeft
    const newScroll = direction === 'left' 
      ? currentScroll - scrollAmount 
      : currentScroll + scrollAmount
    recommendListRef.value.scrollTo({ left: newScroll, behavior: 'smooth' })
  }
}

// 获取推荐商品（只取前4个）
const displayedRecommendProducts = computed(() => {
  return recommendProducts.value.slice(0, 4)
})

// 加载猜你喜欢数据
const loadRecommendProducts = async () => {
  try {
    const res: any = await productApi.getProductList({
      page: 1,
      size: 8
    })
    // 处理返回数据
    const list = res.list || res || []
    if (Array.isArray(list) && list.length > 0) {
      // 过滤掉当前商品，取前4个
      recommendProducts.value = list
        .filter((p: any) => String(p.id) !== String(productId))
        .slice(0, 4)
    }
  } catch (error) {
    console.error('加载推荐商品失败:', error)
  }
}

// 跳转到商品详情
const goToProduct = (id: number) => {
  router.push(`/consumer/product/${id}`)
}

// 跳转到店铺
const goToShop = () => {
  const farmerId = product.value.farmerId || product.value.farmer_id
  if (farmerId) {
    router.push(`/consumer/shop/${farmerId}`)
  } else {
    ElMessage.warning('无法进入店铺')
  }
}

// 获取商品详情
const fetchProductDetail = async () => {
  loading.value = true
  try {
    const res: any = await productApi.getProductDetail(Number(productId))
    if (res) {
      product.value = res
      // 设置当前显示的图片
      currentImage.value = getImageUrl(res.mainImage || res.image)
    }
  } catch (error) {
    console.error('获取商品详情失败:', error)
    ElMessage.error('获取商品详情失败')
  } finally {
    loading.value = false
  }
}

const addToCart = () => {
  // 未登录时提示登录
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  if (!product.value.id) {
    ElMessage.warning('商品信息加载中')
    return
  }
  cartStore.addItem({
    id: Date.now(),
    productId: Number(productId),
    productName: product.value.name,
    productImage: currentImage.value || getImageUrl(product.value.mainImage || product.value.image),
    price: product.value.price,
    unit: product.value.unit,
    quantity: quantity.value,
    stock: product.value.stock,
    farmerId: product.value.farmerId || 1,
    farmerName: farmer.value.name || ''
  })
  ElMessage.success('已加入购物车')
}

const buyNow = () => {
  // 未登录时提示登录
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  if (!product.value.id) {
    ElMessage.warning('商品信息加载中')
    return
  }
  // 直接跳转到确认订单页面，传递商品信息
  router.push({
    path: '/consumer/orders/create',
    query: {
      productId: productId,
      quantity: quantity.value
    }
  })
}

// 检查是否已收藏
const checkIsFavorite = async () => {
  // 未登录时不检查收藏状态
  if (!userStore.isLoggedIn) {
    isFavorite.value = false
    return
  }
  try {
    const res: any = await checkFavorite(Number(productId))
    isFavorite.value = res?.isFavorite || false
  } catch (error) {
    console.error('检查收藏状态失败:', error)
  }
}

// 切换收藏状态
const toggleFavorite = async () => {
  // 未登录时提示登录
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  try {
    if (isFavorite.value) {
      await removeFavorite(Number(productId))
      isFavorite.value = false
      ElMessage.success('已取消收藏')
    } else {
      await addFavorite(Number(productId))
      isFavorite.value = true
      ElMessage.success('已收藏')
    }
  } catch (error) {
    console.error('收藏操作失败:', error)
    ElMessage.error('操作失败，请稍后重试')
  }
}

// 联系客服
const contactService = () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  if (!product.value.farmerId) {
    ElMessage.warning('无法联系商家')
    return
  }
  chatVisible.value = true
}

// 选择订单 - 直接发送当前商品信息
const openOrderSelect = () => {
  // 构建当前商品的订单卡片信息
  const currentProductOrder = {
    id: product.value.id,
    orderNo: `咨询商品-${product.value.id}`,
    productImage: product.value.mainImage || product.value.image,
    productName: product.value.name,
    price: product.value.price,
    unit: product.value.unit || '',
    status: 0,
    statusText: '咨询中',
    // 包含商品详细信息
    productId: product.value.id,
    productDesc: product.value.description
  }
  
  // 直接发送订单卡片
  chatWindowRef.value?.sendOrderCard(currentProductOrder)
}

// 显示更多
const showMore = () => {
  // 点击更多按钮，菜单通过 CSS hover 显示
}

// 复制链接
const copyLink = () => {
  const url = window.location.href
  navigator.clipboard.writeText(url).then(() => {
    ElMessage.success('链接已复制')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

// 打开反馈
const openFeedback = () => {
  ElMessage.info('反馈功能即将上线')
}

// 打开举报
const openReport = () => {
  // 未登录时提示登录
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  router.push(`/product/${productId}/report`)
}

// 用户调研
const openSurvey = () => {
  ElMessage.info('用户调研功能即将上线')
}

onMounted(() => {
  fetchProductDetail()
  checkIsFavorite()
  loadReviews()
  loadReviewStats()
  loadRecommendProducts()
})
</script>

<style scoped>
.product-detail-view {
  max-width: 1500px;
  margin: 0 auto;
  padding: 24px;
  background: #fff;
  min-height: calc(100vh - 64px);
  box-shadow: 0 0 0 100vmax #fff;
  clip-path: inset(0 -100vmax);
}

.breadcrumb {
  margin-bottom: 24px;
}

.product-main {
  display: grid;
  grid-template-columns: 720px 1fr;
  gap: 40px;
  margin-bottom: 40px;
}

.product-gallery {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.main-image {
  width: 700px;
  height: 700px;
  border-radius: 12px;
  overflow: hidden;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
}

.main-image img,
.main-image-el {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.main-image-el :deep(.el-image__inner) {
  width: 100%;
  height: 100%;
  object-fit: cover;
  cursor: zoom-in;
}

.thumbnail-list {
  display: flex;
  gap: 12px;
}

.thumbnail {
  width: 132px;
  height: 132px;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.3s;
}

.thumbnail.active {
  border-color: var(--apple-blue);
}

.thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-info {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.product-name {
  font-size: 34px;
  font-weight: 700;
  color: var(--apple-dark-gray);
}

.product-desc {
  font-size: 14px;
  color: var(--apple-gray);
  line-height: 1.6;
}

.shop-info-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  background: #f5f5f7;
  border-radius: 8px;
  width: fit-content;
}

.shop-label {
  font-size: 13px;
  color: #86868b;
}

.shop-name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 500;
  color: #007aff;
  cursor: pointer;
  transition: all 0.3s;
}

.shop-name:hover {
  color: #0056b3;
  text-decoration: underline;
}

.shop-name .el-icon {
  font-size: 16px;
}

.price-box {
  display: flex;
  align-items: center;
  gap: 16px;
}

.price-value {
  font-size: 45px;
  font-weight: 700;
  color: #007aff;
}

.price-symbol {
  font-weight: 400;
  font-size: 20px;
  margin-right: 2px;
}

.price-unit {
  font-size: 16px;
  font-weight: 400;
  color: #666;
  margin-left: 10px;
}

.info-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.info-row.location-sales {
  margin-top: 8px;
  margin-bottom: 12px;
  gap: 12px;
}

.info-row.location-sales .divider {
  color: #ccc;
  font-size: 14px;
}

.info-value {
  font-size: 14px;
  color: var(--apple-dark-gray);
  display: flex;
  align-items: center;
  gap: 4px;
}

.quantity-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stock-hint {
  font-size: 14px;
  color: var(--apple-gray);
}

.action-buttons {
  display: flex;
  gap: 16px;
  margin-top: 20px;
}

.action-buttons .el-button {
  flex: 1;
}

.favorite-button {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 8px;
  margin-top: 16px;
  padding: 12px 0;
  cursor: pointer;
  transition: all 0.3s ease;
}

.favorite-button:hover {
  opacity: 0.7;
}

.favorite-icon {
  font-size: 18px;
  color: var(--apple-dark-gray);
}

.favorite-button:hover .favorite-icon {
  color: #ff9500;
}

.favorite-button .favorite-icon.is-filled {
  color: #ff9500;
}

.favorite-text {
  font-size: 14px;
  color: var(--apple-dark-gray);
  letter-spacing: 1px;
}

.favorite-text.is-filled {
  color: #ff9500;
}

.purchase-box {
  background: #f5f5f5;
  border-radius: 16px;
  padding: 32px;
  margin-top: 20px;
}

.quantity-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.quantity-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--apple-dark-gray);
  text-transform: uppercase;
  letter-spacing: 1px;
}

/* 核心修改：完全对齐加减按钮和数字 */
.quantity-row :deep(.el-input-number) {
  background: #fff;
  border-radius: 50px;
  overflow: hidden;
  border: 1px solid #e0e0e0;
  height: 48px;
  display: flex;
  align-items: center;
}

.quantity-row :deep(.el-input-number .el-input__wrapper) {
  background: #fff;
  box-shadow: none !important;
  height: 100%;
  display: flex;
  align-items: center;
  padding: 0 8px;
}

.quantity-row :deep(.el-input-number .el-input__wrapper.is-focus) {
  box-shadow: none !important;
}

.quantity-row :deep(.el-input-number .el-input__inner) {
  text-align: center;
  font-size: 16px;
  font-weight: 500;
  height: 100%;
  line-height: 48px;
  padding: 0;
}

.quantity-row :deep(.el-input-number__decrease),
.quantity-row :deep(.el-input-number__increase) {
  background: transparent;
  border: none;
  font-size: 16px;
  color: #333;
  height: 48px;
  width: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0;
  padding: 0;
  line-height: 1;
}

.quantity-row :deep(.el-input-number__decrease:hover),
.quantity-row :deep(.el-input-number__increase:hover) {
  color: #333;
  background: #f0f0f0 !important;
}
/* 核心修改结束 */

.action-buttons {
  display: flex;
  gap: 16px;
  margin-top: 32px;
}

.action-buttons .el-button {
  flex: 1;
  height: 56px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 500;
  border: none;
}

.action-buttons .btn-cart {
  background: #87a2cd;
  color: #fff;
}

.action-buttons .btn-cart:hover {
  background: #7092c8;
}

.action-buttons .btn-buy {
  background: #007aff;
  color: #fff;
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.25);
}

.action-buttons .btn-buy:hover {
  background: #3597ff;
  box-shadow: 0 6px 16px rgba(0, 122, 255, 0.35);
}

.service-tags {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px 24px;
  margin-top: 32px;
  padding-top: 28px;
  border-top: 1px solid #e0e0e0;
}

.service-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #666;
}

.service-item .el-icon {
  font-size: 16px;
  color: #34c759;
}

.farmer-section {
  background: var(--apple-card-bg);
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 40px;
}

.farmer-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.farmer-detail {
  flex: 1;
}

.farmer-detail h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--apple-dark-gray);
  margin-bottom: 4px;
}

.farmer-detail p {
  font-size: 14px;
  color: var(--apple-gray);
  display: flex;
  align-items: center;
  gap: 4px;
}

.detail-tabs {
  background: var(--apple-card-bg);
  border-radius: 12px;
  padding: 24px 0;
}

.detail-wrapper {
  display: flex;
  gap: 24px;
  padding: 20px 0;
}

.detail-content {
  flex: 1;
  font-size: 16px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  line-height: 1.8;
  color: var(--apple-dark-gray);
  text-indent: 2em;
}

.detail-image-box-wrapper {
  position: relative;
  width: 670px;
  height: 900px;
  flex-shrink: 0;
  margin-right: 24px;
}

.detail-image-box-wrapper:hover .detail-image-box {
  transform: rotate(1deg);
}

.detail-image-box-wrapper:hover .detail-image-box-bg {
  transform: translate(-50%, -50%) rotate(2deg);
}

.detail-image-box-bg {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 650px;
  height: 870px;
  border-radius: 34px;
  background: #83bfff;
  z-index: 2;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12), 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: transform 0.3s ease;
}

.detail-image-box {
  position: absolute;
  top: 0;
  left: 0;
  width: 670px;
  height: 900px;
  border-radius: 34px;
  background: #83bfff;
  z-index: 1;
  transform: rotate(2deg);
  transition: transform 0.3s ease;
}

.detail-image-only {
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px 0;
}

.intro-title {
  font-size: 20px;
  font-weight: 600;
  color: #007aff;
  margin-bottom: 12px;
  text-indent: 0;
}

.reviews-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 20px 0;
}

/* 猜你喜欢 */
.recommend-section {
  margin-top: 48px;
  padding: 0 24px;
}

.recommend-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 24px;
}

.recommend-title {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.recommend-subtitle {
  font-size: 12px;
  color: #999;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.recommend-title h3 {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.recommend-nav {
  display: flex;
  gap: 12px;
}

.nav-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 1px solid #e0e0e0;
  background: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.nav-btn:hover {
  background: #f5f5f5;
  border-color: #ccc;
}

.recommend-list {
  display: flex;
  gap: 24px;
  overflow-x: auto;
  scroll-behavior: smooth;
  padding-bottom: 8px;
  padding-top: 10px;
}

.recommend-list::-webkit-scrollbar {
  display: none;
}

.recommend-item {
  flex-shrink: 0;
  width: 345px;
  cursor: pointer;
  transition: transform 0.3s;
}

.recommend-item:hover {
  transform: translateY(-4px);
}

.recommend-image {
  width: 345px;
  height: 445px;
  border-radius: 16px;
  overflow: hidden;
  background: #f5f5f5;
  margin-bottom: 12px;
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.recommend-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.recommend-info {
  padding: 0 4px;
}

.recommend-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin: 0 0 6px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.recommend-price {
  font-size: 13px;
  color: #666;
  margin: 0;
}

/* 评价筛选区域 */
.review-filter-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 0;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 24px;
}

.review-summary {
  display: flex;
  align-items: center;
  gap: 16px;
}

.rating-score {
  display: flex;
  align-items: center;
  gap: 12px;
}

.rating-score .score {
  font-size: 32px;
  font-weight: 700;
  color: #ff9900;
}

.rating-score .total {
  font-size: 14px;
  color: #999;
}

.filter-tabs {
  display: flex;
  gap: 12px;
}

.filter-tab {
  padding: 8px 20px;
  border-radius: 20px;
  border: 1px solid #e0e0e0;
  background: #fff;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.3s;
}

.filter-tab:hover {
  border-color: #007aff;
  color: #007aff;
}

.filter-tab.active {
  background: #007aff;
  border-color: #007aff;
  color: #fff;
}

/* 评价输入区域 */
.review-input-section {
  background: #fafafa;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 32px;
}

.review-input-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0 0 16px 0;
}

.review-input-box {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
}

.review-rating-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.rating-label {
  font-size: 14px;
  color: #666;
}

.review-textarea :deep(.el-textarea__inner) {
  border-radius: 8px;
  resize: none;
}

.review-actions {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-top: 16px;
}

.review-upload :deep(.el-upload--picture-card) {
  width: 80px;
  height: 80px;
  border-radius: 8px;
}

.review-upload :deep(.el-upload-list__item) {
  width: 80px;
  height: 80px;
  border-radius: 8px;
}

.submit-review-btn {
  background: #007aff;
  border: none;
  padding: 12px 32px;
  font-size: 14px;
  border-radius: 8px;
}

.submit-review-btn:hover {
  background: #3597ff;
}

.reviews-list {
  display: flex;
  flex-direction: column;
  gap: 0;
  padding: 0;
}

.review-item {
  padding: 24px 0;
  border-bottom: 1px solid #f0f0f0;
}

.review-item:last-child {
  border-bottom: none;
}

.review-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.user-name {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.no-reviews {
  padding: 60px 0;
}

.review-time {
  font-size: 13px;
  color: #999;
  margin-left: auto;
}

.review-content {
  font-size: 14px;
  color: #555;
  line-height: 1.8;
  margin-bottom: 16px;
  padding-left: 52px;
}

.review-images {
  display: flex;
  gap: 12px;
  padding-left: 52px;
}

.review-images img {
  width: 120px;
  height: 120px;
  border-radius: 12px;
  object-fit: cover;
  cursor: pointer;
  transition: transform 0.3s;
}

.review-images img:hover {
  transform: scale(1.05);
}

.review-footer {
  display: flex;
  justify-content: flex-end;
  padding-left: 52px;
  margin-top: 12px;
}

.report-btn {
  font-size: 13px;
  color: #999;
  cursor: pointer;
  transition: color 0.3s;
}

.report-btn:hover {
  color: #ff4d4f;
}

@media (max-width: 768px) {
  .product-main {
    grid-template-columns: 1fr;
  }

  .main-image {
    width: 100%;
    height: 300px;
  }

  .thumbnail-list {
    justify-content: center;
  }

  .floating-actions {
    display: none;
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
  transition: all 0.2s ease;
  z-index: 1000;
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
  transition: background 0.2s;
  font-size: 13px;
  color: #333;
  white-space: nowrap;
}

.more-menu-item:hover {
  background: #f5f5f5;
}

.more-menu-item .el-icon {
  font-size: 16px;
  color: #666;
}
</style>