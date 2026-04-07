<template>
  <div class="review-report-view">
    <div class="page-header">
      <h2 class="page-title">评价举报审核</h2>
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
        <el-table-column label="被举报评价" min-width="300">
          <template #default="{ row }">
            <div class="review-info">
              <div class="review-header">
                <el-avatar :size="32" :src="row.reviewUserAvatar || '/images/default-avatar.jpg'" />
                <span class="review-user">{{ row.reviewUserName || '匿名用户' }}</span>
                <el-rate :model-value="row.reviewRating" disabled size="small" />
              </div>
              <p class="review-content">{{ row.reviewContent }}</p>
              <div v-if="row.reviewImages && row.reviewImages.length > 0" class="review-images">
                <el-image
                  v-for="(img, index) in row.reviewImages.slice(0, 3)"
                  :key="index"
                  :src="getImageUrl(img)"
                  :preview-src-list="row.reviewImages.map(getImageUrl)"
                  class="review-image"
                  fit="cover"
                  preview-teleported
                />
                <span v-if="row.reviewImages.length > 3" class="more-images">+{{ row.reviewImages.length - 3 }}</span>
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
        <el-table-column prop="createTimeStr" label="举报时间" width="160" align="center" />
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
    <el-dialog v-model="showDetailDialog" title="举报详情" width="800px" top="5vh">
      <div v-if="currentReport" class="report-detail">
        <!-- 被举报评价 -->
        <div class="detail-section">
          <h4>被举报评价</h4>
          <div class="review-detail-info">
            <div class="review-user-info">
              <el-avatar :size="50" :src="currentReport.reviewUserAvatar || '/images/default-avatar.jpg'" />
              <div class="user-text">
                <p class="user-name">{{ currentReport.reviewUserName || '匿名用户' }}</p>
                <el-rate :model-value="currentReport.reviewRating" disabled show-score text-color="#ff9900" />
              </div>
            </div>
            <p class="detail-content">{{ currentReport.reviewContent }}</p>
            <div v-if="currentReport.reviewImages && currentReport.reviewImages.length > 0" class="detail-images">
              <p><strong>评价图片：</strong></p>
              <div class="image-list">
                <el-image
                  v-for="(img, index) in currentReport.reviewImages"
                  :key="index"
                  :src="getImageUrl(img)"
                  :preview-src-list="currentReport.reviewImages.map(getImageUrl)"
                  class="detail-image"
                  fit="cover"
                  preview-teleported
                />
              </div>
            </div>
          </div>
        </div>

        <!-- 举报信息 -->
        <div class="detail-section">
          <h4>举报信息</h4>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="举报类型">{{ getReportTypeText(currentReport.reportType) }}</el-descriptions-item>
            <el-descriptions-item label="举报人">{{ currentReport.reporterName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="举报时间">{{ currentReport.createTime }}</el-descriptions-item>
            <el-descriptions-item label="举报ID">{{ currentReport.id }}</el-descriptions-item>
          </el-descriptions>
          <div class="detail-content-box">
            <p><strong>举报内容：</strong></p>
            <p class="detail-content">{{ currentReport.reportContent || '无详细说明' }}</p>
          </div>
        </div>

        <!-- 处理结果 -->
        <div v-if="currentReport.status !== 0" class="detail-section">
          <h4>处理结果</h4>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="处理状态">
              <el-tag :type="getStatusType(currentReport.status)">{{ getStatusText(currentReport.status) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item v-if="currentReport.handleTime" label="处理时间">{{ currentReport.handleTime }}</el-descriptions-item>
          </el-descriptions>
          <p v-if="currentReport.adminRemark" class="admin-remark"><strong>管理员备注：</strong>{{ currentReport.adminRemark }}</p>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getReportList, handleReport, getReviewStats } from '@/api/review'
import { getImageUrl } from '@/utils/image'

// 统计数据
const stats = reactive({
  pending: 0,
  today: 0,
  approved: 0,
  rejected: 0
})

// 列表数据
const loading = ref(false)
const reportList = ref<any[]>([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 筛选表单 - 默认显示全部举报
const filterForm = reactive({
  status: undefined as number | undefined
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
const currentHandleId = ref<number | string>('')

// 获取统计数据
const loadStats = async () => {
  try {
    const res = await getReviewStats()
    if (res) {
      // 这里需要后端提供举报统计数据，暂时用评价数据代替
      stats.pending = res.pending || 0
      stats.today = res.today || 0
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// 获取举报列表
const loadReportList = async () => {
  loading.value = true
  try {
    const res = await getReportList({
      page: page.value,
      size: pageSize.value,
      status: filterForm.status
    })
    if (res) {
      reportList.value = res.records || []
      total.value = res.total || 0
      
      // 更新统计
      updateStatsFromList()
    }
  } catch (error) {
    console.error('加载举报列表失败:', error)
    ElMessage.error('加载举报列表失败')
  } finally {
    loading.value = false
  }
}

// 从列表数据更新统计
const updateStatsFromList = () => {
  const pending = reportList.value.filter(r => r.status === 0).length
  const approved = reportList.value.filter(r => r.status === 1).length
  const rejected = reportList.value.filter(r => r.status === 2).length
  
  stats.pending = pending
  stats.approved = approved
  stats.rejected = rejected
  
  // 今日举报（简化处理，实际应该从后端获取）
  const today = new Date().toISOString().split('T')[0]
  stats.today = reportList.value.filter(r => r.createTime && r.createTime.startsWith(today)).length
}

// 搜索
const handleSearch = () => {
  page.value = 1
  loadReportList()
}

// 重置筛选
const resetFilter = () => {
  filterForm.status = undefined
  page.value = 1
  loadReportList()
}

// 分页
const handlePageChange = (val: number) => {
  page.value = val
  loadReportList()
}

const handleSizeChange = (val: number) => {
  pageSize.value = val
  page.value = 1
  loadReportList()
}

// 查看详情
const viewDetail = (row: any) => {
  currentReport.value = row
  showDetailDialog.value = true
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
    const status = handleType.value === 'approve' ? 1 : 2
    await handleReport({
      reportId: currentHandleId.value,
      status: status,
      adminRemark: handleForm.adminRemark
    })
    
    ElMessage.success(handleType.value === 'approve' ? '已通过举报' : '已拒绝举报')
    showHandleDialog.value = false
    loadReportList()
    loadStats()
  } catch (error) {
    console.error('处理失败:', error)
    ElMessage.error('处理失败')
  } finally {
    handleLoading.value = false
  }
}

// 举报类型文本
const getReportTypeText = (type: number) => {
  const map: Record<number, string> = {
    1: '垃圾广告',
    2: '人身攻击',
    3: '虚假评价',
    4: '其他'
  }
  return map[type] || '其他'
}

// 状态文本
const getStatusText = (status: number) => {
  const map: Record<number, string> = {
    0: '待处理',
    1: '已通过',
    2: '已拒绝'
  }
  return map[status] || '未知'
}

// 状态类型
const getStatusType = (status: number) => {
  const map: Record<number, any> = {
    0: 'warning',
    1: 'success',
    2: 'danger'
  }
  return map[status] || 'info'
}

onMounted(() => {
  loadStats()
  loadReportList()
})
</script>

<style scoped lang="scss">
.review-report-view {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 500;
}

.stat-row {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;

  .stat-label {
    color: #909399;
    font-size: 14px;
    margin: 0 0 8px;
  }

  .stat-value {
    color: #303133;
    font-size: 24px;
    font-weight: 600;
    margin: 0;
  }
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
  width: 150px;
}

.review-info {
  .review-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;

    .review-user {
      font-size: 14px;
      color: #303133;
      font-weight: 500;
    }
  }

  .review-content {
    font-size: 14px;
    color: #606266;
    line-height: 1.5;
    margin-bottom: 8px;
  }
}

.review-images {
  display: flex;
  gap: 5px;
  align-items: center;

  .review-image {
    width: 50px;
    height: 50px;
    border-radius: 4px;
    cursor: pointer;
  }

  .more-images {
    font-size: 12px;
    color: #909399;
  }
}

.report-info {
  p {
    margin: 4px 0;
    font-size: 13px;
    color: #606266;
  }

  .report-content {
    color: #909399;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.report-detail {
  max-height: 70vh;
  overflow-y: auto;

  .detail-section {
    margin-bottom: 20px;

    h4 {
      margin: 0 0 15px;
      font-size: 16px;
      color: #303133;
      border-left: 4px solid #409eff;
      padding-left: 10px;
    }
  }

  .review-detail-info {
    background: #f5f7fa;
    padding: 15px;
    border-radius: 4px;

    .review-user-info {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 12px;

      .user-text {
        .user-name {
          margin: 0 0 4px;
          font-size: 15px;
          font-weight: 500;
          color: #303133;
        }
      }
    }
  }

  .detail-content {
    line-height: 1.6;
    color: #303133;
    margin: 0;
  }

  .detail-content-box {
    margin-top: 15px;
    padding: 15px;
    background: #f5f7fa;
    border-radius: 4px;

    p {
      margin: 0;
    }
  }

  .detail-images {
    margin-top: 12px;

    p {
      margin: 0 0 8px;
      font-size: 14px;
      color: #606266;
    }

    .image-list {
      display: flex;
      flex-wrap: wrap;
      gap: 10px;

      .detail-image {
        width: 100px;
        height: 100px;
        border-radius: 4px;
        cursor: pointer;
      }
    }
  }

  .admin-remark {
    margin-top: 12px;
    padding: 12px;
    background: #f5f7fa;
    border-radius: 4px;
    color: #606266;
  }
}
</style>
