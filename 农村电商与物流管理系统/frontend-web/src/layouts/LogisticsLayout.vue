<template>
  <div class="logistics-layout">
    <aside class="sidebar">
      <div class="sidebar-header">
        <el-icon size="32" color="#ff9500"><Van /></el-icon>
        <span class="sidebar-title">物流配送</span>
      </div>
      <el-menu
        :default-active="$route.path"
        router
        class="logistics-menu"
        background-color="transparent"
        text-color="#666"
        active-text-color="#ff9500"
      >
        <el-menu-item index="/logistics/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>工作台</span>
        </el-menu-item>
        <el-menu-item index="/logistics/waybills">
          <el-icon><Document /></el-icon>
          <span>运单管理</span>
        </el-menu-item>
        <el-menu-item index="/logistics/print">
          <el-icon><Printer /></el-icon>
          <span>面单打印</span>
        </el-menu-item>
        <el-menu-item index="/logistics/route">
          <el-icon><MapLocation /></el-icon>
          <span>配送路线</span>
        </el-menu-item>
        <el-menu-item index="/logistics/records">
          <el-icon><Finished /></el-icon>
          <span>签收记录</span>
        </el-menu-item>
        <el-menu-item index="/logistics/profile">
          <el-icon><User /></el-icon>
          <span>个人中心</span>
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
import { useUserStore } from '@/stores/user'
import { ElMessageBox } from 'element-plus'

const userStore = useUserStore()

// 初始化用户信息
userStore.initUserInfo()

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
</script>

<style scoped>
.logistics-layout {
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

.logistics-menu {
  flex: 1;
  border: none;
  padding: 16px 0;
}

.logistics-menu :deep(.el-menu-item) {
  margin: 4px 16px;
  border-radius: 10px;
  height: 48px;
  line-height: 48px;
}

.logistics-menu :deep(.el-menu-item.is-active) {
  background: rgba(255, 149, 0, 0.1);
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
</style>
