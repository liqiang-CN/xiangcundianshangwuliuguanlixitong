<template>
  <div class="shop-manage-view">
    <div class="page-header">
      <h2 class="page-title">店铺管理</h2>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <p class="stat-label">总店铺</p>
          <p class="stat-value">{{ stats.total }}</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <p class="stat-label">营业中</p>
          <p class="stat-value">{{ stats.active }}</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <p class="stat-label">已关闭</p>
          <p class="stat-value">{{ stats.closed }}</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <p class="stat-label">本月新增</p>
          <p class="stat-value">{{ stats.newThisMonth }}</p>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选栏 -->
    <el-card class="filter-card">
      <el-form :model="filterForm" inline class="filter-form">
        <el-form-item label="店铺名称" class="filter-item">
          <el-input v-model="filterForm.name" placeholder="请输入店铺名称" clearable class="filter-input" />
        </el-form-item>
        <el-form-item label="主营品类" class="filter-item">
          <el-select v-model="filterForm.category" placeholder="请选择品类" clearable class="filter-select">
            <el-option label="新鲜水果" value="新鲜水果" />
            <el-option label="蔬菜菌菇" value="蔬菜菌菇" />
            <el-option label="粮油米面" value="粮油米面" />
            <el-option label="肉禽蛋品" value="肉禽蛋品" />
            <el-option label="水产海鲜" value="水产海鲜" />
            <el-option label="农副加工" value="农副加工" />
          </el-select>
        </el-form-item>
        <el-form-item label="店铺状态" class="filter-item">
          <el-select v-model="filterForm.status" placeholder="请选择状态" clearable class="filter-select-sm">
            <el-option label="营业中" :value="1" />
            <el-option label="已关闭" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item class="filter-item filter-buttons">
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 店铺列表 -->
    <el-card>
      <el-table :data="shopList" v-loading="loading" style="width: 100%">
        <el-table-column label="店铺信息" min-width="18%">
          <template #default="{ row }">
            <div class="shop-info">
              <el-avatar :size="60" :src="getImageUrl(row.avatar)" />
              <div class="shop-detail">
                <p class="shop-name">{{ row.shopName || row.nickname }}</p>
                <p class="shop-location">
                  <el-icon><Location /></el-icon>
                  {{ row.location || '未设置' }}
                </p>
                <p class="shop-category">主营: {{ row.mainCategory || '未设置' }}</p>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="店铺评分" min-width="12%" align="center">
          <template #default="{ row }">
            <div class="shop-rating">
              <el-rate v-model="row.rating" disabled show-score text-color="#ff9900" />
              <span class="followers">{{ row.followers || 0 }} 关注</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="店铺标签" min-width="20%" align="center">
          <template #default="{ row }">
            <div class="shop-tags">
              <el-tag v-for="tag in parseTags(row.tags)" :key="tag" size="small" class="tag-item">
                {{ tag }}
              </el-tag>
              <span v-if="!row.tags" class="no-tags">暂无标签</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="10%" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '营业中' : '已关闭' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="开通时间" min-width="15%" align="center" />
        <el-table-column label="操作" min-width="25%" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button link type="primary" @click="viewDetail(row)">详情</el-button>
              <el-button link type="primary" @click="editShop(row)">编辑</el-button>
              <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="toggleStatus(row)">
                {{ row.status === 1 ? '关闭' : '开启' }}
              </el-button>
              <el-button link type="danger" @click="deleteShop(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination 
          v-model:current-page="page" 
          v-model:page-size="pageSize" 
          :total="total" 
          layout="total, prev, pager, next" 
          @current-change="handlePageChange" 
        />
      </div>
    </el-card>

    <!-- 店铺详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="店铺详情" width="700px">
      <div v-if="currentShop" class="shop-detail-dialog">
        <div class="detail-header">
          <el-avatar :size="80" :src="getImageUrl(currentShop.avatar)" />
          <div class="detail-info">
            <h3>{{ currentShop.shopName || currentShop.nickname }}</h3>
            <p class="detail-location">
              <el-icon><Location /></el-icon>
              {{ currentShop.location || '未设置地区' }}
            </p>
            <p class="detail-category">主营品类: {{ currentShop.mainCategory || '未设置' }}</p>
          </div>
          <el-tag :type="currentShop.status === 1 ? 'success' : 'info'" size="large">
            {{ currentShop.status === 1 ? '营业中' : '已关闭' }}
          </el-tag>
        </div>

        <el-divider />

        <div class="detail-section">
          <h4>店铺背景</h4>
          <div v-if="currentShop.background" class="background-preview">
            <img :src="getImageUrl(currentShop.background)" alt="店铺背景" />
          </div>
          <p v-else class="no-data">未设置背景图</p>
        </div>

        <div class="detail-section">
          <h4>店铺描述</h4>
          <p class="detail-description">{{ currentShop.description || '暂无描述' }}</p>
        </div>

        <div class="detail-section">
          <h4>店铺标签</h4>
          <div class="detail-tags">
            <el-tag v-for="tag in parseTags(currentShop.tags)" :key="tag" class="tag-item">
              {{ tag }}
            </el-tag>
            <span v-if="!currentShop.tags" class="no-data">暂无标签</span>
          </div>
        </div>

        <div class="detail-stats">
          <div class="stat-item">
            <span class="stat-label">店铺评分</span>
            <span class="stat-value">{{ currentShop.rating || 5.0 }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">关注人数</span>
            <span class="stat-value">{{ currentShop.followers || 0 }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">开通时间</span>
            <span class="stat-value">{{ currentShop.createTime }}</span>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 编辑店铺弹窗 -->
    <el-dialog v-model="editDialogVisible" title="编辑店铺" width="600px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="店铺名称">
          <el-input v-model="editForm.shopName" />
        </el-form-item>
        <el-form-item label="店主名称">
          <el-input v-model="editForm.nickname" />
        </el-form-item>
        <el-form-item label="所在地区">
          <el-input v-model="editForm.location" />
        </el-form-item>
        <el-form-item label="主营品类">
          <el-select v-model="editForm.mainCategory" style="width: 100%">
            <el-option label="新鲜水果" value="新鲜水果" />
            <el-option label="蔬菜菌菇" value="蔬菜菌菇" />
            <el-option label="粮油米面" value="粮油米面" />
            <el-option label="肉禽蛋品" value="肉禽蛋品" />
            <el-option label="水产海鲜" value="水产海鲜" />
            <el-option label="农副加工" value="农副加工" />
          </el-select>
        </el-form-item>
        <el-form-item label="店铺标签">
          <el-input v-model="editForm.tags" placeholder="多个标签用逗号分隔" />
        </el-form-item>
        <el-form-item label="店铺描述">
          <el-input v-model="editForm.description" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="店铺评分">
          <el-rate v-model="editForm.rating" show-score />
        </el-form-item>
        <el-form-item label="店铺状态">
          <el-switch v-model="editForm.status" :active-value="1" :inactive-value="0" active-text="营业中" inactive-text="已关闭" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveShop">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Location } from '@element-plus/icons-vue'
import { getShopList, updateShop, toggleShopStatus, deleteShop as deleteShopById } from '../../api/shop'
import { getImageUrl } from '@/utils/image'

const loading = ref(false)
const filterForm = reactive({
  name: '',
  category: '',
  status: null as number | null
})
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const stats = ref({
  total: 0,
  active: 0,
  closed: 0,
  newThisMonth: 0
})

const shopList = ref<any[]>([])

// 详情弹窗
const detailDialogVisible = ref(false)
const currentShop = ref<any>(null)

// 编辑弹窗
const editDialogVisible = ref(false)
const editForm = reactive({
  id: 0,
  shopName: '',
  nickname: '',
  location: '',
  mainCategory: '',
  tags: '',
  description: '',
  rating: 5,
  status: 1
})

// 解析标签
const parseTags = (tags: string) => {
  if (!tags) return []
  return tags.split(',').filter(tag => tag.trim())
}

// 获取店铺列表
const fetchShopList = async () => {
  loading.value = true
  try {
    const res: any = await getShopList({
      page: page.value,
      size: pageSize.value,
      name: filterForm.name,
      category: filterForm.category,
      status: filterForm.status
    })
    shopList.value = res.list || []
    total.value = res.total || 0
    
    // 更新统计
    stats.value = {
      total: res.total || 0,
      active: res.active || 0,
      closed: res.closed || 0,
      newThisMonth: res.newThisMonth || 0
    }
  } catch (error) {
    console.error('获取店铺列表失败:', error)
    ElMessage.error('获取店铺列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  fetchShopList()
}

const resetFilter = () => {
  filterForm.name = ''
  filterForm.category = ''
  filterForm.status = null
  page.value = 1
  fetchShopList()
}

const handlePageChange = (val: number) => {
  page.value = val
  fetchShopList()
}

// 查看详情
const viewDetail = (row: any) => {
  currentShop.value = row
  detailDialogVisible.value = true
}

// 编辑店铺
const editShop = (row: any) => {
  editForm.id = row.id
  editForm.shopName = row.shopName || row.nickname
  editForm.nickname = row.nickname
  editForm.location = row.location
  editForm.mainCategory = row.mainCategory
  editForm.tags = row.tags
  editForm.description = row.description
  editForm.rating = row.rating || 5
  editForm.status = row.status
  editDialogVisible.value = true
}

// 保存店铺
const saveShop = async () => {
  try {
    await updateShop(editForm.id, editForm)
    ElMessage.success('保存成功')
    editDialogVisible.value = false
    fetchShopList()
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  }
}

// 切换店铺状态
const toggleStatus = async (row: any) => {
  const newStatus = row.status === 1 ? 0 : 1
  const actionText = newStatus === 1 ? '开启' : '关闭'
  
  try {
    await ElMessageBox.confirm(`确定要${actionText}该店铺吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await toggleShopStatus(row.id, newStatus)
    ElMessage.success(`${actionText}成功`)
    row.status = newStatus
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('操作失败:', error)
      ElMessage.error('操作失败')
    }
  }
}

// 删除店铺
const deleteShop = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定要删除店铺 "${row.shopName || row.nickname}" 吗？此操作不可恢复！`, '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await deleteShopById(row.id)
    ElMessage.success('删除成功')
    fetchShopList()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  fetchShopList()
})
</script>

<style scoped>
.shop-manage-view {
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
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
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
  margin-right: 0;
}

.filter-input {
  width: 160px;
}

.filter-select {
  width: 140px;
}

.filter-select-sm {
  width: 120px;
}

.filter-buttons {
  margin-left: auto;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.shop-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.shop-detail {
  flex: 1;
  max-width: 180px;
}

.shop-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.shop-location {
  font-size: 13px;
  color: #909399;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.shop-category {
  font-size: 12px;
  color: #409eff;
}

.shop-rating {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.followers {
  font-size: 12px;
  color: #909399;
}

.shop-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  justify-content: center;
}

.tag-item {
  margin-right: 0;
}

/* 操作按钮样式 */
.action-buttons {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.no-tags {
  font-size: 12px;
  color: #909399;
}

/* 详情弹窗样式 */
.shop-detail-dialog {
  padding: 10px;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.detail-info {
  flex: 1;
}

.detail-info h3 {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.detail-location {
  font-size: 14px;
  color: #909399;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.detail-category {
  font-size: 14px;
  color: #409eff;
}

.detail-section {
  margin-bottom: 20px;
}

.detail-section h4 {
  font-size: 14px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 12px;
}

.background-preview {
  width: 100%;
  height: 150px;
  border-radius: 8px;
  overflow: hidden;
}

.background-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-description {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}

.detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.no-data {
  font-size: 14px;
  color: #909399;
}

.detail-stats {
  display: flex;
  gap: 40px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-item .stat-label {
  font-size: 12px;
  color: #909399;
}

.stat-item .stat-value {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}
</style>
