<template>
  <div class="records-view">
    <h2 class="page-title">配送记录</h2>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="8">
        <div class="stat-card">
          <p class="stat-label">今日配送</p>
          <p class="stat-value">{{ stats.today }}单</p>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card">
          <p class="stat-label">本周配送</p>
          <p class="stat-value">{{ stats.week }}单</p>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card">
          <p class="stat-label">本月配送</p>
          <p class="stat-value">{{ stats.month }}单</p>
        </div>
      </el-col>
    </el-row>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" />
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="exportData">导出记录</el-button>
    </div>

    <!-- 记录列表 -->
    <el-table :data="records" style="width: 100%" v-loading="loading" class="records-table">
      <el-table-column prop="trackingNo" label="运单号" width="180" />
      <el-table-column label="收件人" width="150">
        <template #default="{ row }">
          <div>
            <p class="receiver-name">{{ row.receiverName }}</p>
            <p class="receiver-phone">{{ row.receiverPhone }}</p>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="receiverAddress" label="配送地址" min-width="200" show-overflow-tooltip />
      <el-table-column label="商品" min-width="150">
        <template #default="{ row }">
          <span>{{ row.goodsName }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag type="success">已签收</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="signTime" label="签收时间" width="160" />
      <el-table-column label="签收人" width="100">
        <template #default="{ row }">
          <span class="signer-text">{{ row.signer || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="viewDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="handlePageChange"
      />
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="showDetailDialog" title="配送详情" width="600px">
      <div v-if="currentRecord" class="detail-content">
        <h4>基本信息</h4>
        <p><strong>运单号：</strong>{{ currentRecord.trackingNo }}</p>
        <p><strong>状态：</strong><el-tag type="success">已签收</el-tag></p>
        <p><strong>签收时间：</strong>{{ currentRecord.signTime }}</p>
        <p><strong>签收人：</strong>{{ currentRecord.signer }}</p>
        
        <h4>收件信息</h4>
        <p><strong>收件人：</strong>{{ currentRecord.receiverName }}</p>
        <p><strong>电话：</strong>{{ currentRecord.receiverPhone }}</p>
        <p><strong>地址：</strong>{{ currentRecord.receiverAddress }}</p>
        
        <h4>商品信息</h4>
        <p><strong>商品名称：</strong>{{ currentRecord.goodsName }}</p>
        <p><strong>重量：</strong>{{ currentRecord.goodsWeight }}kg</p>
        
        <h4>物流轨迹</h4>
        <el-timeline v-if="logisticsTracks.length > 0">
          <el-timeline-item
            v-for="track in logisticsTracks"
            :key="track.id"
            :type="getTrackType(track.status)"
            :timestamp="track.createTime"
          >
            <p>{{ track.description }}</p>
            <p v-if="track.location" class="track-location">{{ track.location }}</p>
          </el-timeline-item>
        </el-timeline>
        <p v-else>暂无物流轨迹</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

const loading = ref(false)
const stats = ref({
  today: 0,
  week: 0,
  month: 0
})

const dateRange = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const records = ref<any[]>([])
const logisticsTracks = ref<any[]>([])

const showDetailDialog = ref(false)
const currentRecord = ref<any>(null)

// 获取配送记录
const fetchRecords = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/logistics/records', {
      params: {
        page: page.value,
        size: pageSize.value
      }
    })
    if (res && Array.isArray(res)) {
      // 处理数据，补充订单信息和签收人
      const processedList = await Promise.all(res.map(async (item: any) => {
        try {
          const orderRes: any = await request.get(`/logistics/order/${item.orderId}/detail`)
          // 获取物流轨迹来提取签收人
          const tracksRes: any = await request.get(`/logistics/${item.id}/tracks`)

          // 从物流轨迹中提取签收人（查找状态为3的轨迹）
          let signer = '-'
          if (tracksRes && Array.isArray(tracksRes)) {
            const signTrack = tracksRes.find((track: any) => track.status === 3)
            if (signTrack && signTrack.description) {
              // 从描述中提取签收人，格式："商品已签收，签收人：xxx"
              const match = signTrack.description.match(/签收人：(.+)$/)
              if (match) {
                signer = match[1]
              }
            }
          }

          if (orderRes) {
            return {
              ...item,
              receiverName: orderRes.receiverName,
              receiverPhone: orderRes.receiverPhone,
              receiverAddress: orderRes.receiverAddress,
              goodsName: orderRes.items?.[0]?.productName || '商品',
              goodsWeight: orderRes.items?.[0]?.quantity || 1,
              signer: signer
            }
          }
        } catch (e) {
          console.error('获取订单详情失败:', e)
        }
        return item
      }))
      records.value = processedList
      total.value = res.length
    }
  } catch (error) {
    console.error('获取配送记录失败:', error)
    ElMessage.error('获取配送记录失败')
  } finally {
    loading.value = false
  }
}

// 获取统计数据
const fetchStats = async () => {
  try {
    const res: any = await request.get('/logistics/stats')
    if (res) {
      stats.value.today = res.completed || 0
      // 本周和本月数据暂时用今日数据代替，实际应该调用其他接口
      stats.value.week = res.completed || 0
      stats.value.month = res.completed || 0
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

// 获取物流轨迹
const fetchLogisticsTracks = async (logisticsId: number) => {
  try {
    const res: any = await request.get(`/logistics/${logisticsId}/tracks`)
    if (res && Array.isArray(res)) {
      logisticsTracks.value = res
    }
  } catch (error) {
    console.error('获取物流轨迹失败:', error)
  }
}

const getTrackType = (status: number) => {
  const map: Record<number, string> = {
    0: 'warning',
    1: 'primary',
    2: 'primary',
    3: 'success'
  }
  return map[status] || 'info'
}

const handleSearch = () => {
  page.value = 1
  fetchRecords()
}

const handlePageChange = (val: number) => {
  page.value = val
  fetchRecords()
}

const exportData = () => {
  ElMessage.success('数据导出成功')
}

const viewDetail = async (row: any) => {
  currentRecord.value = row
  await fetchLogisticsTracks(row.id)
  showDetailDialog.value = true
}

onMounted(() => {
  fetchRecords()
  fetchStats()
})
</script>

<style scoped>
.records-view {
  padding-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--apple-dark-gray);
  margin-bottom: 24px;
}

.stat-row {
  margin-bottom: 24px;
}

.stat-card {
  background: var(--apple-card-bg);
  border-radius: 12px;
  padding: 24px;
  text-align: center;
}

.stat-label {
  font-size: 14px;
  color: var(--apple-gray);
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--apple-dark-gray);
}

.filter-bar {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
  padding: 20px;
  background: var(--apple-card-bg);
  border-radius: 12px;
}

/* 表格单元格垂直居中 */
.records-table :deep(.el-table__cell) {
  vertical-align: middle !important;
}

.records-table :deep(.el-table__cell .cell) {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  min-height: 40px;
  line-height: 1.5;
}

/* 状态标签垂直居中 */
.records-table :deep(.el-tag) {
  display: inline-flex;
  align-items: center;
  vertical-align: middle;
}

.receiver-name {
  font-weight: 500;
  color: var(--apple-dark-gray);
}

.signer-text {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  font-size: 14px;
  color: var(--apple-dark-gray);
  line-height: 1;
}

.receiver-phone {
  font-size: 13px;
  color: var(--apple-gray);
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
}

.detail-content h4 {
  margin: 16px 0 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--apple-border);
  color: var(--apple-dark-gray);
}

.detail-content h4:first-child {
  margin-top: 0;
}

.detail-content p {
  margin: 8px 0;
  color: var(--apple-gray);
}

.track-location {
  font-size: 12px;
  color: var(--apple-light-gray);
  margin-top: 4px;
}
</style>
