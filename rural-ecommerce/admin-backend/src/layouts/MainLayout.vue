<template>
  <el-container class="main-layout">
    <el-aside width="220px" class="sidebar">
      <div class="logo" @click="$router.push('/')">
        <img v-if="siteLogo" :src="siteLogo" class="site-logo" />
        <el-icon v-else size="32" color="#007aff"><Shop /></el-icon>
        <span class="logo-text">{{ siteName }}</span>
      </div>
      <div class="sidebar-menu-wrapper">
        <el-menu
          :default-active="$route.path"
          router
          class="sidebar-menu"
          background-color="#001529"
          text-color="#a6adb4"
          active-text-color="#fff"
        >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>数据概览</span>
        </el-menu-item>
        
        <el-sub-menu index="/products">
          <template #title>
            <el-icon><Goods /></el-icon>
            <span>商品管理</span>
          </template>
          <el-menu-item index="/products">商品列表</el-menu-item>
          <el-menu-item index="/product-review">商品审核</el-menu-item>
          <el-menu-item index="/review-manage">评价管理</el-menu-item>
          <el-menu-item index="/review-reports">评价举报</el-menu-item>
          <el-menu-item index="/categories">分类管理</el-menu-item>
        </el-sub-menu>
        
        <el-sub-menu index="/orders">
          <template #title>
            <el-icon><List /></el-icon>
            <span>订单物流</span>
          </template>
          <el-menu-item index="/orders">订单管理</el-menu-item>
          <el-menu-item index="/logistics">物流调度</el-menu-item>
          <el-menu-item index="/delivery-staff">配送员管理</el-menu-item>
        </el-sub-menu>
        
        <el-sub-menu index="/users">
          <template #title>
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </template>
          <el-menu-item index="/shops">店铺管理</el-menu-item>
          <el-menu-item index="/farmers">农户管理</el-menu-item>
          <el-menu-item index="/consumers">用户管理</el-menu-item>
          <el-menu-item index="/admins">管理员管理</el-menu-item>
        </el-sub-menu>
        
        <el-sub-menu index="/config">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统配置</span>
          </template>
          <el-menu-item index="/banners">轮播图管理</el-menu-item>
          <el-menu-item index="/settings">系统设置</el-menu-item>
          <el-menu-item index="/logs">操作日志</el-menu-item>
        </el-sub-menu>
        
        <el-menu-item index="/chat-records">
          <el-icon><ChatDotRound /></el-icon>
          <span>聊天记录</span>
        </el-menu-item>
        </el-menu>
      </div>
    </el-aside>
    
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <breadcrumb />
        </div>
        <div class="header-right">
          <el-badge :value="3" class="message-badge">
            <el-icon size="20"><Bell /></el-icon>
          </el-badge>
          <el-dropdown>
            <div class="user-info">
              <el-avatar :size="32" :src="getImageUrl(userStore.userInfo?.avatar, '/images/default-avatar.jpg')" />
              <span class="username">{{ userStore.userInfo?.nickname }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item>个人中心</el-dropdown-item>
                <el-dropdown-item>修改密码</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      
      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { Shop, Odometer, Goods, List, User, Setting, Bell, ArrowDown } from '@element-plus/icons-vue'
import request from '@/api/request'
import { getImageUrl } from '@/utils/image'

const userStore = useUserStore()
const router = useRouter()
const route = useRoute()

const siteName = ref('农鲜达')
const siteLogo = ref('')

// 获取系统配置
const fetchSiteConfig = async () => {
  try {
    const config = await request.get('/admin/config') as Record<string, string>
    siteName.value = config['site.name'] || '农鲜达'
    siteLogo.value = getImageUrl(config['site.logo'])
  } catch (error) {
    console.error('获取站点配置失败:', error)
  }
}

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    userStore.logout()
    router.push('/login')
  })
}

onMounted(() => {
  fetchSiteConfig()
})
</script>

<style scoped>
.main-layout {
  min-height: 100vh;
}

.sidebar {
  background: #001529;
  position: fixed;
  left: 0;
  top: 0;
  height: 100vh;
  display: flex;
  flex-direction: column;
  z-index: 1000;
}

.sidebar-menu-wrapper {
  flex: 1;
  overflow-y: auto;
}

/* 隐藏滚动条 */
.sidebar-menu-wrapper::-webkit-scrollbar {
  display: none;
}

.sidebar-menu-wrapper {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  cursor: pointer;
}

.logo-text {
  font-size: 20px;
  font-weight: 600;
  color: #fff;
}

.site-logo {
  width: 32px;
  height: 32px;
  object-fit: contain;
  border-radius: 4px;
}

.sidebar-menu {
  border: none;
}

.sidebar-menu :deep(.el-menu-item),
.sidebar-menu :deep(.el-sub-menu__title) {
  height: 50px;
  line-height: 50px;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: #1890ff !important;
}

.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.message-badge {
  cursor: pointer;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background 0.3s;
}

.user-info:hover {
  background: #f5f5f5;
}

.username {
  font-size: 14px;
  color: #333;
}

.main-content {
  background: #f0f2f5;
  padding: 24px;
  margin-left: 220px;
  min-height: calc(100vh - 64px);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
