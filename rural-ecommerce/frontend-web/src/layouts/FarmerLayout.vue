<template>
  <div class="farmer-layout">
    <aside class="sidebar">
      <div class="sidebar-header">
        <el-icon size="32" color="#34c759"><Apple /></el-icon>
        <span class="sidebar-title">农户中心</span>
      </div>
      <el-menu
        :default-active="$route.path"
        router
        class="farmer-menu"
        background-color="transparent"
        text-color="#666"
        active-text-color="#34c759"
      >
        <el-menu-item index="/farmer/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>数据概览</span>
        </el-menu-item>
        <el-menu-item index="/farmer/products">
          <el-icon><Goods /></el-icon>
          <span>商品管理</span>
        </el-menu-item>
        <el-menu-item index="/farmer/orders">
          <el-icon><List /></el-icon>
          <span>订单管理</span>
        </el-menu-item>
        <el-menu-item index="/farmer/shop">
          <el-icon><Shop /></el-icon>
          <span>店铺管理</span>
        </el-menu-item>
        <el-menu-item index="/farmer/income">
          <el-icon><TrendCharts /></el-icon>
          <span>收入统计</span>
        </el-menu-item>
        <el-menu-item index="/farmer/messages" class="message-menu-item">
          <el-icon><Bell /></el-icon>
          <span>消息通知</span>
          <span v-if="unreadCount > 0" class="red-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
        </el-menu-item>
        <el-menu-item index="/farmer/address">
          <el-icon><Location /></el-icon>
          <span>发货地址</span>
        </el-menu-item>
        <el-menu-item index="/farmer/password">
          <el-icon><Lock /></el-icon>
          <span>修改密码</span>
        </el-menu-item>
      </el-menu>
      <div class="sidebar-footer">
        <el-button type="danger" plain @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>
          退出登录
        </el-button>
      </div>
    </aside>
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessageBox } from 'element-plus'
import { Odometer, Goods, List, Shop, TrendCharts, Bell, SwitchButton, Apple, Lock, Location } from '@element-plus/icons-vue'
import { getConversations } from '@/api/chat'

const userStore = useUserStore()
const unreadCount = ref(0)

// 初始化用户信息
userStore.initUserInfo()

// 获取未读消息数量
const fetchUnreadCount = async () => {
  try {
    const res: any = await getConversations()
    if (res && Array.isArray(res)) {
      // 计算所有会话的未读消息总数
      const totalUnread = res.reduce((sum: number, conv: any) => {
        return sum + (conv.unreadCount || 0)
      }, 0)
      unreadCount.value = totalUnread
      console.log('未读消息总数:', totalUnread, '会话列表:', res)
    } else {
      unreadCount.value = 0
    }
  } catch (error) {
    console.error('获取未读消息数量失败:', error)
    unreadCount.value = 0
  }
}

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

onMounted(() => {
  fetchUnreadCount()
  // 每5秒刷新一次未读消息数量，确保及时显示
  setInterval(fetchUnreadCount, 5000)
})
</script>

<style scoped>
.farmer-layout {
  display: flex;
  min-height: 100vh;
  background: var(--apple-bg);
}

.sidebar {
  width: 240px;
  background: var(--apple-card-bg);
  border-right: 1px solid var(--apple-border);
  display: flex;
  flex-direction: column;
  position: fixed;
  height: 100vh;
}

.sidebar-header {
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid var(--apple-border);
}

.sidebar-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--apple-dark-gray);
}

.farmer-menu {
  flex: 1;
  border: none;
  padding: 16px 0;
}

.farmer-menu :deep(.el-menu-item) {
  margin: 4px 16px;
  border-radius: 10px;
  height: 48px;
  line-height: 48px;
}

.farmer-menu :deep(.el-menu-item.is-active) {
  background: rgba(52, 199, 89, 0.1);
}

.message-menu-item {
  position: relative;
}

.red-badge {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  background-color: #ff4d4f;
  border-radius: 9px;
  color: #fff;
  font-size: 12px;
  line-height: 18px;
  text-align: center;
  font-weight: 500;
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid var(--apple-border);
}

.sidebar-footer .el-button {
  width: 100%;
}

.main-content {
  flex: 1;
  margin-left: 240px;
  padding: 24px;
  min-height: 100vh;
}

/* 移除农户页面所有卡片的阴影 */
.main-content :deep(.el-card) {
  box-shadow: none !important;
  border: 1px solid var(--apple-border);
}
</style>
