<template>
  <div class="waybill-view">
    <h2 class="page-title">运单管理</h2>

    <!-- 搜索筛选 -->
    <div class="filter-bar">
      <el-input v-model="searchKeyword" placeholder="运单号/收件人" style="width: 240px" clearable>
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-select v-model="filterStatus" placeholder="运单状态" clearable style="width: 140px">
        <el-option label="全部" value="" />
        <el-option label="待揽件" value="0" />
        <el-option label="运输中" value="1" />
        <el-option label="派送中" value="2" />
        <el-option label="已签收" value="3" />
      </el-select>
      <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" />
      <el-button type="primary" @click="handleSearch">查询</el-button>
    </div>

    <!-- 运单列表 -->
    <el-table :data="waybillList" style="width: 100%" v-loading="loading">
      <el-table-column prop="trackingNo" label="运单号" width="180" />
      <el-table-column label="收件信息" min-width="200">
        <template #default="{ row }">
          <div class="receiver-info">
            <p class="receiver-name">{{ row.receiverName }} {{ row.receiverPhone }}</p>
            <p class="receiver-address">{{ row.receiverAddress }}</p>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="商品信息" min-width="150">
        <template #default="{ row }">
          <div class="goods-info">
            <p class="goods-name">{{ row.goodsName }}</p>
            <p class="goods-weight">{{ row.goodsWeight }}kg</p>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="下单时间" width="160" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 0" type="primary" size="small" @click="pickup(row)">揽件</el-button>
          <el-button v-if="row.status === 1" type="warning" size="small" @click="startDeliver(row)">派送</el-button>
          <el-button v-if="row.status === 2" type="success" size="small" @click="sign(row)">签收</el-button>
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

    <!-- 揽件弹窗 -->
    <el-dialog v-model="showPickupDialog" title="确认揽件" width="400px">
      <p>确认揽收该订单？</p>
      <p class="dialog-waybill">运单号：{{ currentWaybill?.trackingNo }}</p>
      <template #footer>
        <el-button @click="showPickupDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmPickup" :loading="actionLoading">确认揽件</el-button>
      </template>
    </el-dialog>

    <!-- 派送弹窗 -->
    <el-dialog v-model="showDeliverDialog" title="开始派送" width="400px">
      <p>确认开始派送该订单？</p>
      <p class="dialog-waybill">运单号：{{ currentWaybill?.trackingNo }}</p>
      <p class="dialog-address">收件地址：{{ currentWaybill?.receiverAddress }}</p>
      <template #footer>
        <el-button @click="showDeliverDialog = false">取消</el-button>
        <el-button type="warning" @click="confirmDeliver" :loading="actionLoading">开始派送</el-button>
      </template>
    </el-dialog>

    <!-- 签收弹窗 -->
    <el-dialog v-model="showSignDialog" title="确认签收" width="400px">
      <el-form :model="signForm" label-width="100px">
        <el-form-item label="签收人">
          <el-input v-model="signForm.signer" placeholder="请输入签收人姓名" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="signForm.remark" type="textarea" rows="3" placeholder="请输入备注（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSignDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmSign" :loading="actionLoading">确认签收</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="showDetailDialog" title="运单详情" width="600px">
      <div v-if="currentWaybill" class="detail-content">
        <h4>基本信息</h4>
        <p><strong>运单号：</strong>{{ currentWaybill.trackingNo }}</p>
        <p><strong>状态：</strong><el-tag :type="getStatusType(currentWaybill.status)">{{ getStatusText(currentWaybill.status) }}</el-tag></p>
        <p><strong>创建时间：</strong>{{ currentWaybill.createTime }}</p>
        
        <h4>收件信息</h4>
        <p><strong>收件人：</strong>{{ currentWaybill.receiverName }}</p>
        <p><strong>电话：</strong>{{ currentWaybill.receiverPhone }}</p>
        <p><strong>地址：</strong>{{ currentWaybill.receiverAddress }}</p>
        
        <h4>商品信息</h4>
        <p><strong>商品名称：</strong>{{ currentWaybill.goodsName }}</p>
        <p><strong>重量：</strong>{{ currentWaybill.goodsWeight }}kg</p>
        
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
import { Search } from '@element-plus/icons-vue'
import request from '@/api/request'

const loading = ref(false)
const actionLoading = ref(false)
const searchKeyword = ref('')
const filterStatus = ref('')
const dateRange = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const waybillList = ref<any[]>([])
const logisticsTracks = ref<any[]>([])

const showPickupDialog = ref(false)
const showDeliverDialog = ref(false)
const showSignDialog = ref(false)
const showDetailDialog = ref(false)
const currentWaybill = ref<any>(null)
const signForm = ref({
  signer: '',
  remark: ''
})

// 获取运单列表
const fetchWaybillList = async () => {
  loading.value = true
  try {
    let url = ''
    const params: any = { page: page.value, size: pageSize.value }
    
    // 根据状态选择接口
    if (filterStatus.value === '0') {
      url = '/logistics/pending'
    } else if (filterStatus.value === '1' || filterStatus.value === '2') {
      url = '/logistics/delivering'
    } else {
      // 全部状态，这里简化处理，只查询待揽件和配送中
      url = '/logistics/pending'
    }
    
    const res: any = await request.get(url, { params })
    if (res && Array.isArray(res)) {
      // 处理数据，补充订单信息
      const processedList = await Promise.all(res.map(async (item: any) => {
        // 获取订单详情
        try {
          const orderRes: any = await request.get(`/logistics/order/${item.orderId}/detail`)
          if (orderRes) {
            return {
              ...item,
              receiverName: orderRes.receiverName,
              receiverPhone: orderRes.receiverPhone,
              receiverAddress: orderRes.receiverAddress,
              goodsName: orderRes.items?.[0]?.productName || '商品',
              goodsWeight: orderRes.items?.[0]?.quantity || 1,
              createTime: orderRes.createTime
            }
          }
        } catch (e) {
          console.error('获取订单详情失败:', e)
        }
        return item
      }))
      waybillList.value = processedList
      total.value = res.length
    }
  } catch (error) {
    console.error('获取运单列表失败:', error)
    ElMessage.error('获取运单列表失败')
  } finally {
    loading.value = false
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

const getStatusType = (status: number) => {
  const map: Record<number, string> = {
    0: 'warning',
    1: 'primary',
    2: 'primary',
    3: 'success'
  }
  return map[status] || 'info'
}

const getStatusText = (status: number) => {
  const map: Record<number, string> = {
    0: '待揽件',
    1: '运输中',
    2: '派送中',
    3: '已签收'
  }
  return map[status] || '未知'
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
  fetchWaybillList()
}

const handlePageChange = (val: number) => {
  page.value = val
  fetchWaybillList()
}

const pickup = (row: any) => {
  currentWaybill.value = row
  showPickupDialog.value = true
}

const confirmPickup = async () => {
  if (!currentWaybill.value) return
  actionLoading.value = true
  try {
    await request.post(`/logistics/${currentWaybill.value.id}/pickup`)
    ElMessage.success('揽件成功')
    showPickupDialog.value = false
    fetchWaybillList()
  } catch (error) {
    console.error('揽件失败:', error)
    ElMessage.error('揽件失败')
  } finally {
    actionLoading.value = false
  }
}

const startDeliver = (row: any) => {
  currentWaybill.value = row
  showDeliverDialog.value = true
}

const confirmDeliver = async () => {
  if (!currentWaybill.value) return
  actionLoading.value = true
  try {
    await request.post(`/logistics/${currentWaybill.value.id}/deliver`)
    ElMessage.success('开始派送')
    showDeliverDialog.value = false
    fetchWaybillList()
  } catch (error) {
    console.error('操作失败:', error)
    ElMessage.error('操作失败')
  } finally {
    actionLoading.value = false
  }
}

const sign = (row: any) => {
  currentWaybill.value = row
  showSignDialog.value = true
}

const confirmSign = async () => {
  if (!currentWaybill.value) return
  if (!signForm.value.signer) {
    ElMessage.warning('请输入签收人姓名')
    return
  }
  actionLoading.value = true
  try {
    await request.post(`/logistics/${currentWaybill.value.id}/sign`, {
      signer: signForm.value.signer,
      remark: signForm.value.remark
    })
    ElMessage.success('签收成功')
    showSignDialog.value = false
    signForm.value = { signer: '', remark: '' }
    fetchWaybillList()
  } catch (error) {
    console.error('签收失败:', error)
    ElMessage.error('签收失败')
  } finally {
    actionLoading.value = false
  }
}

const viewDetail = async (row: any) => {
  currentWaybill.value = row
  await fetchLogisticsTracks(row.id)
  showDetailDialog.value = true
}

onMounted(() => {
  fetchWaybillList()
})
</script>

<style scoped>
.waybill-view {
  padding-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--apple-dark-gray);
  margin-bottom: 24px;
}

.filter-bar {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
  padding: 20px;
  background: var(--apple-card-bg);
  border-radius: 12px;
}

.receiver-info .receiver-name {
  font-weight: 500;
  color: var(--apple-dark-gray);
  margin-bottom: 4px;
}

.receiver-info .receiver-address {
  font-size: 13px;
  color: var(--apple-gray);
}

.goods-info .goods-name {
  color: var(--apple-dark-gray);
  margin-bottom: 4px;
}

.goods-info .goods-weight {
  font-size: 13px;
  color: var(--apple-gray);
}

.dialog-waybill {
  margin-top: 12px;
  color: var(--apple-gray);
}

.dialog-address {
  margin-top: 8px;
  color: var(--apple-gray);
  font-size: 13px;
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
