<template>
  <div class="profile-view">
    <h2 class="page-title">个人中心</h2>

    <el-row :gutter="20">
      <!-- 左侧：头像和基本信息 -->
      <el-col :span="8">
        <el-card class="profile-card">
          <div class="status-toggle-btn">
            <el-button
              v-if="userInfo.status === 1"
              type="danger"
              @click="toggleStatus(0)"
              :loading="togglingStatus"
            >
              <el-icon><CircleClose /></el-icon>
              下线
            </el-button>
            <el-button
              v-else
              type="success"
              @click="toggleStatus(1)"
              :loading="togglingStatus"
            >
              <el-icon><CircleCheck /></el-icon>
              上线
            </el-button>
          </div>
          <div class="avatar-section">
            <div class="avatar-upload-wrapper">
              <img
                v-if="userInfo.avatar"
                :src="userInfo.avatar"
                class="avatar-preview"
                @click="triggerAvatarUpload"
              />
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
          </div>
          <div class="user-info">
            <h3>{{ userInfo.nickname || '配送员' }}</h3>
            <p class="user-type">物流配送员</p>
            <p class="user-status">
              <el-tag :type="userInfo.status === 1 ? 'success' : 'info'">
                {{ userInfo.status === 1 ? '在职' : '休息中' }}
              </el-tag>
            </p>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：编辑表单 -->
      <el-col :span="16">
        <el-card class="edit-card">
          <template #header>
            <div class="card-header">
              <span>基本信息</span>
              <el-button type="primary" @click="handleSave" :loading="saving">
                保存修改
              </el-button>
            </div>
          </template>

          <el-form :model="form" label-width="100px" class="profile-form">
            <el-form-item label="昵称">
              <el-input v-model="form.nickname" placeholder="请输入昵称" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="form.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="工作区域">
              <el-input v-model="form.location" placeholder="请输入工作区域，如：朝阳区、海淀区" />
            </el-form-item>
          </el-form>
        </el-card>

        <el-card class="password-card" style="margin-top: 20px;">
          <template #header>
            <div class="card-header">
              <span>修改密码</span>
              <el-button type="primary" @click="handleChangePassword" :loading="changingPassword">
                确认修改
              </el-button>
            </div>
          </template>

          <el-form :model="passwordForm" label-width="100px" class="password-form">
            <el-form-item label="原密码">
              <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入原密码" show-password />
            </el-form-item>
            <el-form-item label="新密码">
              <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" show-password />
            </el-form-item>
            <el-form-item label="确认密码">
              <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, CircleClose, CircleCheck } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import request from '@/api/request'

const userStore = useUserStore()

const userInfo = ref<any>({})
const saving = ref(false)
const changingPassword = ref(false)
const togglingStatus = ref(false)
const avatarInputRef = ref<HTMLInputElement>()

const form = reactive({
  nickname: '',
  phone: '',
  location: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 获取用户信息
const fetchUserInfo = async () => {
  try {
    const res: any = await request.get('/user/info')
    if (res) {
      userInfo.value = res
      form.nickname = res.nickname || ''
      form.phone = res.phone || ''
      form.location = res.location || ''
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
    ElMessage.error('获取用户信息失败')
  }
}

// 触发头像文件选择
const triggerAvatarUpload = () => {
  avatarInputRef.value?.click()
}

// 处理头像文件选择
const handleAvatarChange = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]

  if (!file) return

  // 检查文件类型和大小
  const isJPG = file.type === 'image/jpeg'
  const isPNG = file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isJPG && !isPNG) {
    ElMessage.error('头像只能是 JPG 或 PNG 格式!')
    return
  }
  if (!isLt2M) {
    ElMessage.error('头像大小不能超过 2MB!')
    return
  }

  // 上传文件
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
      ElMessage.success('头像上传成功')
      // 自动保存头像到用户信息
      await handleSave()
    } else {
      ElMessage.error('上传失败')
    }
  } catch (error) {
    console.error('上传失败:', error)
    ElMessage.error('上传失败')
  }

  // 清空input，允许重复选择同一文件
  if (avatarInputRef.value) {
    avatarInputRef.value.value = ''
  }
}

// 保存基本信息
const handleSave = async () => {
  saving.value = true
  try {
    const res: any = await request.put('/user/info', {
      id: userInfo.value.id,
      nickname: form.nickname,
      phone: form.phone,
      location: form.location,
      avatar: userInfo.value.avatar
    })
    if (res) {
      ElMessage.success('保存成功')
      // 更新本地存储的用户信息
      if (userStore.userInfo) {
        userStore.setUserInfo({
          ...userStore.userInfo,
          nickname: form.nickname,
          avatar: userInfo.value.avatar
        })
      }
      fetchUserInfo()
    }
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

// 修改密码
const handleChangePassword = async () => {
  if (!passwordForm.oldPassword) {
    ElMessage.error('请输入原密码')
    return
  }
  if (!passwordForm.newPassword) {
    ElMessage.error('请输入新密码')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.error('两次输入的密码不一致')
    return
  }
  if (passwordForm.newPassword.length < 6) {
    ElMessage.error('新密码长度不能少于6位')
    return
  }

  changingPassword.value = true
  try {
    const res: any = await request.put('/user/password', {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    if (res) {
      ElMessage.success('密码修改成功')
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
    }
  } catch (error) {
    console.error('密码修改失败:', error)
    ElMessage.error('密码修改失败')
  } finally {
    changingPassword.value = false
  }
}

// 切换在线状态
const toggleStatus = async (status: number) => {
  togglingStatus.value = true
  try {
    const res: any = await request.put('/user/info', {
      id: userInfo.value.id,
      status: status
    })
    if (res) {
      ElMessage.success(status === 1 ? '已上线，开始接单' : '已下线，休息中')
      userInfo.value.status = status
      // 更新本地存储
      if (userStore.userInfo) {
        userStore.setUserInfo({
          ...userStore.userInfo,
          status: status
        })
      }
    }
  } catch (error) {
    console.error('状态切换失败:', error)
    ElMessage.error('状态切换失败')
  } finally {
    togglingStatus.value = false
  }
}

onMounted(() => {
  fetchUserInfo()
})
</script>

<style scoped>
.profile-view {
  padding: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 20px;
}

:deep(.el-row) {
  align-items: stretch;
}

:deep(.el-col) {
  display: flex;
  flex-direction: column;
}

.profile-card {
  text-align: center;
  padding: 20px;
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  box-shadow: none !important;
  border: 1px solid #e4e7ed;
  border-radius: 12px;
  position: relative;
}

:deep(.profile-card .el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  width: 100%;
}

:deep(.edit-card),
:deep(.password-card) {
  box-shadow: none !important;
  border: 1px solid #e4e7ed;
  border-radius: 12px;
}

:deep(.edit-card) {
  flex: 1;
}

:deep(.password-card) {
  margin-top: 20px;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

/* 头像上传样式 */
.avatar-upload-wrapper {
  display: inline-block;
}

.avatar-preview {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  object-fit: cover;
  cursor: pointer;
  border: 2px solid #e4e7ed;
  transition: all 0.3s;
}

.avatar-preview:hover {
  border-color: #409eff;
  opacity: 0.8;
}

.avatar-placeholder {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  border: 2px dashed #dcdfe6;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #909399;
  transition: all 0.3s;
  background-color: #f5f7fa;
}

.avatar-placeholder:hover {
  border-color: #409eff;
  color: #409eff;
}

.avatar-placeholder span {
  font-size: 14px;
  margin-top: 4px;
}

.user-info h3 {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 12px;
}

.user-type {
  font-size: 16px;
  color: #909399;
  margin: 0 0 16px;
}

.user-status {
  margin: 0;
}

.user-status .el-tag {
  font-size: 14px;
  padding: 6px 16px;
}

.status-toggle-btn {
  position: absolute;
  top: 16px;
  right: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.profile-form,
.password-form {
  max-width: 500px;
}
</style>
