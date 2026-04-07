<template>
  <div class="login-view" @mousemove="handleMouseMove">
    <div class="login-container">
      <div class="login-left">
        <div class="characters-container">
          <!-- 橙色半圆形角色 -->
          <div class="character orange-semicircle" :class="{ 'focused': isFocused, 'error': hasError, 'password-focused': isPasswordFocused }">
            <div class="face">
              <div class="eyes">
                <div class="eye" :class="{ 'closed': isPasswordFocused }">
                  <div class="pupil" :style="{ transform: `translate(${orangePupilX}px, ${orangePupilY}px)` }" v-if="!isPasswordFocused"></div>
                </div>
                <div class="eye" :class="{ 'closed': isPasswordFocused }">
                  <div class="pupil" :style="{ transform: `translate(${orangePupilX}px, ${orangePupilY}px)` }" v-if="!isPasswordFocused"></div>
                </div>
              </div>
              <div class="mouth" :class="{ 'sad': hasError, 'surprised': isFocused, 'closed': isPasswordFocused }"></div>
            </div>
          </div>
          
          <!-- 高瘦紫色矩形角色 -->
          <div class="character purple-rectangle" :class="{ 'focused': isFocused, 'error': hasError, 'password-focused': isPasswordFocused }">
            <div class="face">
              <div class="eyes">
                <div class="eye" :class="{ 'closed': isPasswordFocused }">
                  <div class="pupil" :style="{ transform: `translate(${purplePupilX}px, ${purplePupilY}px)` }" v-if="!isPasswordFocused"></div>
                </div>
                <div class="eye" :class="{ 'closed': isPasswordFocused }">
                  <div class="pupil" :style="{ transform: `translate(${purplePupilX}px, ${purplePupilY}px)` }" v-if="!isPasswordFocused"></div>
                </div>
              </div>
              <div class="mouth" :class="{ 'sad': hasError, 'surprised': isFocused, 'closed': isPasswordFocused }"></div>
            </div>
          </div>
          
          <!-- 黑色矩形角色 -->
          <div class="character black-rectangle" :class="{ 'focused': isFocused, 'error': hasError, 'password-focused': isPasswordFocused }">
            <div class="face">
              <div class="eyes">
                <div class="eye" :class="{ 'closed': isPasswordFocused }">
                  <div class="pupil" :style="{ transform: `translate(${blackPupilX}px, ${blackPupilY}px)` }" v-if="!isPasswordFocused"></div>
                </div>
                <div class="eye" :class="{ 'closed': isPasswordFocused }">
                  <div class="pupil" :style="{ transform: `translate(${blackPupilX}px, ${blackPupilY}px)` }" v-if="!isPasswordFocused"></div>
                </div>
              </div>
              <div class="mouth" :class="{ 'sad': hasError, 'surprised': isFocused, 'closed': isPasswordFocused }"></div>
            </div>
          </div>
          
          <!-- 黄色胶囊形角色 -->
          <div class="character yellow-capsule" :class="{ 'focused': isFocused, 'error': hasError, 'password-focused': isPasswordFocused }">
            <div class="face">
              <div class="eyes">
                <div class="eye" :class="{ 'closed': isPasswordFocused }">
                  <div class="pupil" :style="{ transform: `translate(${yellowPupilX}px, ${yellowPupilY}px)` }" v-if="!isPasswordFocused"></div>
                </div>
                <div class="eye" :class="{ 'closed': isPasswordFocused }">
                  <div class="pupil" :style="{ transform: `translate(${yellowPupilX}px, ${yellowPupilY}px)` }" v-if="!isPasswordFocused"></div>
                </div>
              </div>
              <div class="mouth" :class="{ 'sad': hasError, 'surprised': isFocused, 'closed': isPasswordFocused }"></div>
            </div>
          </div>
        </div>
      </div>
      <div class="login-right">
        <div class="login-box">
        <div class="login-logo">
          <div class="logo-icon">+</div>
        </div>
        <h2 class="login-title">欢迎回来</h2>
        <p class="login-subtitle">请输入您的详细信息</p>
        <el-tabs v-model="activeTab" class="login-tabs">
            <el-tab-pane label="用户" name="consumer"></el-tab-pane>
            <el-tab-pane label="农户" name="farmer"></el-tab-pane>
            <el-tab-pane label="物流员" name="admin"></el-tab-pane>
          </el-tabs>
          <el-form :model="loginForm" :rules="rules" ref="loginFormRef" class="login-form">
            <el-form-item prop="username">
              <el-input
                v-model="loginForm.username"
                placeholder="请输入用户名/手机号"
                size="large"
                :prefix-icon="User"
                @focus="handleFocus"
                @blur="handleBlur"
              >
                <template #suffix>
                  <el-dropdown v-if="currentTypeAccounts.length > 0" trigger="click">
                    <el-icon class="account-dropdown-icon"><ArrowDown /></el-icon>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item
                          v-for="account in currentTypeAccounts"
                          :key="account.username"
                          @click="selectAccount(account)"
                        >
                          {{ account.username }}
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="请输入密码"
                size="large"
                :prefix-icon="Lock"
                show-password
                @focus="handlePasswordFocus"
                @blur="handleBlur"
                @keyup.enter="handleLogin"
              />
            </el-form-item>
            <div class="form-options">
              <el-checkbox v-model="rememberMe" label="记住我" />
              <el-button link type="primary">忘记密码？</el-button>
            </div>
            <el-button
              type="primary"
              size="large"
              class="login-btn"
              :loading="loading"
              @click="handleLogin"
            >
              登 录
            </el-button>
          </el-form>
          <div class="register-link">
            还没有账号？<el-button link type="primary" @click="$router.push('/register')">立即注册</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch, computed, type Ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Shop, CircleCheck, ArrowDown } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { userApi } from '@/api/user'

const router = useRouter()
const userStore = useUserStore()

// 记住的账号列表
interface RememberedAccount {
  username: string
  password: string
  userType: string
}

const activeTab = ref('consumer')
const loading = ref(false)
const rememberMe = ref(false)
const loginFormRef = ref()

const loginForm = reactive({
  username: '',
  password: ''
})

// 记住的账号列表
const rememberedAccounts = ref<RememberedAccount[]>([])

// 当前用户类型下的账号列表
const currentTypeAccounts = computed(() => {
  return rememberedAccounts.value.filter(acc => acc.userType === activeTab.value)
})

// 从 localStorage 加载记住的账号
const loadRememberedAccounts = () => {
  const saved = localStorage.getItem('rememberedAccounts')
  if (saved) {
    try {
      rememberedAccounts.value = JSON.parse(saved)
    } catch (e) {
      console.error('加载记住的账号失败:', e)
      rememberedAccounts.value = []
    }
  }
}

// 保存账号到 localStorage
const saveAccount = (username: string, password: string, userType: string) => {
  // 检查是否已存在
  const existingIndex = rememberedAccounts.value.findIndex(
    acc => acc.username === username && acc.userType === userType
  )

  if (existingIndex !== -1) {
    // 更新已有账号
    rememberedAccounts.value[existingIndex] = { username, password, userType }
  } else {
    // 添加新账号，最多保存3个
    rememberedAccounts.value.unshift({ username, password, userType })
    if (rememberedAccounts.value.length > 3) {
      rememberedAccounts.value = rememberedAccounts.value.slice(0, 3)
    }
  }

  localStorage.setItem('rememberedAccounts', JSON.stringify(rememberedAccounts.value))
}

// 填充记住的账号
const fillRememberedAccount = () => {
  const account = rememberedAccounts.value.find(acc => acc.userType === activeTab.value)
  if (account) {
    loginForm.username = account.username
    loginForm.password = account.password
    rememberMe.value = true
  } else {
    loginForm.username = ''
    loginForm.password = ''
    rememberMe.value = false
  }
}

// 选择已记住的账号
const selectAccount = (account: RememberedAccount) => {
  loginForm.username = account.username
  loginForm.password = account.password
  rememberMe.value = true
}

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// 用户类型映射
const userTypeMap: Record<string, number> = {
  consumer: 1,
  farmer: 2,
  admin: 3
}

// ========== 灵动角色相关状态 ==========
// 鼠标位置
const mouse = ref({ x: 0, y: 0 })
// 聚焦状态
const isFocused = ref(false)
// 密码输入状态
const isPasswordFocused = ref(false)
// 错误状态
const hasError = ref(false)
// 瞳孔位置
const orangePupilX = ref(0)
const orangePupilY = ref(0)
const purplePupilX = ref(0)
const purplePupilY = ref(0)
const blackPupilX = ref(0)
const blackPupilY = ref(0)
const yellowPupilX = ref(0)
const yellowPupilY = ref(0)

// 处理鼠标移动
const handleMouseMove = (event: MouseEvent) => {
  const loginElement = event.currentTarget as HTMLElement
  const rect = loginElement.getBoundingClientRect()
  mouse.value.x = event.clientX - rect.left
  mouse.value.y = event.clientY - rect.top
  
  // 计算瞳孔位置
  updatePupilPositions()
}

// 更新瞳孔位置
const updatePupilPositions = () => {
  // 计算每个角色的瞳孔位置
  calculatePupilPosition('.orange-semicircle', orangePupilX, orangePupilY)
  calculatePupilPosition('.purple-rectangle', purplePupilX, purplePupilY)
  calculatePupilPosition('.black-rectangle', blackPupilX, blackPupilY)
  calculatePupilPosition('.yellow-capsule', yellowPupilX, yellowPupilY)
}

// 计算单个角色的瞳孔位置
const calculatePupilPosition = (selector: string, pupilX: Ref<number>, pupilY: Ref<number>) => {
  const character = document.querySelector(selector)
  if (!character) return
  
  // 获取角色的实际位置
  const rect = character.getBoundingClientRect()
  const loginRect = character.closest('.login-view')?.getBoundingClientRect()
  if (!loginRect) return
  
  // 计算角色眼睛的中心位置（相对于login容器）
  const eyeCenterX = (rect.left + rect.width / 2) - loginRect.left
  const eyeCenterY = (rect.top + rect.height * 0.4) - loginRect.top // 眼睛大概在角色高度的40%位置
  
  // 计算鼠标与眼睛中心的距离
  const dx = mouse.value.x - eyeCenterX
  const dy = mouse.value.y - eyeCenterY
  const distance = Math.sqrt(dx * dx + dy * dy)
  
  // 限制瞳孔移动范围，确保瞳孔在眼球内
  const eyeRadius = 10 // 眼球半径
  const pupilRadius = 4 // 瞳孔半径
  const maxDistance = eyeRadius - pupilRadius // 瞳孔最大移动距离
  
  let moveX = 0
  let moveY = 0
  
  if (distance > 0) {
    moveX = (dx / distance) * Math.min(distance, maxDistance)
    moveY = (dy / distance) * Math.min(distance, maxDistance)
  }
  
  pupilX.value = moveX
  pupilY.value = moveY
}

// 处理输入框聚焦
const handleFocus = () => {
  isFocused.value = true
  hasError.value = false
}

// 处理密码输入框聚焦
const handlePasswordFocus = () => {
  isFocused.value = false
  isPasswordFocused.value = true
  hasError.value = false
}

// 处理输入框失焦
const handleBlur = () => {
  isFocused.value = false
  isPasswordFocused.value = false
}

// 呼吸动画
const startBreathingAnimation = () => {
  const characters = document.querySelectorAll('.character')
  characters.forEach((character, index) => {
    const delay = index * 0.2
    ;(character as HTMLElement).style.animation = `breathe 3s ease-in-out ${delay}s infinite`
  })
}

// 组件挂载时初始化
onMounted(() => {
  startBreathingAnimation()
  loadRememberedAccounts()
  fillRememberedAccount()
})

// 监听标签页切换
watch(activeTab, () => {
  fillRememberedAccount()
})

const handleLogin = async () => {
  const valid = await loginFormRef.value?.validate()
  if (!valid) return

  loading.value = true
  try {
    const res: any = await userApi.login({
      username: loginForm.username,
      password: loginForm.password,
      userType: userTypeMap[activeTab.value]
    })

    userStore.setToken(res.token)
    userStore.setUserInfo(res.userInfo)

    // 如果勾选了记住我，保存账号密码
    if (rememberMe.value) {
      saveAccount(loginForm.username, loginForm.password, activeTab.value)
    }

    ElMessage.success('登录成功')

    // 根据用户类型跳转到不同页面
    const redirectMap: Record<string, string> = {
      consumer: '/',
      farmer: '/farmer/dashboard',
      admin: '/logistics/dashboard'
    }

    router.push(redirectMap[activeTab.value])
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-view {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f5f5f5;
  color: #333;
  padding: 2rem;
}

.login-container {
  display: flex;
  justify-content: space-between;
  width: 1200px;
  height: 830px;
  background: white;
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.login-left {
  display: flex;
  justify-content: center;
  flex: 1;
  height: 850px;
  background-color: #ebebeb;
  border-radius: 22px 0 0 22px;
  position: relative;
  overflow: hidden;
}

.characters-container {
  position: relative;
  width: 100%;
  height: 65%;
  overflow: hidden;
}

.character {
  position: absolute;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  animation: breathe 3s ease-in-out infinite;
}

/* 呼吸动画 */
@keyframes breathe {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
}

/* 橙色半圆形角色 */
.orange-semicircle {
  position: absolute;
  bottom: 0;
  left: 42%;
  transform: translateX(-50%);
  width: 200px;
  height: 100px;
  background-color: #f97316;
  border-radius: 100px 100px 0 0;
  z-index: 4;
}

/* 高瘦紫色矩形角色 */
.purple-rectangle {
  position: absolute;
  bottom: 0;
  left: 42%;
  transform: translateX(-50%);
  width: 120px;
  height: 250px;
  background-color: #8b5cf6;
  border-radius: 60px 60px 0 0;
  z-index: 1;
}

/* 黑色矩形角色 */
.black-rectangle {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100px;
  height: 150px;
  background-color: #000000;
  border-radius: 30px 30px 0 0;
  z-index: 2;
}

/* 黄色胶囊形角色 */
.yellow-capsule {
  position: absolute;
  bottom: 0;
  left: 60%;
  transform: translateX(-50%);
  width: 100px;
  height: 180px;
  background-color: #eab308;
  border-radius: 50px 50px 0 0;
  z-index: 3;
}

/* 角色面部 */
.face {
  position: relative;
  width: 80%;
  height: 80%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

/* 紫色角色的面部，调整眼睛和嘴巴位置 */
.purple-rectangle .face {
  justify-content: flex-start;
  padding-top: 20%;
}

/* 眼睛容器 */
.eyes {
  display: flex;
  gap: 15px;
  margin-bottom: 10px;
}

/* 眼睛 */
.eye {
  width: 20px;
  height: 20px;
  background-color: #ffffff;
  border-radius: 50%;
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
}

/* 黑色角色的眼睛 */
.black-rectangle .eye {
  width: 24px;
  height: 24px;
}

/* 瞳孔 */
.pupil {
  width: 8px;
  height: 8px;
  background-color: #000000;
  border-radius: 50%;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  transition: transform 0.1s ease;
}

/* 黄色角色的瞳孔 */
.yellow-capsule .pupil {
  width: 12px;
  height: 12px;
}

/* 嘴巴 */
.mouth {
  width: 40px;
  height: 20px;
  border-radius: 0 0 20px 20px;
  background-color: #000000;
  transition: all 0.3s ease;
}

/* 紫色角色的嘴巴 */
.purple-rectangle .mouth {
  width: 30px;
  height: 6px;
  border-radius: 3px;
  background-color: #000000;
}

/* 黄色角色的嘴巴 */
.yellow-capsule .mouth {
  width: 60px;
  height: 4px;
  border-radius: 2px;
  background-color: #000000;
}

/* 悲伤的嘴巴 */
.mouth.sad {
  border-radius: 20px 20px 0 0;
  height: 10px;
}

/* 惊讶的嘴巴 */
.mouth.surprised {
  border-radius: 50%;
  height: 20px;
  width: 20px;
}

/* 聚焦状态 */
.orange-semicircle.focused {
  transform: translateX(-50%) scale(1.1);
}

.purple-rectangle.focused {
  transform: translateX(-50%) scale(1.1);
}

.black-rectangle.focused {
  transform: translateX(-50%) scale(1.1);
}

.yellow-capsule.focused {
  transform: translateX(-50%) scale(1.1);
}

.character.focused .eye {
  width: 24px;
  height: 24px;
}

/* 错误状态 */
.orange-semicircle.error {
  transform-origin: center bottom;
  transform: translateX(-50%) scale(0.9);
}

.purple-rectangle.error {
  transform-origin: center bottom;
  transform: translateX(-50%) scale(0.9);
}

.black-rectangle.error {
  transform-origin: center bottom;
  transform: translateX(-50%) scale(0.9);
}

.yellow-capsule.error {
  transform-origin: center bottom;
  transform: translateX(-50%) scale(0.9);
}

.character.error .eye {
  width: 24px;
  height: 24px;
}

/* 密码输入状态 */
.character.password-focused {
  transform-origin: center bottom;
  transform: translateX(-50%) scale(0.95);
}

.character.password-focused .eye.closed {
  width: 24px;
  height: 4px;
  border-radius: 2px;
  background-color: #000000;
  overflow: visible;
}

.character.password-focused .mouth.closed {
  width: 0;
  height: 0;
  border-radius: 0;
}

/* 确保橙色半圆形角色始终保持最高层级 */
.orange-semicircle,
.orange-semicircle.focused,
.orange-semicircle.error {
  z-index: 10 !important;
}

.login-right {
  width: 400px;
  height: 850px;
  padding: 4rem 2rem;
  background-color: #ffffff;
  border-radius: 0 22px 22px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.login-box {
  width: 100%;
  max-width: 300px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.login-logo {
  margin-bottom: 2rem;
  display: flex;
  justify-content: center;
}

.logo-icon {
  width: 40px;
  height: 40px;
  background-color: #000;
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: bold;
  transform: rotate(45deg);
}

.login-title {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 0.5rem;
  color: #000;
  text-align: center;
}

.login-subtitle {
  font-size: 16px;
  color: #666;
  margin-bottom: 2rem;
  text-align: center;
}

.login-tabs {
  margin-bottom: 24px;
  width: 100%;
}

.login-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.login-tabs :deep(.el-tabs__active-bar) {
  background: #000;
}

.login-tabs :deep(.el-tabs__item) {
  color: #666;
  font-size: 14px;
}

.login-tabs :deep(.el-tabs__item.is-active) {
  color: #000;
}

.login-tabs :deep(.el-tabs__nav-scroll) {
  display: flex;
  justify-content: center;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 4px;
  box-shadow: 0 0 0 1px #ddd;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #000;
}

.login-form :deep(.el-input__inner) {
  font-size: 16px;
}

.login-form :deep(.el-input) {
  width: 280px;
}

.login-form :deep(.el-input__wrapper) {
  width: 280px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.login-form :deep(.el-form-item:last-child) {
  margin-bottom: 0;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  font-size: 14px;
}

.form-options :deep(.el-checkbox__label) {
  color: #666;
}

.form-options :deep(.el-button) {
  color: #666;
}

.form-options :deep(.el-button:hover) {
  color: #000;
}

.account-dropdown-icon {
  cursor: pointer;
  color: #999;
  font-size: 14px;
  transition: color 0.3s;
}

.account-dropdown-icon:hover {
  color: #333;
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 500;
  border-radius: 8px;
  background-color: #000;
  border: none;
  transition: background-color 0.3s ease;
}

.login-btn:hover {
  background-color: #333;
}

.register-link {
  text-align: center;
  margin-top: 2rem;
  color: #666;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.register-link :deep(.el-button) {
  color: #409eff;
  font-weight: 500;
  height: auto;
  padding: 0;
}

.register-link :deep(.el-button:hover) {
  color: #66b1ff;
}
</style>
