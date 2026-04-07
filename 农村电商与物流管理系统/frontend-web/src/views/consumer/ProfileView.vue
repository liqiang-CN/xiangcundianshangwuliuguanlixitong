<template>
  <div class="profile-view">
    <div class="profile-container">
      <h2 class="page-title">个人中心</h2>

      <div class="profile-layout">
        <!-- 左侧菜单 -->
        <div class="profile-sidebar">
          <div class="user-card">
            <el-avatar :size="80" :src="userInfo.avatar" />
            <h3 class="user-name">{{ userInfo.nickname }}</h3>
            <p class="user-phone">{{ userInfo.phone }}</p>
          </div>
          <el-menu
            :default-active="activeMenu"
            class="profile-menu"
            @select="handleMenuSelect"
          >
            <el-menu-item index="info">
              <el-icon><User /></el-icon>
              <span>基本信息</span>
            </el-menu-item>
            <el-menu-item index="password">
              <el-icon><Lock /></el-icon>
              <span>修改密码</span>
            </el-menu-item>
            <el-menu-item index="address">
              <el-icon><Location /></el-icon>
              <span>收货地址</span>
            </el-menu-item>
          </el-menu>
        </div>

        <!-- 右侧内容 -->
        <div class="profile-content">
          <!-- 基本信息 -->
          <div v-if="activeMenu === 'info'" class="content-section">
            <h3 class="section-title">基本信息</h3>
            <el-form :model="userInfo" label-width="100px" class="profile-form">
              <el-form-item label="头像">
                <div class="avatar-upload-wrapper">
                  <img v-if="userInfo.avatar" :src="userInfo.avatar" class="avatar-preview" @click="triggerAvatarUpload" />
                  <div v-else class="avatar-placeholder" @click="triggerAvatarUpload">
                    <el-icon size="32"><Plus /></el-icon>
                    <span>点击上传</span>
                  </div>
                  <input
                    ref="avatarInputRef"
                    type="file"
                    accept="image/*"
                    style="display: none"
                    @change="handleAvatarChange"
                  />
                </div>
              </el-form-item>
              <el-form-item label="昵称">
                <el-input v-model="userInfo.nickname" />
              </el-form-item>
              <el-form-item label="手机号">
                <el-input v-model="userInfo.phone" disabled />
              </el-form-item>
              <el-form-item label="邮箱">
                <el-input v-model="userInfo.email" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="saveProfile">保存修改</el-button>
              </el-form-item>
            </el-form>
          </div>

          <!-- 修改密码 -->
          <div v-if="activeMenu === 'password'" class="content-section">
            <h3 class="section-title">修改密码</h3>
            <el-form :model="passwordForm" label-width="100px" class="profile-form">
              <el-form-item label="原密码">
                <el-input v-model="passwordForm.oldPassword" type="password" show-password />
              </el-form-item>
              <el-form-item label="新密码">
                <el-input v-model="passwordForm.newPassword" type="password" show-password />
              </el-form-item>
              <el-form-item label="确认密码">
                <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="updatePassword">确认修改</el-button>
              </el-form-item>
            </el-form>
          </div>

          <!-- 收货地址 -->
          <div v-if="activeMenu === 'address'" class="content-section">
            <h3 class="section-title">收货地址</h3>
            <div v-loading="addressLoading">
              <div v-if="addresses.length === 0" class="empty-address">
                <el-empty description="暂无收货地址" />
                <div class="empty-add-btn">
                  <el-button type="primary" @click="addAddress">+ 添加新地址</el-button>
                </div>
              </div>
              <div v-else class="address-list">
                <div v-for="addr in addresses" :key="addr.id" class="address-card">
                  <div class="address-header">
                    <span class="address-name">{{ addr.receiverName }}</span>
                    <span class="address-phone">{{ addr.receiverPhone }}</span>
                    <el-tag v-if="addr.isDefault === 1" type="primary" size="small">默认</el-tag>
                  </div>
                  <p class="address-detail">{{ addr.province }} {{ addr.city }} {{ addr.district }} {{ addr.detailAddress }}</p>
                  <div class="address-actions">
                    <el-button link type="primary" @click="editAddress(addr)">编辑</el-button>
                    <el-button link type="danger" @click="deleteAddress(addr.id)">删除</el-button>
                    <el-button v-if="addr.isDefault !== 1" link @click="setDefault(addr.id)">设为默认</el-button>
                  </div>
                </div>
                <div class="add-address-wrapper">
                  <el-button type="primary" @click="addAddress">+ 添加新地址</el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Lock, Location, Plus } from '@element-plus/icons-vue'
import request from '@/api/request'
import { useUserStore } from '@/stores/user'
import { addressApi, type Address } from '@/api/address'

const userStore = useUserStore()
const router = useRouter()

const activeMenu = ref('info')
const loading = ref(false)

const userInfo = ref({
  id: 0,
  avatar: '',
  nickname: '',
  phone: '',
  email: ''
})

// 获取用户信息
const fetchUserInfo = async () => {
  try {
    const res: any = await request.get('/user/info')
    if (res) {
      userInfo.value = {
        id: res.id,
        avatar: res.avatar || '',
        nickname: res.nickname || res.username || '用户',
        phone: res.phone || '',
        email: res.email || ''
      }
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
    ElMessage.error('获取用户信息失败')
  }
}

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const addresses = ref<Address[]>([])
const addressLoading = ref(false)

// 获取收货地址列表
const fetchAddresses = async () => {
  addressLoading.value = true
  try {
    const res: any = await addressApi.getAddressList()
    addresses.value = res || []
  } catch (error) {
    console.error('获取地址列表失败:', error)
    ElMessage.error('获取地址列表失败')
  } finally {
    addressLoading.value = false
  }
}

// 监听菜单切换，切换到地址页时加载数据
watch(activeMenu, (newVal) => {
  if (newVal === 'address') {
    fetchAddresses()
  }
})

const handleMenuSelect = (index: string) => {
  activeMenu.value = index
}

const avatarInputRef = ref<HTMLInputElement>()

// 触发头像文件选择
const triggerAvatarUpload = () => {
  avatarInputRef.value?.click()
}

// 处理头像文件选择
const handleAvatarChange = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  
  if (!file) return
  
  // 验证文件类型
  if (!file.type.startsWith('image/')) {
    ElMessage.error('请选择图片文件')
    return
  }
  
  // 验证文件大小（最大5MB）
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过5MB')
    return
  }
  
  const formData = new FormData()
  formData.append('file', file)
  
  try {
    const res: any = await request.post('/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    
    if (res && res.url) {
      userInfo.value.avatar = res.url
      ElMessage.success('头像上传成功，请点击保存修改生效')
    } else {
      ElMessage.error('上传失败')
    }
  } catch (error) {
    console.error('上传失败:', error)
    ElMessage.error('上传失败，请重试')
  }
  
  // 清空input，允许重复选择同一文件
  if (avatarInputRef.value) {
    avatarInputRef.value.value = ''
  }
}

const saveProfile = async () => {
  try {
    await request.put('/user/info', {
      id: userInfo.value.id,
      nickname: userInfo.value.nickname,
      email: userInfo.value.email,
      avatar: userInfo.value.avatar
    })
    
    // 更新user store中的用户信息，使导航栏等位置同步更新
    if (userStore.userInfo) {
      userStore.setUserInfo({
        ...userStore.userInfo,
        nickname: userInfo.value.nickname,
        email: userInfo.value.email,
        avatar: userInfo.value.avatar
      })
    }
    
    // 通知其他页面（如后端管理系统）用户数据已更新
    localStorage.setItem('user-data-updated', Date.now().toString())
    localStorage.removeItem('user-data-updated')
    
    ElMessage.success('保存成功')
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  }
}

const updatePassword = async () => {
  // 表单验证
  if (!passwordForm.value.oldPassword.trim()) {
    ElMessage.error('请输入原密码')
    return
  }
  if (!passwordForm.value.newPassword.trim()) {
    ElMessage.error('请输入新密码')
    return
  }
  if (passwordForm.value.newPassword.length < 6) {
    ElMessage.error('新密码长度不能少于6位')
    return
  }
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    ElMessage.error('两次输入的密码不一致')
    return
  }
  
  try {
    await request.put('/user/password', {
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword
    })
    
    ElMessage.success('密码修改成功，请重新登录')
    
    // 清空密码表单
    passwordForm.value = {
      oldPassword: '',
      newPassword: '',
      confirmPassword: ''
    }

    // 修改密码后退出登录并跳转到登录页面
    userStore.logout()
    router.replace('/login')
  } catch (error: any) {
    console.error('修改密码失败:', error)
    ElMessage.error(error.message || '修改密码失败，请检查原密码是否正确')
  }
}

const addAddress = () => {
  // 跳转到专门的地址管理页面
  router.push('/address')
}

const editAddress = (addr: Address) => {
  // 跳转到专门的地址管理页面
  router.push('/address')
}

const deleteAddress = async (id: number | undefined) => {
  if (!id) return
  
  ElMessageBox.confirm('确定要删除该地址吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await addressApi.deleteAddress(id)
      ElMessage.success('删除成功')
      fetchAddresses()
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  })
}

const setDefault = async (id: number | undefined) => {
  if (!id) return
  
  try {
    await addressApi.setDefaultAddress(id)
    ElMessage.success('设置成功')
    fetchAddresses()
  } catch (error) {
    console.error('设置默认地址失败:', error)
    ElMessage.error('设置失败')
  }
}

onMounted(() => {
  fetchUserInfo()
})
</script>

<style scoped>
.profile-view {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.profile-container {
  background: var(--apple-card-bg);
  border-radius: var(--apple-radius-lg);
  padding: 32px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--apple-dark-gray);
  margin-bottom: 24px;
}

.profile-layout {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 32px;
}

.profile-sidebar {
  background: var(--apple-bg);
  border-radius: 12px;
  padding: 24px;
  height: 600px;
}

.user-card {
  text-align: center;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--apple-border);
  margin-bottom: 16px;
}

.user-name {
  font-size: 18px;
  font-weight: 600;
  color: var(--apple-dark-gray);
  margin-top: 12px;
}

.user-phone {
  font-size: 14px;
  color: var(--apple-gray);
  margin-top: 4px;
}

.profile-menu {
  border: none;
  background: transparent;
}

.profile-menu :deep(.el-menu-item) {
  border-radius: 8px;
  margin-bottom: 4px;
}

.profile-menu :deep(.el-menu-item.is-active) {
  background: var(--apple-card-bg);
}

.profile-content {
  background: var(--apple-bg);
  border-radius: 12px;
  padding: 32px;
  height: 600px;
}

.content-section {
  width: 100%;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--apple-dark-gray);
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--apple-border);
}

.profile-form :deep(.el-input__wrapper) {
  border-radius: 8px;
}

.avatar-uploader {
  border: 1px dashed var(--apple-border);
  border-radius: 50%;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 100px;
  height: 100px;
}

.avatar-uploader:hover {
  border-color: var(--apple-blue);
}

.avatar {
  width: 100px;
  height: 100px;
  display: block;
  object-fit: cover;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 100px;
  height: 100px;
  text-align: center;
  line-height: 100px;
}

.address-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 24px;
}

.address-card {
  background: var(--apple-card-bg);
  padding: 20px;
  border-radius: 12px;
  border: 1px solid var(--apple-border);
}

.address-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.address-name {
  font-weight: 600;
  color: var(--apple-dark-gray);
}

.address-phone {
  color: var(--apple-gray);
}

.address-detail {
  color: var(--apple-dark-gray);
  margin-bottom: 12px;
}

.address-actions {
  display: flex;
  gap: 16px;
}

.empty-address :deep(.el-empty) {
  padding: 10px 0;
}

.empty-add-btn {
  text-align: center;
}

.add-address-wrapper {
  text-align: center;
}

/* 头像上传样式 */
.avatar-upload-wrapper {
  display: inline-block;
}

.avatar-preview {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  object-fit: cover;
  cursor: pointer;
  border: 2px solid var(--apple-border);
  transition: all 0.3s;
}

.avatar-preview:hover {
  border-color: var(--apple-blue);
  opacity: 0.8;
}

.avatar-placeholder {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  border: 2px dashed var(--apple-border);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--apple-gray);
  transition: all 0.3s;
}

.avatar-placeholder:hover {
  border-color: var(--apple-blue);
  color: var(--apple-blue);
}

.avatar-placeholder span {
  font-size: 12px;
  margin-top: 4px;
}

/* 响应式 - 平板 */
@media (max-width: 992px) {
  .profile-view {
    padding: 16px;
  }

  .profile-container {
    padding: 24px;
  }

  .page-title {
    font-size: 22px;
    margin-bottom: 20px;
  }

  .profile-layout {
    grid-template-columns: 240px 1fr;
    gap: 24px;
  }

  .profile-sidebar {
    padding: 20px;
    height: auto;
  }

  .user-card {
    padding-bottom: 20px;
    margin-bottom: 12px;
  }

  .user-name {
    font-size: 16px;
  }

  .profile-content {
    padding: 24px;
    height: auto;
    min-height: 500px;
  }

  .section-title {
    font-size: 17px;
    margin-bottom: 20px;
    padding-bottom: 14px;
  }

  .avatar-preview,
  .avatar-placeholder {
    width: 90px;
    height: 90px;
  }
}

/* 响应式 - 手机 */
@media (max-width: 768px) {
  .profile-view {
    padding: 12px;
  }

  .profile-container {
    padding: 20px;
  }

  .page-title {
    font-size: 20px;
    margin-bottom: 16px;
  }

  .profile-layout {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .profile-sidebar {
    padding: 16px;
    height: auto;
  }

  .user-card {
    padding-bottom: 16px;
    margin-bottom: 12px;
  }

  .user-name {
    font-size: 16px;
    margin-top: 10px;
  }

  .user-phone {
    font-size: 13px;
  }

  .profile-menu {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .profile-menu :deep(.el-menu-item) {
    margin-bottom: 0;
    flex: 1;
    min-width: 100px;
    justify-content: center;
  }

  .profile-content {
    padding: 20px;
    height: auto;
    min-height: 400px;
  }

  .section-title {
    font-size: 16px;
    margin-bottom: 16px;
    padding-bottom: 12px;
  }

  .profile-form :deep(.el-form-item__label) {
    float: none;
    display: block;
    text-align: left;
    padding: 0 0 8px;
  }

  .profile-form :deep(.el-form-item__content) {
    margin-left: 0 !important;
  }

  .avatar-preview,
  .avatar-placeholder {
    width: 80px;
    height: 80px;
  }

  .address-card {
    padding: 16px;
  }

  .address-header {
    gap: 8px;
  }

  .address-actions {
    gap: 12px;
  }
}

/* 响应式 - 超小屏幕 */
@media (max-width: 480px) {
  .profile-view {
    padding: 8px;
  }

  .profile-container {
    padding: 16px;
    border-radius: var(--apple-radius);
  }

  .page-title {
    font-size: 18px;
    margin-bottom: 12px;
  }

  .profile-layout {
    gap: 16px;
  }

  .profile-sidebar {
    padding: 12px;
  }

  .user-card {
    padding-bottom: 12px;
    margin-bottom: 8px;
  }

  .user-name {
    font-size: 15px;
    margin-top: 8px;
  }

  .user-phone {
    font-size: 12px;
  }

  .profile-menu :deep(.el-menu-item) {
    min-width: 80px;
    font-size: 13px;
    padding: 0 8px;
  }

  .profile-menu :deep(.el-icon) {
    font-size: 16px;
  }

  .profile-content {
    padding: 16px;
    min-height: 350px;
  }

  .section-title {
    font-size: 15px;
    margin-bottom: 12px;
    padding-bottom: 10px;
  }

  .avatar-preview,
  .avatar-placeholder {
    width: 70px;
    height: 70px;
  }

  .avatar-placeholder span {
    font-size: 11px;
  }

  .address-card {
    padding: 12px;
  }

  .address-name {
    font-size: 14px;
  }

  .address-phone {
    font-size: 12px;
  }

  .address-detail {
    font-size: 13px;
  }
}
</style>
