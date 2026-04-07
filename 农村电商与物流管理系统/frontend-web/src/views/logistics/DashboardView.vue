<template>
  <div class="logistics-dashboard">
    <h2 class="page-title">工作台</h2>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon blue">
            <el-icon size="32"><Document /></el-icon>
          </div>
          <div class="stat-info">
            <p class="stat-label">待揽件</p>
            <p class="stat-value">{{ stats.pendingPickup }}</p>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon orange">
            <el-icon size="32"><Van /></el-icon>
          </div>
          <div class="stat-info">
            <p class="stat-label">配送中</p>
            <p class="stat-value">{{ stats.delivering }}</p>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon green">
            <el-icon size="32"><CircleCheck /></el-icon>
          </div>
          <div class="stat-info">
            <p class="stat-label">今日完成</p>
            <p class="stat-value">{{ stats.completed }}</p>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon purple">
            <el-icon size="32"><TrendCharts /></el-icon>
          </div>
          <div class="stat-info">
            <p class="stat-label">配送准时率</p>
            <p class="stat-value">{{ stats.onTimeRate }}%</p>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 今日任务 -->
    <el-row :gutter="16">
      <el-col :span="16">
        <div class="task-card">
          <div class="card-header">
            <h3 class="card-title">今日配送任务</h3>
            <el-button link type="primary" @click="$router.push('/logistics/waybills')">查看全部</el-button>
          </div>
          <el-timeline v-if="todayTasks.length > 0">
            <el-timeline-item
              v-for="task in todayTasks"
              :key="task.id"
              :type="task.type"
              :timestamp="task.time"
            >
              <div class="task-item">
                <div class="task-info">
                  <h4 class="task-title">{{ task.title }}</h4>
                  <p class="task-address">{{ task.address }}</p>
                  <p class="task-waybill">运单号：{{ task.waybillNo }}</p>
                </div>
                <el-tag :type="getTaskStatusType(task.status)">{{ task.statusText }}</el-tag>
              </div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无今日任务" />
        </div>
      </el-col>
      <el-col :span="8">
        <div class="quick-actions">
          <h3 class="card-title">快捷操作</h3>
          <div class="action-grid">
            <div class="action-item" @click="$router.push('/logistics/waybills')">
              <el-icon size="32"><Document /></el-icon>
              <span>运单管理</span>
            </div>
            <div class="action-item" @click="$router.push('/logistics/print')">
              <el-icon size="32"><Printer /></el-icon>
              <span>打印面单</span>
            </div>
            <div class="action-item" @click="$router.push('/logistics/route')">
              <el-icon size="32"><MapLocation /></el-icon>
              <span>路线规划</span>
            </div>
            <div class="action-item" @click="$router.push('/logistics/records')">
              <el-icon size="32"><List /></el-icon>
              <span>配送记录</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Document, Van, CircleCheck, TrendCharts, Printer, MapLocation, List } from '@element-plus/icons-vue'
import request from '@/api/request'
import { ElMessage } from 'element-plus'

const stats = ref({
  pendingPickup: 0,
  delivering: 0,
  completed: 0,
  onTimeRate: 98.5
})

const todayTasks = ref<any[]>([])

// 获取统计数据
const fetchStats = async () => {
  try {
    const res: any = await request.get('/logistics/stats')
    if (res) {
      stats.value = {
        pendingPickup: res.pendingPickup || 0,
        delivering: res.delivering || 0,
        completed: res.completed || 0,
        onTimeRate: res.onTimeRate || 98.5
      }
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
    ElMessage.error('获取统计数据失败')
  }
}

// 获取今日任务
const fetchTodayTasks = async () => {
  try {
    const res: any = await request.get('/logistics/today-tasks')
    if (res && Array.isArray(res)) {
      todayTasks.value = res
    }
  } catch (error) {
    console.error('获取今日任务失败:', error)
    ElMessage.error('获取今日任务失败')
  }
}

const getTaskStatusType = (status: string) => {
  const map: Record<string, string> = {
    pending: 'warning',
    transit: 'primary',
    delivering: 'primary',
    completed: 'success'
  }
  return map[status] || 'info'
}

onMounted(() => {
  fetchStats()
  fetchTodayTasks()
})
</script>

<style scoped>
.logistics-dashboard {
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
.stat-icon.orange { background: linear-gradient(135deg, #f093fb, #f5576c); }
.stat-icon.green { background: linear-gradient(135deg, #11998e, #38ef7d); }
.stat-icon.purple { background: linear-gradient(135deg, #4facfe, #00f2fe); }

.stat-info {
  flex: 1;
}

.stat-label {
  font-size: 14px;
  color: var(--apple-gray);
  margin: 0 0 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--apple-dark-gray);
  margin: 0;
}

.task-card {
  background: var(--apple-card-bg);
  border-radius: 12px;
  padding: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--apple-dark-gray);
  margin: 0;
}

.task-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.task-info {
  flex: 1;
}

.task-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--apple-dark-gray);
  margin: 0 0 4px;
}

.task-address {
  font-size: 13px;
  color: var(--apple-gray);
  margin: 0 0 4px;
}

.task-waybill {
  font-size: 12px;
  color: var(--apple-light-gray);
  margin: 0;
}

.quick-actions {
  background: var(--apple-card-bg);
  border-radius: 12px;
  padding: 24px;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-top: 16px;
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px;
  background: var(--apple-bg);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.action-item:hover {
  background: var(--apple-border);
}

.action-item span {
  font-size: 13px;
  color: var(--apple-dark-gray);
}
</style>
