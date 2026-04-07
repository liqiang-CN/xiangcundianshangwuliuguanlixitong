<template>
  <div class="report-view">
    <div class="report-container">
      <!-- 面包屑 -->
      <el-breadcrumb class="breadcrumb">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/products' }">商品</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: `/product/${productId}` }">商品详情</el-breadcrumb-item>
        <el-breadcrumb-item>举报商品</el-breadcrumb-item>
      </el-breadcrumb>

      <el-card class="report-card" shadow="never">
        <template #header>
          <div class="card-header">
            <h2>举报商品</h2>
          </div>
        </template>

        <!-- 被举报商品信息 -->
        <div class="product-info-section">
          <h3>被举报商品</h3>
          <div class="product-info" v-if="product.id">
            <img :src="getImageUrl(product.mainImage || product.image)" class="product-image" />
            <div class="product-detail">
              <p class="product-name">{{ product.name }}</p>
              <p class="product-price">¥{{ product.price }}</p>
              <p class="product-farmer">农户：{{ farmerName }}</p>
            </div>
          </div>
        </div>

        <el-divider />

        <!-- 举报表单 -->
        <el-form :model="reportForm" :rules="rules" ref="reportFormRef" label-position="top">
          <el-form-item label="举报类型" prop="reportType">
            <el-radio-group v-model="reportForm.reportType">
              <el-radio :label="1">虚假信息</el-radio>
              <el-radio :label="2">侵权</el-radio>
              <el-radio :label="3">违禁品</el-radio>
              <el-radio :label="4">其他</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="举报内容" prop="reportContent">
            <el-input
              v-model="reportForm.reportContent"
              type="textarea"
              :rows="4"
              placeholder="请详细描述举报原因，以便我们更好地处理..."
              maxlength="500"
              show-word-limit
            />
          </el-form-item>

          <el-form-item label="上传图片（可选）">
            <el-upload
              action="/api/file/upload"
              list-type="picture-card"
              :headers="uploadHeaders"
              :on-success="handleUploadSuccess"
              :on-remove="handleRemove"
              :file-list="fileList"
              :limit="3"
              accept="image/*"
            >
              <el-icon><Plus /></el-icon>
              <template #tip>
                <div class="upload-tip">最多上传3张图片，每张不超过5MB</div>
              </template>
            </el-upload>
          </el-form-item>
        </el-form>

        <div class="form-actions">
          <el-button @click="goBack">取消</el-button>
          <el-button type="primary" @click="submitReport" :loading="submitting">提交举报</el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '@/api/request'
import { useUserStore } from '@/stores/user'
import { getImageUrl } from '@/utils/image'

const userStore = useUserStore()
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${userStore.token}`
}))

const route = useRoute()
const router = useRouter()
const productId = route.params.id as string

const product = ref<any>({})
const farmerName = ref('')
const reportFormRef = ref()
const submitting = ref(false)
const fileList = ref<any[]>([])

const reportForm = reactive({
  reportType: 1,
  reportContent: '',
  reportImages: ''
})

const rules = {
  reportType: [{ required: true, message: '请选择举报类型', trigger: 'change' }],
  reportContent: [
    { required: true, message: '请填写举报内容', trigger: 'blur' },
    { min: 10, message: '举报内容至少10个字符', trigger: 'blur' }
  ]
}

// 获取商品信息
const fetchProductInfo = async () => {
  try {
    const data: any = await request.get(`/product/${productId}`)
    product.value = data
    // 从商品数据中获取农户名称
    farmerName.value = data.farmerName || data.farmerNickname || '未知农户'
  } catch (error) {
    console.error('获取商品信息失败:', error)
    ElMessage.error('获取商品信息失败')
  }
}

// 处理图片上传成功
const handleUploadSuccess = (response: any, file: any) => {
  console.log('上传成功响应:', response)
  console.log('上传成功文件:', file)
  // response 是后端返回的数据，结构是 { code: 200, message: 'success', data: { url: '...' } }
  if (response && response.code === 200 && response.data && response.data.url) {
    fileList.value.push({ url: response.data.url, response: response.data })
    updateReportImages()
    ElMessage.success('图片上传成功')
  } else {
    ElMessage.error('图片上传失败')
  }
}

// 处理图片删除
const handleRemove = () => {
  updateReportImages()
}

// 更新举报图片
const updateReportImages = () => {
  console.log('当前fileList:', fileList.value)
  const images = fileList.value.map(file => {
    // 优先使用 response 中的 url，然后是 file 本身的 url
    const url = file.response?.url || file.url
    console.log('图片URL:', url)
    return url
  }).filter(url => url) // 过滤掉空值
  console.log('解析后的图片列表:', images)
  reportForm.reportImages = JSON.stringify(images)
  console.log('reportImages:', reportForm.reportImages)
}

// 提交举报
const submitReport = async () => {
  const valid = await reportFormRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const submitData = {
      productId: Number(productId),
      reportType: reportForm.reportType,
      reportContent: reportForm.reportContent,
      reportImages: reportForm.reportImages || ''
    }
    console.log('提交举报数据:', submitData)
    console.log('请求URL:', '/api/report/product')
    const response = await request.post('/report/product', submitData, {
      headers: {
        'Content-Type': 'application/json'
      }
    })
    console.log('提交成功:', response)
    ElMessage.success('举报提交成功，我们会尽快处理')
    router.push(`/product/${productId}`)
  } catch (error: any) {
    console.error('举报提交失败:', error)
    console.error('错误响应:', error.response)
    console.error('错误数据:', error.response?.data)
    ElMessage.error(error.response?.data?.message || error.message || '举报提交失败')
  } finally {
    submitting.value = false
  }
}

// 返回上一页
const goBack = () => {
  ElMessageBox.confirm('确定要取消举报吗？填写的内容将不会保存', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '继续填写',
    type: 'warning'
  }).then(() => {
    router.back()
  }).catch(() => {})
}

onMounted(() => {
  fetchProductInfo()
})
</script>

<style scoped>
.report-view {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 20px 0;
}

.report-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 20px;
}

.breadcrumb {
  margin-bottom: 20px;
}

.report-card {
  background: #fff;
  border-radius: 8px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-header h2 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.product-info-section {
  margin-bottom: 20px;
}

.product-info-section h3 {
  margin: 0 0 15px 0;
  font-size: 16px;
  color: #303133;
}

.product-info {
  display: flex;
  gap: 15px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 8px;
}

.product-image {
  width: 100px;
  height: 100px;
  object-fit: cover;
  border-radius: 4px;
}

.product-detail {
  flex: 1;
}

.product-name {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.product-price {
  margin: 0 0 8px 0;
  font-size: 18px;
  color: #f56c6c;
  font-weight: 600;
}

.product-farmer {
  margin: 0;
  font-size: 14px;
  color: #909399;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}

.form-actions {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}
</style>
