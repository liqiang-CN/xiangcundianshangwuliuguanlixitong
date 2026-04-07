<template>
  <div class="route-view">
    <h2 class="page-title">路线规划</h2>

    <div class="route-container">
      <!-- 左侧：配送点列表 -->
      <div class="point-list">
        <div class="list-header">
          <h3>今日配送点</h3>
          <el-button type="primary" size="small" @click="optimizeRoute">
            <el-icon><MagicStick /></el-icon>
            智能规划
          </el-button>
        </div>
        <div class="list-content" v-loading="loading">
          <el-timeline v-if="deliveryPoints.length > 0">
            <el-timeline-item
              v-for="(point, index) in deliveryPoints"
              :key="point.id"
              :type="point.status === 'completed' ? 'success' : 'primary'"
            >
              <div class="point-item" :class="{ active: currentNavIndex === index && isNavigating }">
                <div class="point-index">{{ index + 1 }}</div>
                <div class="point-info">
                  <h4 class="point-name">{{ point.name }}</h4>
                  <p class="point-address">{{ point.address }}</p>
                  <p class="point-waybill">运单：{{ point.waybillNo }}</p>
                  <p v-if="isNavigating && currentNavIndex === index" class="nav-status">
                    <el-icon class="nav-icon"><Van /></el-icon>
                    正在配送中...
                  </p>
                </div>
                <div class="point-actions">
                  <el-tag v-if="point.status === 'completed'" type="success" size="small">已完成</el-tag>
                  <el-tag v-else-if="point.logisticsStatus === 1" type="primary" size="small">运输中</el-tag>
                  <el-tag v-else-if="point.logisticsStatus === 2" type="warning" size="small">派送中</el-tag>
                  <el-tag v-else type="info" size="small">待配送</el-tag>
                  <el-button
                    v-if="isNavigating && currentNavIndex === index && point.logisticsStatus === 2"
                    type="success"
                    size="small"
                    class="sign-btn"
                    @click="quickSign(point)"
                  >
                    签收
                  </el-button>
                  <el-button
                    v-else-if="isNavigating && currentNavIndex === index && point.logisticsStatus === 1"
                    type="warning"
                    size="small"
                    class="sign-btn"
                    @click="startDeliverCurrent"
                  >
                    开始派送
                  </el-button>
                </div>
              </div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无配送任务" />
        </div>
        <div class="list-footer">
          <div class="route-info">
            <p>总距离：<span class="highlight">{{ routeInfo.distance }}km</span></p>
            <p>预计耗时：<span class="highlight">{{ routeInfo.duration }}分钟</span></p>
            <p>配送点：<span class="highlight">{{ deliveryPoints.length }}个</span></p>
            <p v-if="isNavigating">当前：第{{ currentNavIndex + 1 }}个</p>
          </div>
          <el-button v-if="!isNavigating" type="primary" @click="startNavigation">
            <el-icon><Position /></el-icon>
            开始导航
          </el-button>
          <el-button v-else type="danger" @click="stopNavigation">
            <el-icon><CircleClose /></el-icon>
            结束导航
          </el-button>
        </div>
      </div>

      <!-- 右侧：地图模拟 -->
      <div class="map-container">
        <div v-if="!isNavigating" class="map-placeholder">
          <el-icon size="64" color="#ccc"><MapLocation /></el-icon>
          <p>点击"开始导航"开始模拟配送</p>
          <p class="map-tip">系统将自动模拟配送路线</p>
        </div>
        <div v-else class="nav-simulation">
          <div class="nav-header">
            <h3>🚚 配送模拟中</h3>
            <p>正在前往第 {{ currentNavIndex + 1 }} 个配送点</p>
          </div>
          <div class="nav-progress">
            <el-progress :percentage="navProgress" :stroke-width="20" status="success">
              <template #default>
                <span class="progress-text">{{ navProgress.toFixed(0) }}%</span>
              </template>
            </el-progress>
          </div>
          <div class="nav-current-point" v-if="currentPoint">
            <div class="point-card">
              <h4>{{ currentPoint.name }}</h4>
              <p>{{ currentPoint.address }}</p>
              <p class="waybill">运单：{{ currentPoint.waybillNo }}</p>
              <p class="phone">电话：{{ currentPoint.phone }}</p>
            </div>
          </div>
          <div class="nav-animation">
            <div class="road">
              <div class="car" :style="{ left: navProgress + '%' }">🚚</div>
            </div>
            <div class="delivery-points">
              <div
                v-for="(point, index) in deliveryPoints"
                :key="point.id"
                class="point-marker"
                :class="{ completed: index < currentNavIndex, current: index === currentNavIndex, pending: index > currentNavIndex }"
                :style="{ left: ((index + 1) / deliveryPoints.length * 100) + '%' }"
              >
                {{ index + 1 }}
              </div>
            </div>
          </div>
          <div class="nav-actions">
            <template v-if="currentPoint && currentPoint.logisticsStatus === 1">
              <el-button type="warning" size="large" @click="startDeliverCurrent">
                <el-icon><Van /></el-icon>
                开始派送
              </el-button>
            </template>
            <template v-else>
              <el-button type="success" size="large" @click="signCurrent">
                <el-icon><CircleCheck /></el-icon>
                确认送达并签收
              </el-button>
            </template>
            <el-button type="primary" size="large" :disabled="currentNavIndex >= deliveryPoints.length - 1" @click="nextPoint">
              <el-icon><ArrowRight /></el-icon>
              下一个
            </el-button>
          </div>
        </div>
      </div>
    </div>

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
        <el-button type="success" @click="confirmSign" :loading="signLoading">确认签收</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MagicStick, Position, MapLocation, Van, CircleClose, CircleCheck, ArrowRight } from '@element-plus/icons-vue'
import request from '@/api/request'

const loading = ref(false)
const signLoading = ref(false)
const isNavigating = ref(false)
const isDelivering = ref(false) // 是否开始派送（控制动画）
const currentNavIndex = ref(0)
const navProgress = ref(0)

const deliveryPoints = ref<any[]>([])

const routeInfo = ref({
  distance: 0,
  duration: 0
})

const showSignDialog = ref(false)
const currentSignPoint = ref<any>(null)
const signForm = ref({
  signer: '',
  remark: ''
})

const currentPoint = computed(() => {
  if (isNavigating.value && currentNavIndex.value < deliveryPoints.value.length) {
    return deliveryPoints.value[currentNavIndex.value]
  }
  return null
})

// 获取配送点列表（派送中的任务）
const fetchDeliveryPoints = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/logistics/delivering', {
      params: { page: 1, size: 100 }
    })

    if (res && Array.isArray(res)) {
      // 处理数据，补充订单信息
      const processedList = await Promise.all(res.map(async (item: any, index: number) => {
        try {
          const orderRes: any = await request.get(`/logistics/order/${item.orderId}/detail`)
          if (orderRes) {
            return {
              id: item.id,
              logisticsId: item.id,
              logisticsStatus: item.status,
              name: orderRes.receiverName,
              address: orderRes.receiverAddress,
              waybillNo: item.trackingNo,
              status: 'pending',
              phone: orderRes.receiverPhone,
              lat: 39.9 + Math.random() * 0.1, // 模拟坐标
              lng: 116.4 + Math.random() * 0.1
            }
          }
        } catch (e) {
          console.error('获取订单详情失败:', e)
        }
        return null
      }))

      deliveryPoints.value = processedList.filter(item => item !== null)

      // 计算路线信息（简化计算）
      calculateRouteInfo()
    }
  } catch (error) {
    console.error('获取配送点失败:', error)
    ElMessage.error('获取配送点失败')
  } finally {
    loading.value = false
  }
}

// 计算路线信息（简化版）
const calculateRouteInfo = () => {
  const count = deliveryPoints.value.length
  if (count === 0) {
    routeInfo.value = { distance: 0, duration: 0 }
    return
  }

  // 简单估算：每个点之间平均5公里，每公里2分钟
  const avgDistancePerPoint = 5
  const timePerKm = 2

  const totalDistance = count * avgDistancePerPoint
  const totalDuration = totalDistance * timePerKm

  routeInfo.value = {
    distance: totalDistance,
    duration: totalDuration
  }
}

const optimizeRoute = () => {
  // 简单模拟路线优化（按地址排序）
  deliveryPoints.value.sort((a, b) => a.address.localeCompare(b.address))
  ElMessage.success('路线已优化，预计节省15分钟')
  calculateRouteInfo()
}

// 开始导航（模拟）
const startNavigation = () => {
  if (deliveryPoints.value.length === 0) {
    ElMessage.warning('暂无配送任务')
    return
  }
  isNavigating.value = true
  isDelivering.value = false // 初始状态，动画不开始
  currentNavIndex.value = 0
  navProgress.value = 0
  ElMessage.success('导航已启动，请点击"开始派送"开始配送')
}

// 模拟进度
const simulateProgress = () => {
  const interval = setInterval(() => {
    if (!isNavigating.value || !isDelivering.value) {
      clearInterval(interval)
      return
    }
    if (navProgress.value < 100) {
      navProgress.value += Math.random() * 5
      if (navProgress.value > 100) navProgress.value = 100
    } else {
      clearInterval(interval)
    }
  }, 500)
}

// 结束导航
const stopNavigation = () => {
  ElMessageBox.confirm('确定要结束导航吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    isNavigating.value = false
    isDelivering.value = false
    currentNavIndex.value = 0
    navProgress.value = 0
    // 注意：不重置 logisticsStatus，保持和后端状态一致
    ElMessage.info('导航已结束')
  }).catch(() => {})
}

// 下一个配送点
const nextPoint = () => {
  if (currentNavIndex.value < deliveryPoints.value.length - 1) {
    currentNavIndex.value++
    navProgress.value = 0
    isDelivering.value = false // 重置派送状态，需要重新点击"开始派送"
    ElMessage.info(`前往第 ${currentNavIndex.value + 1} 个配送点，请点击"开始派送"`)
  }
}

// 签收当前配送点
const signCurrent = () => {
  if (currentPoint.value) {
    quickSign(currentPoint.value)
  }
}

// 开始派送当前配送点
const startDeliverCurrent = async () => {
  if (!currentPoint.value) return
  try {
    // 如果已经是派送中状态，直接开始动画，不再调用后端接口
    if (currentPoint.value.logisticsStatus !== 2) {
      await request.post(`/logistics/${currentPoint.value.logisticsId}/deliver`)
      ElMessage.success('开始派送')
      // 更新本地状态
      currentPoint.value.logisticsStatus = 2
      // 更新列表中的状态
      const index = deliveryPoints.value.findIndex(p => p.id === currentPoint.value.id)
      if (index !== -1) {
        deliveryPoints.value[index].logisticsStatus = 2
      }
    }
    // 开始动画
    isDelivering.value = true
    simulateProgress()
  } catch (error) {
    console.error('开始派送失败:', error)
    ElMessage.error('开始派送失败')
  }
}

// 快速签收
const quickSign = (point: any) => {
  currentSignPoint.value = point
  signForm.value.signer = point.name || '本人'
  signForm.value.remark = ''
  showSignDialog.value = true
}

// 确认签收
const confirmSign = async () => {
  if (!currentSignPoint.value) return
  if (!signForm.value.signer) {
    ElMessage.warning('请输入签收人姓名')
    return
  }

  signLoading.value = true
  try {
    await request.post(`/logistics/${currentSignPoint.value.logisticsId}/sign`, {
      signer: signForm.value.signer,
      remark: signForm.value.remark
    })
    ElMessage.success('签收成功')
    showSignDialog.value = false

    // 更新本地状态
    const index = deliveryPoints.value.findIndex(p => p.id === currentSignPoint.value.id)
    if (index !== -1) {
      deliveryPoints.value[index].status = 'completed'
    }

    // 自动下一个
    setTimeout(() => {
      if (currentNavIndex.value < deliveryPoints.value.length - 1) {
        nextPoint()
      } else {
        ElMessage.success('所有配送点已完成！')
        isNavigating.value = false
        // 刷新列表
        fetchDeliveryPoints()
      }
    }, 1000)
  } catch (error) {
    console.error('签收失败:', error)
    ElMessage.error('签收失败')
  } finally {
    signLoading.value = false
  }
}

onMounted(() => {
  fetchDeliveryPoints()
})
</script>

<style scoped>
.route-view {
  padding-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--apple-dark-gray);
  margin-bottom: 24px;
}

.route-container {
  display: flex;
  gap: 24px;
  height: calc(100vh - 200px);
}

.point-list {
  width: 400px;
  background: var(--apple-card-bg);
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid var(--apple-light-gray);
}

.list-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--apple-dark-gray);
}

.list-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.point-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  transition: all 0.3s;
}

.point-item.active {
  background: rgba(0, 122, 255, 0.1);
  border: 1px solid var(--apple-blue);
}

.point-index {
  width: 28px;
  height: 28px;
  background: var(--apple-blue);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}

.point-info {
  flex: 1;
}

.point-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--apple-dark-gray);
  margin-bottom: 4px;
}

.point-address {
  font-size: 13px;
  color: var(--apple-gray);
  margin-bottom: 4px;
}

.point-waybill {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}

.nav-status {
  font-size: 13px;
  color: var(--apple-orange);
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 4px;
}

.nav-icon {
  animation: bounce 1s infinite;
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-3px); }
}

.point-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.sign-btn {
  margin-top: 4px;
}

.list-footer {
  padding: 20px;
  border-top: 1px solid var(--apple-light-gray);
  background: var(--apple-bg);
}

.route-info {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.route-info p {
  font-size: 14px;
  color: var(--apple-gray);
}

.route-info .highlight {
  color: var(--apple-blue);
  font-weight: 600;
}

.list-footer .el-button {
  width: 100%;
  height: 44px;
  border-radius: 22px;
}

.map-container {
  flex: 1;
  background: var(--apple-card-bg);
  border-radius: 12px;
  overflow: hidden;
}

.map-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #999;
}

.map-placeholder p {
  margin-top: 16px;
}

.map-tip {
  font-size: 12px;
  color: #bbb;
  margin-top: 8px;
}

/* 导航模拟样式 */
.nav-simulation {
  padding: 40px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.nav-header {
  text-align: center;
  margin-bottom: 30px;
}

.nav-header h3 {
  font-size: 24px;
  color: var(--apple-dark-gray);
  margin-bottom: 8px;
}

.nav-header p {
  color: var(--apple-gray);
}

.nav-progress {
  margin-bottom: 30px;
}

.progress-text {
  font-weight: 600;
  color: var(--apple-green);
}

.nav-current-point {
  margin-bottom: 30px;
}

.point-card {
  background: var(--apple-bg);
  border-radius: 12px;
  padding: 20px;
  text-align: center;
}

.point-card h4 {
  font-size: 20px;
  color: var(--apple-dark-gray);
  margin-bottom: 8px;
}

.point-card p {
  color: var(--apple-gray);
  margin-bottom: 4px;
}

.point-card .waybill,
.point-card .phone {
  font-size: 13px;
  color: #999;
}

.nav-animation {
  flex: 1;
  position: relative;
  margin-bottom: 30px;
}

.road {
  position: absolute;
  top: 50%;
  left: 10%;
  right: 10%;
  height: 4px;
  background: var(--apple-light-gray);
  border-radius: 2px;
  transform: translateY(-50%);
}

.car {
  position: absolute;
  top: 50%;
  transform: translate(-50%, -50%);
  font-size: 32px;
  transition: left 0.5s ease;
}

.delivery-points {
  position: absolute;
  top: 50%;
  left: 10%;
  right: 10%;
  transform: translateY(-50%);
}

.point-marker {
  position: absolute;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  transform: translateX(-50%);
  transition: all 0.3s;
}

.point-marker.completed {
  background: var(--apple-green);
  color: white;
}

.point-marker.current {
  background: var(--apple-orange);
  color: white;
  animation: pulse 1s infinite;
}

.point-marker.pending {
  background: var(--apple-light-gray);
  color: var(--apple-gray);
}

@keyframes pulse {
  0%, 100% { transform: translateX(-50%) scale(1); }
  50% { transform: translateX(-50%) scale(1.1); }
}

.nav-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
}

.nav-actions .el-button {
  min-width: 160px;
  height: 48px;
  border-radius: 24px;
}
</style>
