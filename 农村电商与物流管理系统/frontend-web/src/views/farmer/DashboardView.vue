<template>
  <div class="farmer-dashboard">
    <h2 class="page-title">数据概览</h2>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon blue">
            <el-icon size="32"><Money /></el-icon>
          </div>
          <div class="stat-info">
            <p class="stat-label">今日收入</p>
            <p class="stat-value">¥{{ stats.todayIncome }}</p>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon green">
            <el-icon size="32"><ShoppingCart /></el-icon>
          </div>
          <div class="stat-info">
            <p class="stat-label">今日订单</p>
            <p class="stat-value">{{ stats.todayOrders }}</p>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon orange">
            <el-icon size="32"><Goods /></el-icon>
          </div>
          <div class="stat-info">
            <p class="stat-label">在售商品</p>
            <p class="stat-value">{{ stats.onSaleProducts }}</p>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon purple">
            <el-icon size="32"><View /></el-icon>
          </div>
          <div class="stat-info">
            <p class="stat-label">商品浏览</p>
            <p class="stat-value">{{ stats.productViews }}</p>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :span="16">
        <div class="chart-card">
          <div class="chart-header">
            <h3 class="chart-title">销售趋势</h3>
            <el-radio-group v-model="timeRange" size="small">
              <el-radio-button label="week">本周</el-radio-button>
              <el-radio-button label="month">本月</el-radio-button>
              <el-radio-button label="year">本年</el-radio-button>
            </el-radio-group>
          </div>
          <v-chart class="chart" :option="salesChartOption" autoresize />
        </div>
      </el-col>
      <el-col :span="8">
        <div class="chart-card">
          <h3 class="chart-title">商品销量排行</h3>
          <div class="rank-list">
            <div v-for="(item, index) in productRank" :key="index" class="rank-item">
              <span class="rank-index" :class="{ top: index < 3 }">{{ index + 1 }}</span>
              <span class="rank-name">{{ item.name }}</span>
              <span class="rank-value">{{ item.sales }}件</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 待办事项 -->
    <el-row :gutter="16">
      <el-col :span="12">
        <div class="todo-card">
          <h3 class="card-title">待处理订单</h3>
          <el-table :data="pendingOrders" style="width: 100%">
            <el-table-column prop="orderNo" label="订单号" />
            <el-table-column prop="productName" label="商品" />
            <el-table-column prop="amount" label="金额" />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="shipOrder(row.id)">发货</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="todo-card">
          <h3 class="card-title">消息通知</h3>
          <div class="message-list">
            <div v-for="msg in messages" :key="msg.id" class="message-item">
              <div class="message-icon" :class="msg.type">
                <el-icon><Bell v-if="msg.type === 'order'" /><Warning v-else-if="msg.type === 'warning'" /><InfoFilled v-else /></el-icon>
              </div>
              <div class="message-content">
                <p class="message-title">{{ msg.title }}</p>
                <p class="message-time">{{ msg.time }}</p>
              </div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Money, ShoppingCart, Goods, View, Bell, Warning, InfoFilled } from '@element-plus/icons-vue'
import request from '@/api/request'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent } from 'echarts/components'
import VChart from 'vue-echarts'

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent])

const stats = ref({
  todayIncome: 0,
  todayOrders: 0,
  onSaleProducts: 0,
  productViews: 0
})

const productRank = ref<any[]>([])
const pendingOrders = ref<any[]>([])
const messages = ref<any[]>([])
const timeRange = ref('week')

// 销售趋势图表配置（和后台管理系统一样）
const salesChartOption = computed(() => ({
  tooltip: {
    trigger: 'axis'
  },
  legend: {
    data: ['订单量', '销售额'],
    top: 0
  },
  grid: {
    left: '3%',
    right: '4%',
    bottom: '3%',
    top: 40,
    containLabel: true
  },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  },
  yAxis: [
    {
      type: 'value',
      name: '订单量'
    },
    {
      type: 'value',
      name: '销售额',
      axisLabel: {
        formatter: '¥{value}'
      }
    }
  ],
  series: [
    {
      name: '订单量',
      type: 'line',
      data: [12, 15, 8, 20, 18, 25, 22],
      smooth: true,
      itemStyle: { color: '#1890ff' }
    },
    {
      name: '销售额',
      type: 'line',
      yAxisIndex: 1,
      data: [1200, 1500, 800, 2000, 1800, 2500, 2200],
      smooth: true,
      itemStyle: { color: '#52c41a' }
    }
  ]
}))

// 获取统计数据
const fetchStats = async () => {
  try {
    const res: any = await request.get('/farmer/stats')
    if (res) {
      stats.value = {
        todayIncome: res.todayIncome || 0,
        todayOrders: res.todayOrders || 0,
        onSaleProducts: res.onSaleProducts || 0,
        productViews: res.productViews || 0
      }
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

// 获取待处理订单
const fetchPendingOrders = async () => {
  try {
    const res: any = await request.get('/farmer/orders', {
      params: { status: 1, page: 1, size: 5 }
    })
    if (res && Array.isArray(res)) {
      pendingOrders.value = res.map((order: any) => ({
        id: order.id,
        orderNo: order.orderNo,
        productName: order.items?.[0]?.productName || '未知商品',
        amount: `¥${order.payAmount}`
      }))
    }
  } catch (error) {
    console.error('获取待处理订单失败:', error)
  }
}

// 获取商品销量排行
const fetchProductRank = async () => {
  try {
    const res: any = await request.get('/farmer/products', {
      params: { page: 1, size: 100, status: 1 }
    })
    if (res && Array.isArray(res)) {
      // 按销量从高到低排序
      productRank.value = res
        .sort((a: any, b: any) => (b.sales || 0) - (a.sales || 0))
        .map((product: any) => ({
          name: product.name,
          sales: product.sales || 0
        }))
    }
  } catch (error) {
    console.error('获取商品销量排行失败:', error)
  }
}

const shipOrder = (id: number) => {
  ElMessage.success('发货成功')
}

onMounted(() => {
  fetchStats()
  fetchPendingOrders()
  fetchProductRank()
})
</script>

<style scoped>
.farmer-dashboard {
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
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.stat-icon.blue { background: linear-gradient(135deg, #667eea, #764ba2); }
.stat-icon.green { background: linear-gradient(135deg, #11998e, #38ef7d); }
.stat-icon.orange { background: linear-gradient(135deg, #f093fb, #f5576c); }
.stat-icon.purple { background: linear-gradient(135deg, #4facfe, #00f2fe); }

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

.chart-row {
  margin-bottom: 24px;
}

.chart-card {
  background: var(--apple-card-bg);
  border-radius: 12px;
  padding: 24px;
  height: 400px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.chart-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--apple-dark-gray);
}

.chart {
  height: 320px;
}

.rank-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.rank-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--apple-light-gray);
}

.rank-item:last-child {
  border-bottom: none;
}

.rank-index {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--apple-light-gray);
  border-radius: 50%;
  font-weight: 600;
  color: var(--apple-gray);
}

.rank-index.top {
  background: linear-gradient(135deg, #ffd700, #ffb700);
  color: white;
}

.rank-name {
  flex: 1;
  color: var(--apple-dark-gray);
}

.rank-value {
  color: var(--apple-gray);
  font-size: 14px;
}

.todo-card {
  background: var(--apple-card-bg);
  border-radius: 12px;
  padding: 24px;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--apple-dark-gray);
  margin-bottom: 20px;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: var(--apple-bg);
  border-radius: 12px;
}

.message-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.message-icon.order { background: #007aff; }
.message-icon.warning { background: #ff9500; }
.message-icon.system { background: #34c759; }

.message-title {
  font-size: 14px;
  color: var(--apple-dark-gray);
  margin-bottom: 4px;
}

.message-time {
  font-size: 12px;
  color: var(--apple-gray);
}
</style>
