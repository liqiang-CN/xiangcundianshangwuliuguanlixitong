<template>
  <div class="password-view">
    <div class="page-header">
      <h2 class="page-title">修改密码</h2>
      <p class="page-desc">为了您的账户安全，建议定期更换密码</p>
    </div>

    <el-card class="password-card" shadow="never">
      <div class="card-header">
        <el-icon size="24" color="#34c759"><Lock /></el-icon>
        <span class="card-title">密码设置</span>
      </div>

      <el-form
        ref="formRef"
        :model="passwordForm"
        :rules="formRules"
        label-width="100px"
        class="password-form"
        status-icon
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            show-password
            placeholder="请输入原密码"
            size="large"
          />
        </el-form-item>

        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            show-password
            placeholder="请输入新密码（至少6位）"
            size="large"
          />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            show-password
            placeholder="请再次输入新密码"
            size="large"
          />
        </el-form-item>

        <el-form-item label-width="0" style="width: 100%">
          <div class="form-actions">
            <el-button
              type="primary"
              size="large"
              @click="updatePassword"
              :loading="loading"
              class="submit-btn"
            >
              <el-icon><Check /></el-icon>
              确认修改
            </el-button>
            <el-button size="large" @click="resetForm">
              <el-icon><RefreshRight /></el-icon>
              重置
            </el-button>
          </div>
        </el-form-item>
      </el-form>

      <div class="password-tips">
        <h4><el-icon><InfoFilled /></el-icon> 密码安全提示</h4>
        <ul>
          <li>密码长度至少为6位</li>
          <li>建议包含字母、数字和特殊字符</li>
          <li>不要使用与其他网站相同的密码</li>
          <li>修改密码后需要重新登录</li>
        </ul>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Lock, Check, RefreshRight, InfoFilled } from '@element-plus/icons-vue'
import request from '@/api/request'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 自定义验证规则
const validateConfirmPassword = (rule: any, value: string, callback: any) => {
  if (value === '') {
    callback(new Error('请再次输入新密码'))
  } else if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const formRules: FormRules = {
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const updatePassword = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await request.put('/user/password', {
          oldPassword: passwordForm.oldPassword,
          newPassword: passwordForm.newPassword
        })

        ElMessage.success('密码修改成功，请重新登录')

        // 清空密码表单
        resetForm()

        // 修改密码后退出登录并跳转到登录页面
        userStore.logout()
        router.replace('/login')
      } catch (error: any) {
        console.error('修改密码失败:', error)
        ElMessage.error(error.message || '修改密码失败，请检查原密码是否正确')
      } finally {
        loading.value = false
      }
    }
  })
}

const resetForm = () => {
  if (formRef.value) {
    formRef.value.resetFields()
  }
}
</script>

<style scoped>
.password-view {
  padding: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: calc(100vh - 48px);
  box-sizing: border-box;
}

.page-header {
  text-align: center;
  margin-bottom: 32px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0 0 8px 0;
}

.page-desc {
  font-size: 14px;
  color: #86868b;
  margin: 0;
}

.password-card {
  width: 100%;
  max-width: 800px;
  min-height: 600px;
  border-radius: 12px;
  border: none;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 24px;
  border-bottom: 1px solid #f0f0f0;
  margin: -20px -24px 24px -24px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1d1d1f;
}

.password-form {
  padding: 0 24px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
  color: #1d1d1f;
}

:deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px #d1d1d6 inset;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #34c759 inset;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #34c759 inset;
}

.form-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 24px;
  width: 100%;
}

.submit-btn {
  background: #34c759;
  border-color: #34c759;
}

.submit-btn:hover {
  background: #2db14d;
  border-color: #2db14d;
}

.password-tips {
  margin-top: 32px;
  padding: 20px 24px;
  background: #f5f5f7;
  border-radius: 8px;
}

.password-tips h4 {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0 0 12px 0;
}

.password-tips ul {
  margin: 0;
  padding-left: 20px;
  color: #86868b;
  font-size: 13px;
  line-height: 1.8;
}

.password-tips li {
  margin-bottom: 4px;
}
</style>
