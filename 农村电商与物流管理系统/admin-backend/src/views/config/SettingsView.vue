<template>
  <div class="settings-view">
    <div class="page-header">
      <h2 class="page-title">系统设置</h2>
    </div>

    <el-tabs v-model="activeTab" class="settings-tabs">
      <el-tab-pane label="基础设置" name="basic">
        <el-card>
          <el-form :model="basicForm" label-width="150px">
            <el-form-item label="系统名称">
              <el-input v-model="basicForm.siteName" placeholder="请输入系统名称" />
            </el-form-item>
            <el-form-item label="系统Logo">
              <el-upload 
                class="logo-uploader" 
                action="/api/file/upload" 
                name="file"
                :show-file-list="false" 
                :on-success="handleLogoSuccess"
                :on-error="handleLogoError"
              >
                <img v-if="basicForm.logo" :src="getImageUrl(basicForm.logo)" class="logo-preview" />
                <el-icon v-else class="logo-uploader-icon"><Plus /></el-icon>
              </el-upload>
            </el-form-item>
            <el-form-item label="客服电话">
              <el-input v-model="basicForm.servicePhone" placeholder="请输入客服电话" />
            </el-form-item>
            <el-form-item label="备案号">
              <el-input v-model="basicForm.icp" placeholder="请输入备案号" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveBasic">保存设置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="支付设置" name="payment">
        <el-card>
          <el-form :model="paymentForm" label-width="150px">
            <el-form-item label="微信支付">
              <el-switch v-model="paymentForm.wechatEnabled" />
            </el-form-item>
            <el-form-item label="微信支付AppID">
              <el-input v-model="paymentForm.wechatAppId" placeholder="请输入AppID" />
            </el-form-item>
            <el-form-item label="微信支付密钥">
              <el-input v-model="paymentForm.wechatSecret" type="password" placeholder="请输入密钥" />
            </el-form-item>
            <el-form-item label="支付宝">
              <el-switch v-model="paymentForm.alipayEnabled" />
            </el-form-item>
            <el-form-item label="支付宝AppID">
              <el-input v-model="paymentForm.alipayAppId" placeholder="请输入AppID" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="savePayment">保存设置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>


    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '@/api/request'
import { getImageUrl } from '@/utils/image'

const activeTab = ref('basic')
const loading = ref(false)

const basicForm = reactive({
  siteName: '',
  logo: '',
  servicePhone: '',
  icp: ''
})

const paymentForm = reactive({
  wechatEnabled: false,
  wechatAppId: '',
  wechatSecret: '',
  alipayEnabled: false,
  alipayAppId: ''
})

// 获取系统配置
const fetchConfig = async () => {
  try {
    const config = await request.get('/admin/config') as Record<string, string>
    // 基础设置
    basicForm.siteName = config['site.name'] || ''
    basicForm.logo = config['site.logo'] || ''
    basicForm.servicePhone = config['contact.phone'] || ''
    basicForm.icp = config['site.icp'] || ''
  } catch (error) {
    console.error('获取配置失败:', error)
    ElMessage.error('获取配置失败')
  }
}

const handleLogoSuccess = (res: any) => {
  // 处理上传成功响应
  if (res && res.url) {
    basicForm.logo = res.url
    ElMessage.success('上传成功')
  } else if (res && res.data && res.data.url) {
    basicForm.logo = res.data.url
    ElMessage.success('上传成功')
  } else {
    ElMessage.error('上传失败')
  }
}

const handleLogoError = () => {
  ElMessage.error('上传失败')
}

const saveBasic = async () => {
  try {
    loading.value = true
    const config = {
      'site.name': basicForm.siteName,
      'site.logo': basicForm.logo,
      'contact.phone': basicForm.servicePhone,
      'site.icp': basicForm.icp
    }
    await request.post('/admin/config', config)
    ElMessage.success('保存成功')
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  } finally {
    loading.value = false
  }
}

const savePayment = () => {
  ElMessage.success('保存成功')
}

onMounted(() => {
  fetchConfig()
})
</script>

<style scoped>
.settings-view {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.settings-tabs :deep(.el-tabs__header) {
  margin-bottom: 20px;
}

.logo-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 120px;
  height: 120px;
}

.logo-uploader:hover {
  border-color: #409eff;
}

.logo-preview {
  width: 120px;
  height: 120px;
  display: block;
  object-fit: contain;
}

.logo-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 120px;
  height: 120px;
  text-align: center;
  line-height: 120px;
}
</style>
