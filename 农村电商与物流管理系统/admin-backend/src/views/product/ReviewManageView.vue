<template>
  <div class="review-manage-view">
    <div class="page-header">
      <h2 class="page-title">评价管理</h2>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <p class="stat-label">总评价数</p>
          <p class="stat-value">{{ stats.total }}</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <p class="stat-label">今日新增</p>
          <p class="stat-value">{{ stats.today }}</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <p class="stat-label">已发布</p>
          <p class="stat-value">{{ stats.published }}</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <p class="stat-label">待审核</p>
          <p class="stat-value">{{ stats.pending }}</p>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选栏 -->
    <el-card class="filter-card">
      <el-form :model="filterForm" inline class="filter-form">
        <el-form-item label="评价状态" class="filter-item">
          <el-select v-model="filterForm.status" placeholder="请选择状态" clearable class="filter-select">
            <el-option label="待审核" :value="0" />
            <el-option label="已发布" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词" class="filter-item">
          <el-input v-model="filterForm.keyword" placeholder="搜索评价内容" clearable class="filter-input" />
        </el-form-item>
        <el-form-item class="filter-item filter-buttons">
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 评价列表 -->
    <el-card>
      <div class="table-header">
        <el-button type="danger" :disabled="selectedIds.length === 0" @click="handleBatchDelete">
          批量删除
        </el-button>
      </div>
      <el-table :data="reviewList" v-loading="loading" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="用户信息" width="150" align="center">
          <template #default="{ row }">
            <div class="user-info">
              <el-avatar :size="40" :src="row.userAvatar || '/images/default-avatar.jpg'" />
              <span class="user-name">{{ row.userName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="商品ID" width="80" align="center" prop="productId" />
        <el-table-column label="评分" width="160" align="center">
          <template #default="{ row }">
            <el-rate :model-value="row.rating" disabled show-score text-color="#ff9900" />
          </template>
        </el-table-column>
        <el-table-column label="评价内容" min-width="200">
          <template #default="{ row }">
            <div class="review-content">
              <p class="content-text">{{ row.content }}</p>
              <div v-if="row.images && row.images.length > 0" class="review-images">
                <el-image
                  v-for="(img, index) in row.images.slice(0, 3)"
                  :key="index"
                  :src="getImageUrl(img)"
                  :preview-src-list="row.images.map(getImageUrl)"
                  class="review-image"
                  fit="cover"
                  preview-teleported
                />
                <span v-if="row.images.length > 3" class="more-images">+{{ row.images.length - 3 }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="time" label="评价时间" width="160" align="center" />
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewDetail(row)">查看</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
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
    <el-dialog v-model="showDetailDialog" title="评价详情" width="700px">
      <div v-if="currentReview" class="review-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="评价ID">{{ currentReview.id }}</el-descriptions-item>
          <el-descriptions-item label="商品ID">{{ currentReview.productId }}</el-descriptions-item>
          <el-descriptions-item label="用户">{{ currentReview.userName }}</el-descriptions-item>
          <el-descriptions-item label="评分">
            <el-rate :model-value="currentReview.rating" disabled show-score text-color="#ff9900" />
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentReview.status)">{{ getStatusText(currentReview.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="评价时间">{{ currentReview.time }}</el-descriptions-item>
        </el-descriptions>
        
        <div class="detail-section">
          <h4>评价内容</h4>
          <p class="detail-content">{{ currentReview.content }}</p>
        </div>
        
        <div v-if="currentReview.images && currentReview.images.length > 0" class="detail-section">
          <h4>评价图片</h4>
          <div class="detail-images">
            <el-image
              v-for="(img, index) in currentReview.images"
              :key="index"
              :src="getImageUrl(img)"
              :preview-src-list="currentReview.images.map(getImageUrl)"
              class="detail-image"
              fit="cover"
              preview-teleported
            />
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getReviewList, getReviewStats, deleteReview, batchDeleteReviews } from '@/api/review'
import { getImageUrl } from '@/utils/image'

// 统计数据
const stats = reactive({
  total: 0,
  today: 0,
  published: 0,
  pending: 0
})

// 列表数据
const loading = ref(false)
const reviewList = ref<any[]>([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const selectedIds = ref<number[]>([])

// 筛选表单
const filterForm = reactive({
  status: undefined as number | undefined,
  keyword: ''
})

// 详情弹窗
const showDetailDialog = ref(false)
const currentReview = ref<any>(null)

// 获取统计数据
const loadStats = async () => {
  try {
    const res = await getReviewStats()
    if (res) {
      stats.total = res.total || 0
      stats.today = res.today || 0
      stats.published = res.published || 0
      stats.pending = res.pending || 0
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// 获取评价列表
const loadReviewList = async () => {
  loading.value = true
  try {
    const res = await getReviewList({
      page: page.value,
      size: pageSize.value,
      status: filterForm.status,
      keyword: filterForm.keyword || undefined
    })
    if (res) {
      reviewList.value = res.records || []
      total.value = res.total || 0
    }
  } catch (error) {
    console.error('加载评价列表失败:', error)
    ElMessage.error('加载评价列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  page.value = 1
  loadReviewList()
}

// 重置筛选
const resetFilter = () => {
  filterForm.status = undefined
  filterForm.keyword = ''
  page.value = 1
  loadReviewList()
}

// 分页
const handlePageChange = (val: number) => {
  page.value = val
  loadReviewList()
}

const handleSizeChange = (val: number) => {
  pageSize.value = val
  page.value = 1
  loadReviewList()
}

// 选择变化
const handleSelectionChange = (selection: any[]) => {
  selectedIds.value = selection.map(item => item.id)
}

// 查看详情
const viewDetail = (row: any) => {
  currentReview.value = row
  showDetailDialog.value = true
}

// 删除评价
const handleDelete = (row: any) => {
  ElMessageBox.confirm('确定要删除该评价吗？删除后不可恢复', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteReview(row.id)
      ElMessage.success('删除成功')
      loadReviewList()
      loadStats()
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

// 批量删除
const handleBatchDelete = () => {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请选择要删除的评价')
    return
  }
  ElMessageBox.confirm(`确定要删除选中的 ${selectedIds.value.length} 条评价吗？删除后不可恢复`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await batchDeleteReviews(selectedIds.value)
      ElMessage.success('批量删除成功')
      loadReviewList()
      loadStats()
    } catch (error) {
      console.error('批量删除失败:', error)
      ElMessage.error('批量删除失败')
    }
  }).catch(() => {})
}

// 状态文本
const getStatusText = (status: number) => {
  const map: Record<number, string> = {
    0: '待审核',
    1: '已发布',
    2: '已删除'
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
  loadReviewList()
})
</script>

<style scoped lang="scss">
.review-manage-view {
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

.filter-input {
  width: 200px;
}

.table-header {
  margin-bottom: 15px;
}

.user-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;

  .user-name {
    font-size: 12px;
    color: #606266;
  }
}

.review-content {
  .content-text {
    margin: 0 0 8px;
    color: #303133;
    line-height: 1.5;
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

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.review-detail {
  .detail-section {
    margin-top: 20px;

    h4 {
      margin: 0 0 10px;
      font-size: 14px;
      color: #303133;
    }
  }

  .detail-content {
    padding: 15px;
    background: #f5f7fa;
    border-radius: 4px;
    line-height: 1.6;
    color: #303133;
    margin: 0;
  }

  .detail-images {
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
</style>
