<template>
  <div class="log-view">
    <div class="page-header">
      <h2 class="page-title">操作日志</h2>
    </div>

    <!-- 筛选栏 -->
    <el-card class="filter-card">
      <el-form :model="filterForm" inline>
        <el-form-item label="操作人">
          <el-input v-model="filterForm.operator" placeholder="请输入操作人" clearable />
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select v-model="filterForm.type" placeholder="请选择类型" clearable>
            <el-option label="登录" value="login" />
            <el-option label="新增" value="create" />
            <el-option label="修改" value="update" />
            <el-option label="删除" value="delete" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作时间">
          <el-date-picker v-model="filterForm.dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 日志列表 -->
    <el-card>
      <el-table :data="logList" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="operator" label="操作人" width="120" align="center" />
        <el-table-column prop="type" label="操作类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getTypeType(row.type)">{{ getTypeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="module" label="操作模块" width="120" align="center" />
        <el-table-column prop="content" label="操作内容" min-width="300" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP地址" width="140" align="center" />
        <el-table-column prop="createTime" label="操作时间" width="160" align="center" />
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total" layout="total, prev, pager, next" @current-change="handlePageChange" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'

const loading = ref(false)
const filterForm = reactive({
  operator: '',
  type: '',
  dateRange: []
})
const page = ref(1)
const pageSize = ref(10)
const total = ref(100)

const logList = ref([
  {
    id: 1,
    operator: 'admin',
    type: 'login',
    module: '系统',
    content: '管理员登录系统',
    ip: '192.168.1.1',
    createTime: '2024-01-15 10:30:00'
  },
  {
    id: 2,
    operator: 'operator',
    type: 'update',
    module: '商品',
    content: '修改商品信息：红富士苹果',
    ip: '192.168.1.2',
    createTime: '2024-01-15 09:15:00'
  },
  {
    id: 3,
    operator: 'admin',
    type: 'create',
    module: '用户',
    content: '新增管理员：运营小李',
    ip: '192.168.1.1',
    createTime: '2024-01-14 16:20:00'
  }
])

const getTypeType = (type: string) => {
  const map: Record<string, string> = {
    login: 'info',
    create: 'success',
    update: 'warning',
    delete: 'danger'
  }
  return map[type] || 'info'
}

const getTypeText = (type: string) => {
  const map: Record<string, string> = {
    login: '登录',
    create: '新增',
    update: '修改',
    delete: '删除'
  }
  return map[type] || type
}

const handleSearch = () => {}
const resetFilter = () => {
  filterForm.operator = ''
  filterForm.type = ''
  filterForm.dateRange = []
}
const handlePageChange = (val: number) => {
  page.value = val
}
</script>

<style scoped>
.log-view {
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

.filter-card {
  margin-bottom: 20px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
