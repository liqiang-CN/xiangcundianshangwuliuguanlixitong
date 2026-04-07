<template>
  <div class="login-view">
    <div class="login-box">
      <div class="login-header">
        <img v-if="siteLogo" :src="getImageUrl(siteLogo)" class="site-logo" />
        <el-icon v-else size="48" color="#1890ff"><Shop /></el-icon>
        <h1 class="login-title">{{ siteName }}后台管理</h1>
      </div>
      <el-form :model="loginForm" :rules="rules" ref="loginFormRef" class="login-form">
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            size="large"
            :prefix-icon="User"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Shop } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { login } from '@/api/user'
import request from '@/api/request'
import { getImageUrl } from '@/utils/image'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const loginFormRef = ref()
const siteName = ref('农鲜达')
const siteLogo = ref('')

const loginForm = reactive({
  username: 'admin',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  const valid = await loginFormRef.value?.validate()
  if (!valid) return

  loading.value = true
  try {
    const res = await login({
      username: loginForm.username,
      password: loginForm.password
    })
    
    userStore.setToken(res.token)
    userStore.setUserInfo(res.userInfo)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (error: any) {
    // 错误已在请求拦截器中处理
    console.error('登录失败:', error)
  } finally {
    loading.value = false
  }
}

// 获取系统配置
const fetchSiteConfig = async () => {
  try {
    const config = await request.get('/admin/config') as Record<string, string>
    siteName.value = config['site.name'] || '农鲜达'
    siteLogo.value = config['site.logo'] || ''
  } catch (error) {
    console.error('获取配置失败:', error)
  }
}

onMounted(() => {
  fetchSiteConfig()
})
</script>

<style scoped>
.login-view {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f2f5;
  background-image: url('/images/LoginBgc.png');
  background-size: 100% auto;
  background-position: center top;
  background-repeat: repeat-y;
}

.login-box {
  width: 400px;
  padding: 40px;
  background: rgba(255, 255, 255, 0.25);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  margin-left: 50%;
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.site-logo {
  width: 56px;
  height: 56px;
  object-fit: contain;
  border-radius: 8px;
}

.login-title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin-top: 16px;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 4px;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  border-radius: 4px;
}
</style>
