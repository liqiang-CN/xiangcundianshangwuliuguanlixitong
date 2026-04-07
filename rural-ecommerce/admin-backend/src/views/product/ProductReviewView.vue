<template>
  <div class="product-review-view">
    <div class="page-header">
      <h2 class="page-title">商品审核</h2>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <p class="stat-label">待处理举报</p>
          <p class="stat-value">{{ stats.pending }}</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <p class="stat-label">今日举报</p>
          <p class="stat-value">{{ stats.today }}</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <p class="stat-label">已通过</p>
          <p class="stat-value">{{ stats.approved }}</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <p class="stat-label">已拒绝</p>
          <p class="stat-value">{{ stats.rejected }}</p>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选栏 -->
    <el-card class="filter-card">
      <el-form :model="filterForm" inline class="filter-form">
        <el-form-item label="处理状态" class="filter-item">
          <el-select v-model="filterForm.status" placeholder="请选择状态" clearable class="filter-select">
            <el-option label="待处理" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已拒绝" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item class="filter-item filter-buttons">
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 举报列表 -->
    <el-card>
      <el-table :data="reportList" v-loading="loading">
        <el-table-column prop="id" label="举报ID" width="80" align="center" />
        <el-table-column label="被举报商品" min-width="250">
          <template #default="{ row }">
            <div class="product-info">
              <img :src="getImageUrl(row.productImage)" class="product-image" />
              <div class="product-detail">
                <p class="product-name">{{ row.productName }}</p>
                <p class="product-price">¥{{ row.productPrice }}<span v-if="row.productUnit" class="unit">/{{ row.productUnit }}</span></p>
                <p class="farmer-name">农户：{{ row.farmerName || '-' }}</p>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="举报信息" min-width="200">
          <template #default="{ row }">
            <div class="report-info">
              <p><strong>类型：</strong>{{ getReportTypeText(row.reportType) }}</p>
              <p><strong>举报人：</strong>{{ row.reporterName || '-' }}</p>
              <p class="report-content" :title="row.reportContent">{{ row.reportContent }}</p>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="举报时间" width="160" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="viewDetail(row)">查看</el-button>
            <template v-if="row.status === 0">
              <el-button type="success" size="small" @click="handleApprove(row)">通过</el-button>
              <el-button type="danger" size="small" @click="handleReject(row)">拒绝</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="showDetailDialog" title="举报详情" width="900px" top="5vh">
      <div v-if="currentReport" class="report-detail">
        <!-- 被举报商品 -->
        <div class="detail-section">
          <h4>被举报商品</h4>
          <div class="product-detail-info">
            <img :src="getImageUrl(currentReport.product?.mainImage || currentReport.report?.productImage)" class="detail-product-image" />
            <div class="detail-product-text">
              <p><strong>商品名称：</strong>{{ currentReport.product?.name || currentReport.report?.productName }}</p>
              <p><strong>商品价格：</strong>¥{{ currentReport.product?.price || currentReport.report?.productPrice }}<span v-if="currentReport.product?.unit || currentReport.report?.productUnit">/{{ currentReport.product?.unit || currentReport.report?.productUnit }}</span></p>
              <p v-if="currentReport.product?.stock !== undefined"><strong>库存：</strong>{{ currentReport.product.stock }}</p>
              <p v-if="currentReport.product?.sales !== undefined"><strong>销量：</strong>{{ currentReport.product.sales }}</p>
              <p v-if="currentReport.product?.description"><strong>商品描述：</strong>{{ currentReport.product.description }}</p>
            </div>
          </div>
          <!-- 商品图片列表 -->
          <div v-if="currentReport.product?.images" class="product-images-section">
            <p><strong>商品图片：</strong></p>
            <div class="image-list">
              <img
                v-for="(img, index) in parseImages(currentReport.product.images)"
                :key="index"
                :src="getImageUrl(img)"
                class="product-gallery-image"
                @click="previewImage(img)"
              />
            </div>
          </div>
          
          <!-- 商品介绍 -->
          <div v-if="currentReport.product?.introductionList && currentReport.product.introductionList.length > 0" class="product-introduction-section">
            <p><strong>商品介绍：</strong></p>
            <div class="introduction-list">
              <div v-for="(item, index) in currentReport.product.introductionList" :key="index" class="introduction-item">
                <!-- 文本+图片模式 -->
                <template v-if="item.type === 'text_image'">
                  <div v-if="item.title" class="intro-title">{{ item.title }}</div>
                  <div v-if="item.content" class="intro-content">{{ item.content }}</div>
                  <img v-if="item.imageUrl" :src="getImageUrl(item.imageUrl)" class="intro-image" @click="previewImage(item.imageUrl)" />
                </template>
                <!-- 仅图片模式 -->
                <template v-else-if="item.type === 'image'">
                  <img v-if="item.imageUrl" :src="getImageUrl(item.imageUrl)" class="intro-image" @click="previewImage(item.imageUrl)" />
                </template>
              </div>
            </div>
          </div>
        </div>

        <!-- 商家/店铺信息 -->
        <div class="detail-section">
          <h4>商家信息</h4>
          <div class="shop-info">
            <div class="shop-header">
              <img :src="getImageUrl(currentReport.shop?.avatar)" class="shop-avatar" />
              <div class="shop-basic">
                <p class="shop-name">{{ currentReport.shop?.shopName || currentReport.shop?.nickname || currentReport.report?.farmerName || '-' }}</p>
                <p v-if="currentReport.shop?.username"><strong>用户名：</strong>{{ currentReport.shop.username }}</p>
              </div>
            </div>
            <div class="shop-details">
              <el-row :gutter="20">
                <el-col :span="12">
                  <p v-if="currentReport.shop?.location"><strong>所在地：</strong>{{ currentReport.shop.location }}</p>
                  <p v-if="currentReport.shop?.mainCategory"><strong>主营类目：</strong>{{ currentReport.shop.mainCategory }}</p>
                  <p v-if="currentReport.shop?.phone"><strong>联系电话：</strong>{{ currentReport.shop.phone }}</p>
                  <p v-if="currentReport.shop?.email"><strong>邮箱：</strong>{{ currentReport.shop.email }}</p>
                </el-col>
                <el-col :span="12">
                  <p v-if="currentReport.shop?.rating"><strong>店铺评分：</strong><el-rate :model-value="currentReport.shop.rating" disabled show-score text-color="#ff9900" /></p>
                  <p v-if="currentReport.shop?.followers !== undefined"><strong>关注人数：</strong>{{ currentReport.shop.followers }}</p>
                  <p v-if="currentReport.shop?.status !== undefined"><strong>店铺状态：</strong><el-tag :type="currentReport.shop.status === 1 ? 'success' : 'danger'">{{ currentReport.shop.status === 1 ? '正常' : '禁用' }}</el-tag></p>
                  <p v-if="currentReport.shop?.createTime"><strong>入驻时间：</strong>{{ formatDate(currentReport.shop.createTime) }}</p>
                </el-col>
              </el-row>
              <div v-if="currentReport.shop?.tags" class="shop-tags">
                <strong>店铺标签：</strong>
                <el-tag v-for="(tag, index) in parseTags(currentReport.shop.tags)" :key="index" size="small" class="shop-tag">{{ tag }}</el-tag>
              </div>
              <p v-if="currentReport.shop?.description" class="shop-description"><strong>店铺简介：</strong>{{ currentReport.shop.description }}</p>
            </div>
          </div>
        </div>

        <!-- 举报信息 -->
        <div class="detail-section">
          <h4>举报信息</h4>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="举报类型">{{ getReportTypeText(currentReport.report?.reportType) }}</el-descriptions-item>
            <el-descriptions-item label="举报人">{{ currentReport.report?.reporterName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="举报时间">{{ currentReport.report?.createTime }}</el-descriptions-item>
            <el-descriptions-item label="举报ID">{{ currentReport.report?.id }}</el-descriptions-item>
          </el-descriptions>
          <div class="detail-content-box">
            <p><strong>举报内容：</strong></p>
            <p class="detail-content">{{ currentReport.report?.reportContent }}</p>
          </div>
          <!-- 用户提交的图片 -->
          <div v-if="currentReport.report?.reportImages && currentReport.report.reportImages !== '[]' && currentReport.report.reportImages !== ''" class="detail-images">
            <p><strong>用户提交的举报图片：</strong></p>
            <div class="image-list">
              <img
                v-for="(img, index) in parseImages(currentReport.report.reportImages)"
                :key="index"
                :src="getImageUrl(img)"
                class="report-image"
                @click="previewImage(img)"
              />
            </div>
          </div>
          <div v-else class="detail-images">
            <p><strong>用户提交的举报图片：</strong><span style="color: #909399;">未上传图片</span></p>
          </div>
        </div>

        <!-- 处理结果 -->
        <div v-if="currentReport.report?.status !== 0" class="detail-section">
          <h4>处理结果</h4>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="处理状态">
              <el-tag :type="getStatusType(currentReport.report.status)">{{ getStatusText(currentReport.report.status) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item v-if="currentReport.report?.handleTime" label="处理时间">{{ currentReport.report.handleTime }}</el-descriptions-item>
          </el-descriptions>
          <p v-if="currentReport.report?.adminRemark" class="admin-remark"><strong>管理员备注：</strong>{{ currentReport.report.adminRemark }}</p>
        </div>
      </div>
    </el-dialog>

    <!-- 处理弹窗 -->
    <el-dialog v-model="showHandleDialog" :title="handleType === 'approve' ? '通过举报' : '拒绝举报'" width="500px">
      <el-form :model="handleForm" label-position="top">
        <el-form-item label="管理员备注">
          <el-input
            v-model="handleForm.adminRemark"
            type="textarea"
            :rows="3"
            placeholder="请输入处理备注..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showHandleDialog = false">取消</el-button>
        <el-button :type="handleType === 'approve' ? 'success' : 'danger'" @click="confirmHandle" :loading="handleLoading">
          {{ handleType === 'approve' ? '确认通过' : '确认拒绝' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 图片预览 -->
    <el-dialog v-model="showPreviewDialog" title="图片预览" width="800px">
      <img :src="previewImageUrl" style="width: 100%;" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'

const loading = ref(false)
const reportList = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const filterForm = reactive({
  status: null as number | null
})

const stats = reactive({
  pending: 0,
  today: 0,
  approved: 0,
  rejected: 0
})

// 详情弹窗
const showDetailDialog = ref(false)
const currentReport = ref<any>(null)

// 处理弹窗
const showHandleDialog = ref(false)
const handleType = ref<'approve' | 'reject'>('approve')
const handleForm = reactive({
  adminRemark: ''
})
const handleLoading = ref(false)
const currentHandleId = ref<number>(0)

// 图片预览
const showPreviewDialog = ref(false)
const previewImageUrl = ref('')

// 获取举报列表
const fetchReportList = async () => {
  loading.value = true
  try {
    const params: any = {
      page: page.value,
      size: pageSize.value
    }
    if (filterForm.status !== null) {
      params.status = filterForm.status
    }
    const data: any = await request.get('/report/list', { params })
    reportList.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    console.error('获取举报列表失败:', error)
    ElMessage.error('获取举报列表失败')
  } finally {
    loading.value = false
  }
}

// 获取统计数据
const fetchStats = async () => {
  try {
    const data: any = await request.get('/report/stats')
    stats.pending = data.pending || 0
    stats.today = data.today || 0
    stats.approved = data.approved || 0
    stats.rejected = data.rejected || 0
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

// 获取举报类型文本
const getReportTypeText = (type: number) => {
  const map: Record<number, string> = {
    1: '虚假信息',
    2: '侵权',
    3: '违禁品',
    4: '其他'
  }
  return map[type] || '未知'
}

// 获取状态类型
const getStatusType = (status: number) => {
  const map: Record<number, string> = {
    0: 'warning',
    1: 'success',
    2: 'danger'
  }
  return map[status] || 'info'
}

// 获取状态文本
const getStatusText = (status: number) => {
  const map: Record<number, string> = {
    0: '待处理',
    1: '已通过',
    2: '已拒绝'
  }
  return map[status] || '未知'
}

// 处理图片URL
const getImageUrl = (url?: string) => {
  if (!url) return '/images/default-product.jpg'
  if (url.startsWith('http')) return url
  if (url.startsWith('/images/')) return url
  if (url.startsWith('/uploads/')) {
    return url // 使用相对路径让代理处理
  }
  return url
}

// 解析图片JSON
const parseImages = (images: string | string[]): string[] => {
  if (!images) return []
  if (Array.isArray(images)) return images
  try {
    const parsed = JSON.parse(images)
    return Array.isArray(parsed) ? parsed : [parsed]
  } catch {
    // 如果不是JSON，可能是逗号分隔的字符串
    return images.split(',').filter(img => img.trim())
  }
}

// 解析店铺标签
const parseTags = (tags: string | string[]): string[] => {
  if (!tags) return []
  if (Array.isArray(tags)) return tags
  try {
    const parsed = JSON.parse(tags)
    return Array.isArray(parsed) ? parsed : [parsed]
  } catch {
    return tags.split(',').filter(tag => tag.trim())
  }
}

// 格式化日期
const formatDate = (date: string | Date): string => {
  if (!date) return '-'
  const d = new Date(date)
  return d.toLocaleDateString('zh-CN')
}

// 查看详情
const viewDetail = async (row: any) => {
  try {
    const data: any = await request.get(`/report/${row.id}`)
    console.log('举报详情数据:', data)
    console.log('reportImages:', data.report?.reportImages)
    currentReport.value = data
    showDetailDialog.value = true
  } catch (error) {
    ElMessage.error('获取举报详情失败')
  }
}

// 预览图片
const previewImage = (url: string) => {
  previewImageUrl.value = getImageUrl(url)
  showPreviewDialog.value = true
}

// 通过举报
const handleApprove = (row: any) => {
  currentHandleId.value = row.id
  handleType.value = 'approve'
  handleForm.adminRemark = ''
  showHandleDialog.value = true
}

// 拒绝举报
const handleReject = (row: any) => {
  currentHandleId.value = row.id
  handleType.value = 'reject'
  handleForm.adminRemark = ''
  showHandleDialog.value = true
}

// 确认处理
const confirmHandle = async () => {
  handleLoading.value = true
  try {
    const url = handleType.value === 'approve'
      ? `/report/${currentHandleId.value}/approve`
      : `/report/${currentHandleId.value}/reject`
    console.log('处理举报URL:', url)
    console.log('处理类型:', handleType.value)
    const response = await request.put(url, { adminRemark: handleForm.adminRemark })
    console.log('处理响应:', response)
    ElMessage.success(handleType.value === 'approve' ? '已通过举报，商品已下架' : '已拒绝举报')
    showHandleDialog.value = false
    fetchReportList()
    fetchStats()
  } catch (error: any) {
    console.error('处理失败:', error)
    ElMessage.error(error.message || '处理失败')
  } finally {
    handleLoading.value = false
  }
}

// 搜索
const handleSearch = () => {
  page.value = 1
  fetchReportList()
}

// 重置筛选
const resetFilter = () => {
  filterForm.status = null
  page.value = 1
  fetchReportList()
}

// 分页
const handlePageChange = (val: number) => {
  page.value = val
  fetchReportList()
}

const handleSizeChange = (val: number) => {
  pageSize.value = val
  page.value = 1
  fetchReportList()
}

onMounted(() => {
  fetchReportList()
  fetchStats()
})
</script>

<style scoped>
.product-review-view {
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

.stat-row {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin: 0 0 8px 0;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.filter-card {
  margin-bottom: 20px;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-item {
  margin-bottom: 0;
}

.filter-select {
  width: 160px;
}

.product-info {
  display: flex;
  gap: 12px;
}

.product-image {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 4px;
}

.product-detail {
  flex: 1;
}

.product-name {
  margin: 0 0 6px 0;
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.product-price {
  margin: 0 0 6px 0;
  font-size: 16px;
  color: #f56c6c;
  font-weight: 600;
}

.unit {
  font-size: 12px;
  color: #909399;
  margin-left: 2px;
}

.farmer-name {
  margin: 0;
  font-size: 12px;
  color: #909399;
}

.report-info p {
  margin: 0 0 4px 0;
  font-size: 13px;
}

.report-content {
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.report-detail {
  max-height: 600px;
  overflow-y: auto;
}

.detail-section {
  margin-bottom: 20px;
}

.detail-section h4 {
  margin: 0 0 12px 0;
  font-size: 16px;
  color: #303133;
  border-left: 4px solid #007aff;
  padding-left: 10px;
}

.product-detail-info {
  display: flex;
  gap: 15px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 8px;
}

.detail-product-image {
  width: 120px;
  height: 120px;
  object-fit: cover;
  border-radius: 4px;
}

.detail-product-text {
  flex: 1;
}

.detail-product-text p {
  margin: 0 0 8px 0;
}

.detail-content-box {
  margin-top: 12px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
}

.detail-content {
  margin: 8px 0 0 0;
  line-height: 1.6;
  color: #606266;
}

.detail-images {
  margin-top: 12px;
}

.image-list {
  display: flex;
  gap: 10px;
  margin-top: 8px;
  flex-wrap: wrap;
}

.report-image {
  width: 100px;
  height: 100px;
  object-fit: cover;
  border-radius: 4px;
  cursor: pointer;
  transition: transform 0.2s;
}

.report-image:hover {
  transform: scale(1.05);
}

/* 商品图片 */
.product-images-section {
  margin-top: 15px;
}

.product-gallery-image {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 4px;
  cursor: pointer;
  transition: transform 0.2s;
}

.product-gallery-image:hover {
  transform: scale(1.05);
}

/* 商品介绍 */
.product-introduction-section {
  margin-top: 15px;
}

.introduction-list {
  margin-top: 8px;
}

.introduction-item {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 12px;
}

.intro-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.intro-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 8px;
  text-indent: 2em;
}

.intro-image {
  max-width: 200px;
  max-height: 200px;
  object-fit: cover;
  border-radius: 4px;
  cursor: pointer;
  transition: transform 0.2s;
}

.intro-image:hover {
  transform: scale(1.05);
}

/* 商家信息 */
.shop-info {
  padding: 15px;
  background: #f5f7fa;
  border-radius: 8px;
}

.shop-header {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 15px;
  padding-bottom: 15px;
  border-bottom: 1px solid #e4e7ed;
}

.shop-avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  object-fit: cover;
}

.shop-basic {
  flex: 1;
}

.shop-name {
  margin: 0 0 5px 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.shop-details p {
  margin: 0 0 8px 0;
}

.shop-tags {
  margin-top: 10px;
}

.shop-tag {
  margin-right: 8px;
  margin-bottom: 5px;
}

.shop-description {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed #dcdfe6;
  color: #606266;
  line-height: 1.6;
}

.admin-remark {
  margin-top: 15px;
  padding: 10px;
  background: #fdf6ec;
  border-radius: 4px;
  color: #e6a23c;
}
</style>
