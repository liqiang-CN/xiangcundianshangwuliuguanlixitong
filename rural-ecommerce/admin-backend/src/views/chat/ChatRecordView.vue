<template>
  <div class="chat-record-view">
    <!-- 统计卡片 -->
    <div class="statistics-cards">
      <el-card class="stat-card">
        <div class="stat-value">{{ statistics.totalMessages || 0 }}</div>
        <div class="stat-label">总消息数</div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-value">{{ statistics.textMessages || 0 }}</div>
        <div class="stat-label">文本消息</div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-value">{{ statistics.imageMessages || 0 }}</div>
        <div class="stat-label">图片消息</div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-value">{{ statistics.orderMessages || 0 }}</div>
        <div class="stat-label">订单卡片</div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-value">{{ statistics.productMessages || 0 }}</div>
        <div class="stat-label">商品卡片</div>
      </el-card>
    </div>

    <!-- 搜索栏 -->
    <el-card class="filter-card">
      <el-form :model="searchForm" inline class="filter-form">
        <el-form-item label="关键词" class="filter-item">
          <el-input v-model="searchForm.keyword" placeholder="搜索用户/商家/消息内容" clearable class="filter-input" />
        </el-form-item>
        <el-form-item label="时间范围" class="filter-item">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            class="filter-date-range"
          />
        </el-form-item>
        <el-form-item class="filter-item filter-buttons">
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>搜索
          </el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 会话列表 -->
    <el-card class="session-list-card">
      <template #header>
        <div class="card-header">
          <span>聊天会话列表</span>
          <el-button type="primary" link @click="refreshData">
            <el-icon><Refresh /></el-icon>刷新
          </el-button>
        </div>
      </template>

      <el-table :data="sessionList" v-loading="loading" stripe>
        <el-table-column type="index" width="50" />
        <el-table-column label="用户" min-width="180">
          <template #default="{ row }">
            <div class="user-info">
              <el-avatar :size="40" :src="getImageUrl(row.userAvatar)" />
              <div class="user-detail">
                <div class="user-name">{{ row.userName }}</div>
                <div class="user-id">ID: {{ row.userId }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="商家" min-width="180">
          <template #default="{ row }">
            <div class="user-info">
              <el-avatar :size="40" :src="getImageUrl(row.targetAvatar)" />
              <div class="user-detail">
                <div class="user-name">{{ row.targetName }}</div>
                <div class="shop-name" v-if="row.targetShopName">{{ row.targetShopName }}</div>
                <div class="user-id">ID: {{ row.targetId }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="最新消息" min-width="200">
          <template #default="{ row }">
            <div class="last-message">
              <el-tag size="small" :type="getMessageTypeTag(row.lastMessageType)">
                {{ getMessageTypeText(row.lastMessageType) }}
              </el-tag>
              <span class="message-content" :title="row.lastMessage">{{ row.lastMessage }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="消息数" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="info">{{ row.messageCount }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最后时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.lastMessageTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewChatHistory(row)">
              查看记录
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 聊天记录详情弹窗 -->
    <el-dialog
      v-model="historyDialogVisible"
      :title="`聊天记录 - ${currentSession?.userName} 与 ${currentSession?.targetName}`"
      width="800px"
      :close-on-click-modal="false"
    >
      <div class="chat-history-container" v-loading="historyLoading">
        <div v-if="chatHistory.length === 0" class="empty-history">
          <el-empty description="暂无聊天记录" />
        </div>
        <div v-else class="chat-history-list">
          <div
            v-for="msg in chatHistory"
            :key="msg.id"
            class="chat-message-item"
            :class="{ 'user': msg.senderId === currentSession?.userId, 'merchant': msg.senderId === currentSession?.targetId }"
          >
            <div class="message-avatar">
              <el-avatar :size="40" :src="getImageUrl(msg.senderAvatar)" />
            </div>
            <div class="message-content-wrapper">
              <div class="message-header">
                <template v-if="msg.senderId === currentSession?.userId">
                  <span class="message-time">{{ formatDateTime(msg.createTime) }}</span>
                  <el-tag size="small" type="primary" class="sender-tag">用户</el-tag>
                  <span class="sender-name">{{ msg.senderName }}</span>
                </template>
                <template v-else>
                  <span class="sender-name">{{ msg.senderName }}</span>
                  <el-tag size="small" type="success" class="sender-tag">商家</el-tag>
                  <span class="message-time">{{ formatDateTime(msg.createTime) }}</span>
                </template>
              </div>
              <div class="message-body">
                <!-- 文本消息 -->
                <div v-if="msg.type === 1" class="text-content">{{ msg.content }}</div>
                
                <!-- 图片消息 -->
                <div v-else-if="msg.type === 2" class="image-content">
                  <img :src="getImageUrl(msg.content)" @click="previewImage(msg.content)" />
                </div>
                
                <!-- 订单卡片 -->
                <div v-else-if="msg.type === 3" class="card-message order-card">
                  <div class="card-header">
                    <img :src="getImageUrl(msg.extra?.productImage)" class="card-image" />
                    <div class="card-info">
                      <div class="card-title">{{ msg.extra?.productName }}</div>
                      <div class="card-desc" v-if="msg.extra?.productDesc">{{ msg.extra?.productDesc }}</div>
                    </div>
                  </div>
                  <div class="card-footer">
                    <span class="card-price">
                      ¥{{ msg.extra?.price }}<span v-if="msg.extra?.unit">/{{ msg.extra?.unit }}</span>
                    </span>
                    <span class="card-status">{{ msg.extra?.statusText }}</span>
                  </div>
                </div>
                
                <!-- 商品卡片 -->
                <div v-else-if="msg.type === 4" class="card-message product-card">
                  <div class="card-header">
                    <img :src="getImageUrl(msg.extra?.productImage)" class="card-image" />
                    <div class="card-info">
                      <div class="card-title">{{ msg.extra?.productName }}</div>
                      <div class="card-desc" v-if="msg.extra?.productDesc">{{ msg.extra?.productDesc }}</div>
                    </div>
                  </div>
                  <div class="card-footer">
                    <span class="card-price">
                      ¥{{ msg.extra?.price }}<span v-if="msg.extra?.unit">/{{ msg.extra?.unit }}</span>
                    </span>
                    <span class="card-status">已售{{ msg.extra?.sales }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 图片预览 -->
    <el-image-viewer
      v-if="previewVisible"
      :url-list="[previewImageUrl]"
      @close="previewVisible = false"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getChatSessions, getChatHistory, getChatStatistics } from '@/api/chat'
import { getImageUrl } from '@/utils/image'

// 搜索表单
const searchForm = ref({
  keyword: '',
  dateRange: null as [string, string] | null
})

// 统计数据
const statistics = ref<any>({})

// 会话列表
const sessionList = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)

// 聊天记录
const historyDialogVisible = ref(false)
const historyLoading = ref(false)
const chatHistory = ref<any[]>([])
const currentSession = ref<any>(null)

// 图片预览
const previewVisible = ref(false)
const previewImageUrl = ref('')

// 获取统计数据
const loadStatistics = async () => {
  try {
    const res: any = await getChatStatistics()
    statistics.value = res || {}
  } catch (error) {
    console.error('获取统计失败:', error)
  }
}

// 获取会话列表
const loadSessions = async () => {
  loading.value = true
  try {
    const params: any = {
      page: page.value,
      size: pageSize.value
    }
    if (searchForm.value.keyword) {
      params.keyword = searchForm.value.keyword
    }
    if (searchForm.value.dateRange) {
      params.startTime = searchForm.value.dateRange[0] + ' 00:00:00'
      params.endTime = searchForm.value.dateRange[1] + ' 23:59:59'
    }
    
    const res: any = await getChatSessions(params)
    sessionList.value = res.list || []
    total.value = res.total || 0
  } catch (error) {
    ElMessage.error('获取会话列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  page.value = 1
  loadSessions()
}

// 重置搜索
const resetSearch = () => {
  searchForm.value = {
    keyword: '',
    dateRange: null
  }
  page.value = 1
  loadSessions()
}

// 刷新数据
const refreshData = () => {
  loadStatistics()
  loadSessions()
}

// 查看聊天记录
const viewChatHistory = async (session: any) => {
  currentSession.value = session
  historyDialogVisible.value = true
  historyLoading.value = true
  
  try {
    const res: any = await getChatHistory({
      userId: session.userId,
      targetId: session.targetId,
      page: 1,
      size: 100
    })
    chatHistory.value = res || []
  } catch (error) {
    ElMessage.error('获取聊天记录失败')
  } finally {
    historyLoading.value = false
  }
}

// 分页
const handleSizeChange = (val: number) => {
  pageSize.value = val
  loadSessions()
}

const handlePageChange = (val: number) => {
  page.value = val
  loadSessions()
}

// 获取消息类型标签
const getMessageTypeTag = (type: number) => {
  const map: Record<number, string> = {
    1: '',
    2: 'success',
    3: 'warning',
    4: 'danger'
  }
  return map[type] || ''
}

// 获取消息类型文本
const getMessageTypeText = (type: number) => {
  const map: Record<number, string> = {
    1: '文本',
    2: '图片',
    3: '订单',
    4: '商品'
  }
  return map[type] || '未知'
}

// 预览图片
const previewImage = (url: string) => {
  previewImageUrl.value = getImageUrl(url)
  previewVisible.value = true
}

// 格式化日期时间
const formatDateTime = (dateStr: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  loadStatistics()
  loadSessions()
})
</script>

<style scoped>
.chat-record-view {
  padding: 20px;
}

.statistics-cards {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #007aff;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

.filter-card {
  margin-bottom: 20px;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0;
}

.filter-form .filter-item {
  margin-bottom: 0;
  margin-right: 20px;
}

.filter-form .filter-item:last-child {
  margin-right: 0;
}

.filter-form .filter-buttons {
  margin-left: auto;
}

.filter-input {
  width: 220px;
}

.filter-date-range {
  width: 320px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-detail {
  display: flex;
  flex-direction: column;
}

.user-name {
  font-weight: 500;
  color: #333;
}

.shop-name {
  font-size: 12px;
  color: #007aff;
}

.user-id {
  font-size: 12px;
  color: #999;
}

.last-message {
  display: flex;
  align-items: center;
  gap: 8px;
}

.message-content {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #666;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 聊天记录弹窗 */
.chat-history-container {
  max-height: 500px;
  overflow-y: auto;
}

.empty-history {
  padding: 40px 0;
}

.chat-history-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.chat-message-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
}

.chat-message-item.user {
  flex-direction: row-reverse;
  background: #e6f2ff;
}

.chat-message-item.merchant {
  background: #f8f9fa;
}

.message-avatar {
  flex-shrink: 0;
}

.message-content-wrapper {
  flex: 1;
  min-width: 0;
}

.chat-message-item.user .message-content-wrapper {
  text-align: right;
}

.message-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.chat-message-item.user .message-header {
  justify-content: flex-end;
}

.sender-name {
  font-weight: 500;
  font-size: 14px;
  color: #333;
}

.sender-tag {
  flex-shrink: 0;
}

.message-time {
  font-size: 12px;
  color: #999;
}

.message-body {
  display: inline-block;
  text-align: left;
}

.text-content {
  font-size: 14px;
  color: #333;
  line-height: 1.5;
  word-break: break-word;
}

.image-content img {
  max-width: 200px;
  max-height: 200px;
  border-radius: 4px;
  cursor: pointer;
}

.card-message {
  width: 260px;
  background: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  overflow: hidden;
}

.order-card .card-header,
.product-card .card-header {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: #f8f9fa;
  align-items: flex-start;
}

.card-image {
  width: 60px !important;
  height: 60px !important;
  max-width: 60px !important;
  max-height: 60px !important;
  object-fit: cover;
  border-radius: 4px;
}

.order-card .card-img,
.product-card .card-image {
  width: 60px !important;
  height: 60px !important;
  max-width: 60px !important;
  max-height: 60px !important;
  object-fit: cover;
  border-radius: 4px;
}

.card-info {
  flex: 1;
  min-width: 0;
}

.card-title {
  font-weight: 500;
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-desc {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border-top: 1px solid #e5e5e5;
}

.order-card .card-footer,
.product-card .card-footer {
  margin-top: 0;
  padding: 8px 12px;
}

.card-price {
  font-weight: 600;
  color: #ff6b6b;
  font-size: 14px;
}

.card-status {
  font-size: 12px;
  color: #007aff;
}

@media (max-width: 1200px) {
  .statistics-cards {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .statistics-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
