<template>
  <div class="income-view">
    <h2 class="page-title">收入统计</h2>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="8">
        <div class="stat-card primary">
          <p class="stat-label">今日收入</p>
          <p class="stat-value">¥{{ income.today }}</p>
          <p class="stat-compare">
            较昨日 {{ income.todayCompare > 0 ? '+' : '' }}{{ income.todayCompare }}%
          </p>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card success">
          <p class="stat-label">本月收入</p>
          <p class="stat-value">¥{{ income.month }}</p>
          <p class="stat-compare">
            较上月 {{ income.monthCompare > 0 ? '+' : '' }}{{ income.monthCompare }}%
          </p>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card warning">
          <p class="stat-label">累计收入</p>
          <p class="stat-value">¥{{ income.total }}</p>
          <p class="stat-compare">共 {{ income.orderCount }} 笔订单</p>
        </div>
      </el-col>
    </el-row>

    <!-- 趋势图 -->
    <el-card class="chart-card">
      <template #header>
        <div class="chart-header">
          <span>收入趋势</span>
          <el-radio-group v-model="timeRange" size="small">
            <el-radio-button label="week">本周</el-radio-button>
            <el-radio-button label="month">本月</el-radio-button>
            <el-radio-button label="year">本年</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <v-chart class="chart" :option="chartOption" autoresize />
    </el-card>

    <!-- 收入明细 -->
    <el-card class="detail-card">
      <template #header>
        <span>收入明细</span>
      </template>
      <el-table :data="incomeList" style="width: 100%" v-loading="loading">
        <el-table-column prop="date" label="日期" width="120" />
        <el-table-column prop="orderCount" label="订单数" width="100" />
        <el-table-column prop="orderAmount" label="订单金额" width="120">
          <template #default="{ row }">
            <span>¥{{ row.orderAmount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="freight" label="运费" width="100">
          <template #default="{ row }">
            <span>¥{{ row.freight }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="commission" label="平台佣金" width="100">
          <template #default="{ row }">
            <span style="color: #ff6b6b">-¥{{ row.commission }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="actualIncome" label="实际收入" width="120">
          <template #default="{ row }">
            <span style="color: #34c759; font-weight: 600">¥{{ row.actualIncome }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="结算状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'settled' ? 'success' : 'warning'" size="small">
              {{ row.status === 'settled' ? '已结算' : '待结算' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getFarmerStats, getFarmerIncome } from '@/api/farmer'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent } from 'echarts/components'
import VChart from 'vue-echarts'

// 注册 ECharts 组件
use([CanvasRenderer, BarChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent])

// 收入统计数据
const income = ref({
  today: 0,
  todayCompare: 0,
  month: 0,
  monthCompare: 0,
  total: 0,
  orderCount: 0
})

const timeRange = ref('week')
const loading = ref(false)

// 图表配置
const chartOption = computed(() => {
  const dates = incomeList.value.map(item => item.date).reverse()
  const actualIncomes = incomeList.value.map(item => item.actualIncome).reverse()
  const orderAmounts = incomeList.value.map(item => item.orderAmount).reverse()
  
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: function(params: any[]) {
        let result = `<div style="font-weight:600;margin-bottom:5px">${params[0].axisValue}</div>`
        params.forEach(param => {
          const color = param.color
          result += `<div style="display:flex;align-items:center;margin:3px 0">
            <span style="display:inline-block;width:10px;height:10px;background:${color};border-radius:50%;margin-right:8px"></span>
            <span style="flex:1">${param.seriesName}:</span>
            <span style="font-weight:600">¥${param.value}</span>
          </div>`
        })
        return result
      }
    },
    legend: {
      data: ['订单金额', '实际收入'],
      bottom: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: {
        lineStyle: {
          color: '#e0e0e0'
        }
      },
      axisLabel: {
        color: '#666',
        rotate: dates.length > 7 ? 45 : 0
      }
    },
    yAxis: {
      type: 'value',
      axisLine: {
        show: false
      },
      axisTick: {
        show: false
      },
      axisLabel: {
        color: '#666',
        formatter: (value: number) => {
          if (value >= 1000) {
            return '¥' + (value / 1000).toFixed(1) + 'k'
          }
          return '¥' + value
        }
      },
      splitLine: {
        lineStyle: {
          color: '#f0f0f0'
        }
      }
    },
    series: [
      {
        name: '订单金额',
        type: 'bar',
        data: orderAmounts,
        itemStyle: {
          color: '#667eea',
          borderRadius: [4, 4, 0, 0]
        },
        barWidth: '30%',
        label: {
          show: true,
          position: 'top',
          color: '#667eea',
          fontSize: 11,
          fontWeight: 'bold',
          formatter: (params: any) => {
            const value = params.value
            if (value >= 1000) {
              return (value / 1000).toFixed(1) + 'k'
            }
            return value > 0 ? value : ''
          }
        }
      },
      {
        name: '实际收入',
        type: 'bar',
        data: actualIncomes,
        itemStyle: {
          color: '#11998e',
          borderRadius: [4, 4, 0, 0]
        },
        barWidth: '30%',
        label: {
          show: true,
          position: 'top',
          color: '#11998e',
          fontSize: 11,
          fontWeight: 'bold',
          formatter: (params: any) => {
            const value = params.value
            if (value >= 1000) {
              return (value / 1000).toFixed(1) + 'k'
            }
            return value > 0 ? value : ''
          }
        }
      }
    ]
  }
})

// 收入明细列表
const incomeList = ref<any[]>([])

// 获取统计数据
const fetchStats = async () => {
  try {
    const res: any = await getFarmerStats()
    if (res) {
      income.value.today = res.todayIncome || 0
      income.value.month = res.monthIncome || 0
      income.value.total = res.totalIncome || 0
      income.value.orderCount = res.totalOrders || 0
      // 较昨日/较上月的数据需要从收入统计接口获取
      await fetchIncomeStats()
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
    ElMessage.error('获取统计数据失败')
  }
}

// 获取收入统计数据（包含对比数据）
const fetchIncomeStats = async () => {
  try {
    // 获取今日和昨日的对比
    const today = new Date()
    const yesterday = new Date(today)
    yesterday.setDate(yesterday.getDate() - 1)
    
    const todayStr = today.toISOString().split('T')[0]
    const yesterdayStr = yesterday.toISOString().split('T')[0]
    
    // 获取本月和上月的对比
    const thisMonth = today.toISOString().slice(0, 7) // YYYY-MM
    const lastMonthDate = new Date(today.getFullYear(), today.getMonth() - 1, 1)
    const lastMonth = lastMonthDate.toISOString().slice(0, 7)
    
    // 这里需要后端支持获取昨日和上月收入来计算对比
    // 暂时使用模拟的对比数据，后续可以完善
    income.value.todayCompare = 0
    income.value.monthCompare = 0
  } catch (error) {
    console.error('获取收入对比数据失败:', error)
  }
}

// 获取收入明细
const fetchIncomeList = async () => {
  loading.value = true
  try {
    // 根据时间范围计算日期
    const endDate = new Date().toISOString().split('T')[0]
    let startDate = endDate
    
    const today = new Date()
    if (timeRange.value === 'week') {
      const weekAgo = new Date(today)
      weekAgo.setDate(weekAgo.getDate() - 7)
      startDate = weekAgo.toISOString().split('T')[0]
    } else if (timeRange.value === 'month') {
      const monthAgo = new Date(today)
      monthAgo.setDate(1)
      startDate = monthAgo.toISOString().split('T')[0]
    } else if (timeRange.value === 'year') {
      startDate = `${today.getFullYear()}-01-01`
    }
    
    const res: any = await getFarmerIncome({ startDate, endDate })
    if (res && res.dailyStats) {
      incomeList.value = res.dailyStats.map((item: any) => ({
        date: item.date,
        orderCount: item.orderCount || 0,
        orderAmount: item.orderAmount || 0,
        freight: item.freight || 0,
        commission: item.commission || 0,
        actualIncome: item.actualIncome || 0,
        status: item.status || 'settled'
      }))
    } else {
      incomeList.value = []
    }
  } catch (error) {
    console.error('获取收入明细失败:', error)
    ElMessage.error('获取收入明细失败')
    incomeList.value = []
  } finally {
    loading.value = false
  }
}

// 监听时间范围变化
watch(timeRange, () => {
  fetchIncomeList()
})

onMounted(() => {
  fetchStats()
  fetchIncomeList()
})
</script>

<style scoped>
.income-view {
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
  border-radius: 12px;
  padding: 24px;
  color: white;
}

.stat-card.primary {
  background: linear-gradient(135deg, #667eea, #764ba2);
}

.stat-card.success {
  background: linear-gradient(135deg, #11998e, #38ef7d);
}

.stat-card.warning {
  background: linear-gradient(135deg, #f093fb, #f5576c);
}

.stat-label {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 8px;
}

.stat-compare {
  font-size: 13px;
  opacity: 0.8;
}

.chart-card {
  margin-bottom: 24px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart {
  height: 300px;
}

.detail-card {
  margin-bottom: 24px;
}
</style>
