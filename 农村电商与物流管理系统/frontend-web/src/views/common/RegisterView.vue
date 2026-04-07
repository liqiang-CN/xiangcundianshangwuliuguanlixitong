<template>
  <div class="register-view">
    <div class="register-container">
      <div class="register-header">
        <div class="brand" @click="$router.push('/consumer/home')">
          <el-icon size="40" color="#007aff"><Shop /></el-icon>
          <span class="brand-name">农鲜达</span>
        </div>
      </div>
      <div class="register-box">
        <h2 class="register-title">创建账号</h2>
        <p class="register-subtitle">加入我们，开启农村电商之旅</p>
        
        <el-steps :active="currentStep" finish-status="success" class="register-steps">
          <el-step title="选择类型" />
          <el-step title="填写信息" />
          <el-step title="完成注册" />
        </el-steps>

        <!-- 步骤1：选择用户类型 -->
        <div v-if="currentStep === 0" class="step-content">
          <div class="type-cards">
            <div 
              v-for="type in userTypes" 
              :key="type.value"
              class="type-card"
              :class="{ active: registerForm.userType === type.value }"
              @click="registerForm.userType = type.value"
            >
              <el-icon size="48" :color="type.color"><component :is="type.icon" /></el-icon>
              <h3 class="type-name">{{ type.label }}</h3>
              <p class="type-desc">{{ type.desc }}</p>
            </div>
          </div>
          <el-button type="primary" size="large" class="next-btn" @click="nextStep">
            下一步 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>

        <!-- 步骤2：填写信息 -->
        <div v-if="currentStep === 1" class="step-content">
          <el-form :model="registerForm" :rules="rules" ref="registerFormRef">
            <el-form-item prop="username">
              <el-input v-model="registerForm.username" placeholder="用户名" size="large" :prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="nickname">
              <el-input v-model="registerForm.nickname" placeholder="昵称" size="large" :prefix-icon="Avatar" />
            </el-form-item>
            <el-form-item prop="phone">
              <el-input v-model="registerForm.phone" placeholder="手机号" size="large" :prefix-icon="Phone" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="registerForm.password" type="password" placeholder="密码" size="large" :prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <el-input v-model="registerForm.confirmPassword" type="password" placeholder="确认密码" size="large" :prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item prop="agreement" class="agreement-item">
              <el-checkbox v-model="registerForm.agreement" size="large">
                <span class="agreement-text">我已阅读并同意</span>
                <el-button link type="primary" class="agreement-link">用户协议</el-button>
                <span class="agreement-text">和</span>
                <el-button link type="primary" class="agreement-link">隐私政策</el-button>
              </el-checkbox>
            </el-form-item>
          </el-form>
          <div class="step-actions">
            <el-button size="large" @click="prevStep">上一步</el-button>
            <el-button type="primary" size="large" :loading="loading" @click="handleRegister">注册</el-button>
          </div>
        </div>

        <!-- 步骤3：注册成功 -->
        <div v-if="currentStep === 2" class="step-content success-step">
          <el-icon size="80" color="#34c759"><CircleCheckFilled /></el-icon>
          <h3 class="success-title">注册成功！</h3>
          <p class="success-desc">欢迎加入农鲜达，即将跳转到登录页面...</p>
        </div>
      </div>
      <div class="register-footer">
        已有账号？<el-button link type="primary" @click="$router.push('/login')">立即登录</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Shop, Avatar, Phone, ArrowRight, CircleCheckFilled, UserFilled, Apple, Van } from '@element-plus/icons-vue'
import { userApi } from '@/api/user'

const router = useRouter()
const currentStep = ref(0)
const loading = ref(false)
const registerFormRef = ref()

const userTypes = [
  { value: 1, label: '用户', icon: 'UserFilled', color: '#007aff', desc: '购买新鲜农产品，享受便捷配送' },
  { value: 2, label: '农户', icon: 'Apple', color: '#34c759', desc: '发布农产品，拓展销售渠道' },
  { value: 3, label: '物流人员', icon: 'Van', color: '#ff9500', desc: '接单配送，增加收入来源' }
]

const registerForm = reactive({
  userType: 1,
  username: '',
  nickname: '',
  phone: '',
  password: '',
  confirmPassword: '',
  agreement: false
})

const validatePass2 = (rule: any, value: string, callback: Function) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const validateAgreement = (rule: any, value: boolean, callback: Function) => {
  if (!value) {
    callback(new Error('请阅读并同意用户协议和隐私政策'))
  } else {
    callback()
  }
}

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请确认密码', trigger: 'blur' }, { validator: validatePass2, trigger: 'blur' }],
  agreement: [{ validator: validateAgreement, trigger: 'change' }]
}

const nextStep = () => {
  if (!registerForm.userType) {
    ElMessage.warning('请选择用户类型')
    return
  }
  currentStep.value++
}

const prevStep = () => {
  currentStep.value--
}

const handleRegister = async () => {
  const valid = await registerFormRef.value?.validate()
  if (!valid) return

  loading.value = true
  try {
    await userApi.register({
      username: registerForm.username,
      password: registerForm.password,
      phone: registerForm.phone,
      userType: registerForm.userType,
      nickname: registerForm.nickname
    })
    
    currentStep.value++
    setTimeout(() => {
      router.push('/login')
    }, 2000)
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-view {
  min-height: 100vh;
  background: var(--apple-bg);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 20px;
}

.register-container {
  width: 100%;
  max-width: 600px;
}

.register-header {
  margin-bottom: 32px;
  text-align: center;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.brand-name {
  font-size: 28px;
  font-weight: 700;
  background: linear-gradient(135deg, #007aff, #34c759);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.register-box {
  background: white;
  border-radius: 24px;
  padding: 48px;
  box-shadow: var(--apple-shadow);
}

.register-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--apple-dark-gray);
  text-align: center;
  margin-bottom: 8px;
}

.register-subtitle {
  font-size: 16px;
  color: var(--apple-gray);
  text-align: center;
  margin-bottom: 32px;
}

.register-steps {
  margin-bottom: 40px;
}

.step-content {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.type-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 32px;
}

.type-card {
  padding: 24px;
  border: 2px solid var(--apple-light-gray);
  border-radius: 16px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
}

.type-card:hover {
  border-color: var(--apple-blue);
  transform: translateY(-4px);
}

.type-card.active {
  border-color: var(--apple-blue);
  background: rgba(0, 122, 255, 0.05);
}

.type-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--apple-dark-gray);
  margin: 12px 0 8px;
}

.type-desc {
  font-size: 12px;
  color: var(--apple-gray);
  line-height: 1.5;
}

.next-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 10px;
}

.step-actions {
  display: flex;
  gap: 16px;
}

.step-actions .el-button {
  flex: 1;
  height: 48px;
  font-size: 16px;
  border-radius: 10px;
}

.success-step {
  text-align: center;
  padding: 40px 0;
}

.success-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--apple-dark-gray);
  margin: 24px 0 12px;
}

.success-desc {
  font-size: 16px;
  color: var(--apple-gray);
}

.register-footer {
  text-align: center;
  margin-top: 24px;
  color: var(--apple-gray);
  font-size: 14px;
}

/* 协议勾选样式 */
.agreement-item {
  margin-top: 8px;
}

.agreement-item :deep(.el-checkbox) {
  align-items: flex-start;
  white-space: normal;
  line-height: 1.5;
}

.agreement-item :deep(.el-checkbox__label) {
  display: inline;
  padding-left: 8px;
}

.agreement-text {
  color: var(--apple-gray);
  font-size: 14px;
}

.agreement-link {
  font-size: 14px;
  padding: 0 2px;
  height: auto;
}

.agreement-link:hover {
  text-decoration: underline;
}
</style>
