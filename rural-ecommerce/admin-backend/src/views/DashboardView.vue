<template>
  <div class="dashboard-view">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <div class="stat-card" v-loading="loading">
          <div class="stat-icon" style="background: #e6f7ff; color: #1890ff;">
            <el-icon size="32"><ShoppingCart /></el-icon>
          </div>
          <div class="stat-info">
            <p class="stat-label">今日订单</p>
            <p class="stat-value">{{ stats.todayOrders }}</p>
            <p class="stat-change" :class="stats.ordersChange >= 0 ? 'up' : 'down'">
              {{ formatChange(stats.ordersChange) }} <span>较昨日</span>
            </p>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" v-loading="loading">
          <div class="stat-icon" style="background: #f6ffed; color: #52c41a;">
            <el-icon size="32"><Money /></el-icon>
          </div>
          <div class="stat-info">
            <p class="stat-label">今日交易额</p>
            <p class="stat-value">{{ formatAmount(stats.todayAmount) }}</p>
            <p class="stat-change" :class="stats.amountChange >= 0 ? 'up' : 'down'">
              {{ formatChange(stats.amountChange) }} <span>较昨日</span>
            </p>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" v-loading="loading">
          <div class="stat-icon" style="background: #fff7e6; color: #fa8c16;">
            <el-icon size="32"><User /></el-icon>
          </div>
          <div class="stat-info">
            <p class="stat-label">新增用户</p>
            <p class="stat-value">{{ stats.newUsers }}</p>
            <p class="stat-change" :class="stats.usersChange >= 0 ? 'up' : 'down'">
              {{ formatChange(stats.usersChange) }} <span>较昨日</span>
            </p>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" v-loading="loading">
          <div class="stat-icon" style="background: #f9f0ff; color: #722ed1;">
            <el-icon size="32"><Van /></el-icon>
          </div>
          <div class="stat-info">
            <p class="stat-label">待配送订单</p>
            <p class="stat-value">{{ stats.pendingDelivery }}</p>
            <p class="stat-change up">待处理</p>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :span="16">
        <div class="chart-card">
          <div class="chart-header">
            <h3 class="chart-title">交易趋势</h3>
            <el-radio-group v-model="timeRange" size="small">
              <el-radio-button label="week">本周</el-radio-button>
              <el-radio-button label="month">本月</el-radio-button>
              <el-radio-button label="year">本年</el-radio-button>
            </el-radio-group>
          </div>
          <v-chart class="chart" :option="tradeChartOption" autoresize />
        </div>
      </el-col>
      <el-col :span="8">
        <div class="chart-card">
          <div class="chart-header">
            <h3 class="chart-title">订单状态分布</h3>
          </div>
          <v-chart class="chart" :option="orderPieOption" autoresize />
        </div>
      </el-col>
    </el-row>

    <!-- 排行榜 -->
    <el-row :gutter="16" class="rank-row">
      <el-col :span="12">
        <div class="rank-card">
          <div class="rank-header">
            <h3 class="rank-title">热销商品排行</h3>
            <el-button link type="primary">查看更多</el-button>
          </div>
          <el-table :data="hotProducts" style="width: 100%">
            <el-table-column type="index" width="50" />
            <el-table-column prop="name" label="商品名称" />
            <el-table-column prop="sales" label="销量" width="100" />
            <el-table-column prop="amount" label="销售额" width="120" />
          </el-table>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="rank-card">
          <div class="rank-header">
            <h3 class="rank-title">农户销售排行</h3>
            <el-button link type="primary">查看更多</el-button>
          </div>
          <el-table :data="topFarmers" style="width: 100%">
            <el-table-column type="index" width="50" />
            <el-table-column prop="name" label="农户名称" />
            <el-table-column prop="orders" label="订单数" width="100" />
            <el-table-column prop="amount" label="销售额" width="120" />
          </el-table>
        </div>
      </el-col>
    </el-row>

    <!-- 待办事项 -->
    <el-row :gutter="16">
      <el-col :span="24">
        <div class="todo-card">
          <div class="todo-header">
            <h3 class="todo-title">待办事项</h3>
          </div>
          <el-row :gutter="16">
            <el-col :span="6">
              <div class="todo-item">
                <div class="todo-icon pending">
                  <el-icon size="24"><Box /></el-icon>
                </div>
                <div class="todo-info">
                  <p class="todo-count">12</p>
                  <p class="todo-label">待审核商品</p>
                </div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="todo-item">
                <div class="todo-icon warning">
                  <el-icon size="24"><Warning /></el-icon>
                </div>
                <div class="todo-info">
                  <p class="todo-count">5</p>
                  <p class="todo-label">异常订单</p>
                </div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="todo-item">
                <div class="todo-icon refund">
                  <el-icon size="24"><RefreshLeft /></el-icon>
                </div>
                <div class="todo-info">
                  <p class="todo-count">8</p>
                  <p class="todo-label">退款申请</p>
                </div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="todo-item">
                <div class="todo-icon farmer">
                  <el-icon size="24"><User /></el-icon>
                </div>
                <div class="todo-info">
                  <p class="todo-count">3</p>
                  <p class="todo-label">待审核农户</p>
                </div>
              </div>
            </el-col>
          </el-row>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent } from 'echarts/components'
import { ShoppingCart, Money, User, Van, Box, Warning, RefreshLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

use([CanvasRenderer, LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent])

const timeRange = ref('week')
const loading = ref(false)

// 统计数据
const stats = ref({
  todayOrders: 0,
  ordersChange: 0,
  todayAmount: 0,
  amountChange: 0,
  newUsers: 0,
  usersChange: 0,
  pendingDelivery: 0
})

const orderStatus = ref({
  unpaid: 0,
  unshipped: 0,
  unreceived: 0,
  completed: 0,
  cancelled: 0
})

const tradeTrend = ref<any[]>([])
const hotProducts = ref<any[]>([])
const topFarmers = ref<any[]>([])

// 获取统计数据
const fetchDashboardStats = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/admin/dashboard/stats')
    if (res) {
      // 今日统计
      stats.value.todayOrders = res.today?.orders || 0
      stats.value.ordersChange = res.today?.ordersChange || 0
      stats.value.todayAmount = res.today?.amount || 0
      stats.value.amountChange = res.today?.amountChange || 0
      stats.value.newUsers = res.today?.newUsers || 0
      stats.value.usersChange = res.today?.usersChange || 0
      stats.value.pendingDelivery = res.today?.pendingDelivery || 0

      // 订单状态分布
      orderStatus.value = res.orderStatus || {
        unpaid: 0,
        unshipped: 0,
        unreceived: 0,
        completed: 0,
        cancelled: 0
      }

      // 交易趋势
      tradeTrend.value = res.tradeTrend || []

      // 热销商品
      hotProducts.value = res.hotProducts || []

      // 农户排行
      topFarmers.value = res.topFarmers || []
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
    ElMessage.error('获取统计数据失败')
  } finally {
    loading.value = false
  }
}

// 格式化金额
const formatAmount = (amount: number) => {
  if (amount >= 10000) {
    return '¥' + (amount / 10000).toFixed(1) + '万'
  }
  return '¥' + amount.toFixed(0)
}

// 格式化变化率
const formatChange = (change: number) => {
  const sign = change >= 0 ? '+' : ''
  return sign + change.toFixed(1) + '%'
}

// 交易趋势图表配置
const tradeChartOption = computed(() => {
  const dates = tradeTrend.value.map(item => item.date)
  const orders = tradeTrend.value.map(item => item.orders)
  const amounts = tradeTrend.value.map(item => item.amount)

  return {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['订单量', '交易额']
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates.length > 0 ? dates : ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    },
    yAxis: [
      {
        type: 'value',
        name: '订单量'
      },
      {
        type: 'value',
        name: '交易额',
        axisLabel: {
          formatter: '¥{value}'
        }
      }
    ],
    series: [
      {
        name: '订单量',
        type: 'line',
        data: orders.length > 0 ? orders : [120, 132, 101, 134, 90, 230, 210],
        smooth: true,
        itemStyle: { color: '#1890ff' }
      },
      {
        name: '交易额',
        type: 'line',
        yAxisIndex: 1,
        data: amounts.length > 0 ? amounts : [8200, 9320, 9010, 9340, 12900, 13300, 13200],
        smooth: true,
        itemStyle: { color: '#52c41a' }
      }
    ]
  }
})

// 订单状态饼图配置
const orderPieOption = computed(() => ({
  tooltip: {
    trigger: 'item'
  },
  legend: {
    orient: 'vertical',
    right: 10,
    top: 'center'
  },
  series: [
    {
      name: '订单状态',
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 10,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: {
        show: false
      },
      data: [
        { value: orderStatus.value.unpaid || 0, name: '待付款', itemStyle: { color: '#faad14' } },
        { value: orderStatus.value.unshipped || 0, name: '待发货', itemStyle: { color: '#1890ff' } },
        { value: orderStatus.value.unreceived || 0, name: '待收货', itemStyle: { color: '#722ed1' } },
        { value: orderStatus.value.completed || 0, name: '已完成', itemStyle: { color: '#52c41a' } },
        { value: orderStatus.value.cancelled || 0, name: '已取消', itemStyle: { color: '#f5222d' } }
      ]
    }
  ]
}))

onMounted(() => {
  fetchDashboardStats()
})
</script>

<style scoped>
.dashboard-view {
  padding-bottom: 24px;
}

.stat-row {
  margin-bottom: 16px;
}

.stat-card {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #333;
  margin-bottom: 8px;
}

.stat-change {
  font-size: 13px;
}

.stat-change.up {
  color: #52c41a;
}

.stat-change.down {
  color: #f5222d;
}

.stat-change span {
  color: #999;
}

.chart-row {
  margin-bottom: 16px;
}

.chart-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.chart-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.chart {
  height: 300px;
}

.rank-row {
  margin-bottom: 16px;
}

.rank-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
}

.rank-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.rank-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.todo-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
}

.todo-header {
  margin-bottom: 20px;
}

.todo-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.todo-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #f5f5f5;
  border-radius: 8px;
}

.todo-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.todo-icon.pending {
  background: #1890ff;
}

.todo-icon.warning {
  background: #faad14;
}

.todo-icon.refund {
  background: #f5222d;
}

.todo-icon.farmer {
  background: #52c41a;
}

.todo-count {
  font-size: 24px;
  font-weight: 700;
  color: #333;
}

.todo-label {
  font-size: 13px;
  color: #666;
  margin-top: 4px;
}
</style>
