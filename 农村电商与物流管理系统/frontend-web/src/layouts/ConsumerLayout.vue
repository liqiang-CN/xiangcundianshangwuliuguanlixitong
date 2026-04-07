<template>
  <div class="consumer-layout">
    <header class="header">
      <div class="header-content">
        <div class="header-left">
          <div class="logo" @click="$router.push('/')">
            <img v-if="siteLogo" :src="siteLogo" class="site-logo" />
            <el-icon v-else size="32" color="#007aff"><Shop /></el-icon>
            <span class="logo-text">{{ siteName }}</span>
          </div>
        </div>
        <nav class="nav-menu">
          <router-link to="/" class="nav-item" exact-active-class="active">首页</router-link>
          <router-link to="/products" class="nav-item" active-class="active">商品</router-link>
          <span class="nav-item" :class="{ active: $route.path === '/orders' }" @click="handleOrdersClick">订单</span>
        </nav>
        <div class="header-actions">
          <!-- 搜索框 -->
          <div class="header-search" v-click-outside="closeDropdown">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索商品..."
              clearable
              size="small"
              @keyup.enter="handleSearch"
              @clear="handleSearch"
              @focus="showDropdown = true"
            >
              <template #append>
                <el-button type="primary" @click="handleSearch">
                  <el-icon><Search /></el-icon>
                </el-button>
              </template>
            </el-input>
            <!-- 搜索下拉框 -->
            <Transition name="dropdown">
              <div v-if="showDropdown" class="search-dropdown" @mousedown.prevent>
                <div class="dropdown-section">
                  <div class="section-title">搜索类型</div>
                  <div class="type-options">
                    <div 
                      class="type-option" 
                      :class="{ active: searchType === 'product' }"
                      @click="selectType('product')"
                    >
                      <span>搜索商品</span>
                    </div>
                    <div 
                      class="type-option" 
                      :class="{ active: searchType === 'shop' }"
                      @click="selectType('shop')"
                    >
                      <span>搜索店铺</span>
                    </div>
                  </div>
                </div>
                <div class="dropdown-section">
                  <div class="section-title">热门搜索</div>
                  <div class="hot-tags">
                    <span 
                      v-for="tag in hotTags" 
                      :key="tag"
                      class="hot-tag"
                      @click="selectHotTag(tag)"
                    >
                      {{ tag }}
                    </span>
                  </div>
                </div>
              </div>
            </Transition>
          </div>
          <!-- 消息图标 -->
          <el-badge v-if="userStore.isLoggedIn && unreadCount > 0" :value="unreadCount" class="message-badge">
            <el-icon size="24" class="action-icon" @click="$router.push('/messages')"><Bell /></el-icon>
          </el-badge>
          <el-icon v-else-if="userStore.isLoggedIn" size="24" class="action-icon" @click="$router.push('/messages')"><Bell /></el-icon>
          
          <el-badge v-if="cartStore.totalCount > 0" :value="cartStore.totalCount" class="cart-badge">
            <el-icon size="24" class="action-icon" @click="$router.push('/cart')"><ShoppingCart /></el-icon>
          </el-badge>
          <el-icon v-else size="24" class="action-icon" @click="$router.push('/cart')"><ShoppingCart /></el-icon>
          <el-dropdown v-if="userStore.isLoggedIn">
            <div class="user-info">
              <el-avatar :size="36" :src="userStore.userInfo?.avatar" />
              <span class="username">{{ userStore.userInfo?.nickname }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="$router.push('/profile')">个人中心</el-dropdown-item>
                <el-dropdown-item @click="$router.push('/orders')">我的订单</el-dropdown-item>
                <el-dropdown-item @click="$router.push('/favorites')">我的收藏</el-dropdown-item>
                <el-dropdown-item @click="$router.push('/followed-shops')">关注店铺</el-dropdown-item>
                <el-dropdown-item @click="$router.push('/messages')">
                  消息中心
                  <el-badge v-if="unreadCount > 0" :value="unreadCount" class="menu-badge" />
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <span v-else class="login-text" @click="$router.push('/login')">登录</span>
        </div>
      </div>
    </header>
    <main class="main-content">
      <router-view />
    </main>
    <footer class="footer">
      <div class="footer-content">
        <div class="footer-main">
          <div class="footer-brand">
            <h3>{{ siteName }}</h3>
            <p>致力于连接城乡，让新鲜农产品从田间直达餐桌。从新疆高原到热带山谷，为您甄选最优质的农产品。</p>
          </div>
          <div class="footer-links">
            <div class="footer-column">
              <h4>客户服务</h4>
              <ul>
                <li><a href="#">售后保障</a></li>
                <li><a href="#">配送说明</a></li>
                <li><a href="#">退换货政策</a></li>
              </ul>
            </div>
            <div class="footer-column">
              <h4>法律条款</h4>
              <ul>
                <li><a href="#">隐私政策</a></li>
                <li><a href="#">服务条款</a></li>
                <li><a href="#">商家入驻</a></li>
              </ul>
            </div>
            <div class="footer-column">
              <h4>订阅资讯</h4>
              <p class="subscribe-text">订阅我们的资讯，获取最新优惠信息</p>
              <div class="subscribe-form">
                <input type="email" placeholder="输入邮箱地址" />
                <button><el-icon><ArrowRight /></el-icon></button>
              </div>
            </div>
          </div>
        </div>
        <div class="footer-bottom">
          <p>© 2026 {{ siteName }} - 农村电商与物流配送系统</p>
          <div class="footer-contact" v-if="servicePhone">
            <span class="contact-label">客服电话：</span>
            <span class="contact-phone">{{ servicePhone }}</span>
          </div>
          <div class="footer-social">
            <a href="#"><el-icon><ChatDotRound /></el-icon></a>
            <a href="#"><el-icon><Phone /></el-icon></a>
          </div>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Shop, ShoppingCart, Search, ArrowRight, ChatDotRound, Phone, Bell } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { onMounted, ref, computed, onUnmounted } from 'vue'
import request from '@/api/request'
import { vClickOutside } from '../directives/clickOutside'
import { getImageUrl } from '@/utils/image'
import { messageApi, type UnreadCount } from '@/api/message'

const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()

// 未读消息数量
const unreadCounts = ref<UnreadCount>({
  total: 0,
  system: 0,
  product: 0,
  order: 0,
  chat: 0
})
const unreadCount = computed(() => unreadCounts.value.total)
let messageTimer: NodeJS.Timeout | null = null

// 获取未读消息数量
const fetchUnreadCount = async () => {
  if (!userStore.isLoggedIn) return
  try {
    const res: any = await messageApi.getUnreadCount()
    // request 拦截器已经提取了 data
    if (res) {
      unreadCounts.value = res
    }
  } catch (error) {
    console.error('获取未读消息数量失败:', error)
  }
}

// 启动定时器轮询未读消息
const startMessagePolling = () => {
  fetchUnreadCount()
  messageTimer = setInterval(fetchUnreadCount, 30000) // 30秒轮询一次
}

// 站点配置
const siteName = ref('农鲜达')
const siteLogo = ref('')
const servicePhone = ref('')

// 获取站点配置
const fetchSiteConfig = async () => {
  try {
    const config = await request.get('/admin/config') as Record<string, string>
    siteName.value = config['site.name'] || '农鲜达'
    siteLogo.value = getImageUrl(config['site.logo'])
    servicePhone.value = config['contact.phone'] || ''
  } catch (error) {
    console.error('获取站点配置失败:', error)
  }
}

// 搜索相关
const searchKeyword = ref('')
const searchType = ref('product')
const showDropdown = ref(false)

// 热搜标签
const productHotTags = ref(['苹果', '橙子', '农家土鸡蛋', '新鲜蔬菜', '有机大米', '土特产'])
const shopHotTags = ref(['水果批发', '生态农场', '土特产的店', '有机蔬菜基地', '果园直发', '农家自产'])

// 根据搜索类型获取对应的热搜标签
const hotTags = computed(() => {
  return searchType.value === 'product' ? productHotTags.value : shopHotTags.value
})

// 关闭下拉框
const closeDropdown = () => {
  showDropdown.value = false
}

// 选择搜索类型
const selectType = (type: string) => {
  searchType.value = type
  // 保持下拉框打开，不关闭
}

// 选择热搜标签
const selectHotTag = (tag: string) => {
  searchKeyword.value = tag
  handleSearch()
}

// 搜索
const handleSearch = () => {
  if (!searchKeyword.value.trim()) return
  showDropdown.value = false
  
  if (searchType.value === 'product') {
    router.push({
      path: '/consumer/products',
      query: { keyword: searchKeyword.value }
    })
  } else {
    router.push({
      path: '/consumer/shops',
      query: { keyword: searchKeyword.value }
    })
  }
}

// 获取用户信息
const fetchUserInfo = async () => {
  if (!userStore.isLoggedIn) return
  
  try {
    const res: any = await request.get('/user/info')
    if (res) {
      userStore.setUserInfo({
        id: res.id,
        username: res.username,
        nickname: res.nickname || res.username,
        avatar: res.avatar || '',
        phone: res.phone || '',
        email: res.email || '',
        userType: res.userType === 0 ? 'consumer' : res.userType === 1 ? 'farmer' : 'logistics',
        status: res.status
      })
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
  }
}

onMounted(() => {
  userStore.initUserInfo()
  fetchUserInfo()
  fetchSiteConfig()
  startMessagePolling()
})

onUnmounted(() => {
  if (messageTimer) {
    clearInterval(messageTimer)
  }
})

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    userStore.logout()
    window.location.href = '/login'
  })
}

const handleOrdersClick = () => {
  if (userStore.isLoggedIn) {
    router.push('/orders')
  } else {
    ElMessage.warning('请先登录')
    router.push('/login')
  }
}
</script>

<style scoped>
.consumer-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid var(--apple-border);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 24px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
  transition: height 0.3s ease, padding 0.3s ease;
}

.header-left {
  display: flex;
  align-items: center;
  min-width: 200px;
  transition: min-width 0.3s ease;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: gap 0.3s ease;
}

.logo-text {
  font-size: 24px;
  font-weight: 700;
  background: linear-gradient(135deg, #007aff, #34c759);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  transition: font-size 0.3s ease;
}

.site-logo {
  width: 32px;
  height: 32px;
  object-fit: contain;
  border-radius: 4px;
}

.nav-menu {
  display: flex;
  gap: clamp(12px, 4vw, 48px);
  justify-content: center;
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  transition: gap 0.3s ease;
}

.nav-item {
  font-size: 16px;
  font-weight: 500;
  color: var(--apple-dark-gray);
  text-decoration: none;
  padding: 8px 16px;
  border-radius: 8px;
  transition: all 0.3s ease;
  cursor: pointer;
}

.nav-item:hover {
  color: var(--apple-blue);
}

.nav-item.active {
  color: var(--apple-blue);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 20px;
  justify-content: flex-end;
  min-width: 200px;
  transition: gap 0.3s ease, min-width 0.3s ease;
}

/* 响应式 - 大屏幕缩小 */
@media (max-width: 1200px) {
  .header-content {
    height: 56px;
    padding: 0 20px;
  }
  
  .header-left {
    min-width: 160px;
  }
  
  .logo-text {
    font-size: 20px;
  }
  
  .nav-item {
    font-size: 15px;
    padding: 6px 12px;
  }
  
  .header-actions {
    gap: 16px;
    min-width: 160px;
  }
  
}

/* 响应式 - 平板 */
@media (max-width: 992px) {
  .header-content {
    height: 52px;
    padding: 0 16px;
  }
  
  .header-left {
    min-width: 140px;
  }
  
  .logo {
    gap: 6px;
  }
  
  .logo-text {
    font-size: 18px;
  }
  
  .nav-item {
    font-size: 14px;
    padding: 6px 10px;
  }
  
  .header-actions {
    gap: 12px;
    min-width: 140px;
  }
}

/* 响应式 - 手机 */
@media (max-width: 768px) {
  .header-content {
    height: 48px;
    padding: 0 12px;
  }
  
  .header-left {
    min-width: auto;
  }
  
  .logo-text {
    font-size: 16px;
  }
  
  .nav-menu {
    position: static;
    transform: none;
    margin: 0 auto;
    gap: clamp(8px, 3vw, 16px);
  }
  
  .nav-item {
    font-size: 13px;
    padding: 4px 6px;
  }
  
  .header-actions {
    gap: 8px;
    min-width: auto;
  }
  
  .username {
    display: none;
  }
}

.header-search {
  width: clamp(80px, 15vw, 200px);
  transition: width 0.3s ease;
  position: relative;
}

.header-search:focus-within {
  width: clamp(100px, 20vw, 280px);
}

.header-search :deep(.el-input__wrapper) {
  border-radius: 20px 0 0 20px;
  box-shadow: none;
  border: 1px solid #e5e5e5;
  border-right: none;
  padding-left: 12px;
  height: 32px;
  transition: border-color 0.3s;
}

.header-search:focus-within :deep(.el-input__wrapper) {
  border-color: #007aff;
}

.header-search :deep(.el-input__inner) {
  font-size: 14px;
}

.header-search :deep(.el-input-group__append) {
  border-radius: 0 20px 20px 0;
  background: #007aff;
  border: 1px solid transparent;
  border-left: none;
  padding: 0;
  height: 32px;
  transition: border-color 0.3s;
}

.header-search:focus-within :deep(.el-input-group__append) {
  border-color: #007aff;
}

/* 搜索下拉框 */
.search-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  right: 0;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  padding: 16px;
  z-index: 1000;
  min-width: 280px;
}

/* 下拉框过渡动画 - 从上往下展开，从上往下收起 */
.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  transform-origin: top center;
}

.dropdown-enter-from {
  opacity: 0;
  transform: scaleY(0);
}

.dropdown-enter-to {
  opacity: 1;
  transform: scaleY(1);
}

.dropdown-leave-from {
  opacity: 1;
  transform: scaleY(1);
}

.dropdown-leave-to {
  opacity: 0;
  transform: scaleY(0);
}

.dropdown-section {
  margin-bottom: 16px;
}

.dropdown-section:last-child {
  margin-bottom: 0;
}

.dropdown-section .section-title {
  font-size: 12px;
  color: #999;
  margin-bottom: 12px;
  font-weight: 500;
}

.type-options {
  display: flex;
  gap: 12px;
}

.type-option {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  background: #f5f5f7;
  flex: 1;
  font-size: 12px;
}

.type-option:hover {
  background: #e8e8ed;
}

.type-option.active {
  background: #007aff;
  color: white;
}

.hot-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hot-tag {
  padding: 6px 12px;
  background: #f5f5f7;
  border-radius: 16px;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  transition: all 0.3s;
}

.hot-tag:hover {
  background: #007aff;
  color: white;
}

.header-search :deep(.el-input-group__append .el-button) {
  color: white !important;
  font-weight: 500;
  font-size: 14px;
  padding: 0 12px;
  border: none !important;
  background: transparent !important;
  width: 40px;
  height: 100%;
  margin: 0;
}

.action-icon {
  cursor: pointer;
  color: var(--apple-dark-gray);
  transition: color 0.3s;
}

.action-icon:hover {
  color: var(--apple-blue);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 12px;
  border-radius: 20px;
  transition: background 0.3s;
}

.user-info:hover {
  background: var(--apple-light-gray);
}

.username {
  font-size: 14px;
  color: var(--apple-dark-gray);
}

.login-text {
  font-size: 14px;
  color: #000;
  cursor: pointer;
  padding: 8px 16px;
  transition: color 0.3s;
}

.login-text:hover {
  color: var(--apple-blue);
}

.main-content {
  flex: 1;
  background: var(--apple-bg);
}

.footer {
  background: #fff;
  border-top: 1px solid var(--apple-border);
  padding: 60px 40px 24px;
}

.footer-content {
  max-width: 1400px;
  margin: 0 auto;
}

.footer-main {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 80px;
  margin-bottom: 48px;
}

.footer-brand h3 {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin: 0 0 16px 0;
}

.footer-brand p {
  font-size: 14px;
  color: #666;
  line-height: 1.8;
  margin: 0;
}

.footer-links {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 40px;
}

.footer-column h4 {
  font-size: 13px;
  font-weight: 600;
  color: #333;
  text-transform: uppercase;
  letter-spacing: 1px;
  margin: 0 0 20px 0;
}

.footer-column ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.footer-column li {
  margin-bottom: 12px;
}

.footer-column a {
  font-size: 14px;
  color: #666;
  text-decoration: none;
  transition: color 0.3s;
}

.footer-column a:hover {
  color: #333;
}

.subscribe-text {
  font-size: 13px;
  color: #999;
  margin: 0 0 12px 0;
}

.subscribe-form {
  display: flex;
  border-bottom: 1px solid #ddd;
}

.subscribe-form input {
  flex: 1;
  border: none;
  background: transparent;
  padding: 8px 0;
  font-size: 14px;
  outline: none;
}

.subscribe-form input::placeholder {
  color: #999;
}

.subscribe-form button {
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 8px;
  color: #333;
}

.footer-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 24px;
  border-top: 1px solid #eee;
}

.footer-bottom p {
  font-size: 12px;
  color: #999;
  margin: 0;
}

.footer-contact {
  display: flex;
  align-items: center;
  gap: 8px;
}

.contact-label {
  font-size: 12px;
  color: #666;
}

.contact-phone {
  font-size: 14px;
  color: #666;
}

.footer-social {
  display: flex;
  gap: 16px;
}

.footer-social a {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  text-decoration: none;
  transition: all 0.3s;
}

.footer-social a:hover {
  background: #333;
  color: #fff;
}
</style>
