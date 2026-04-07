<template>
  <div class="category-view">
    <div class="page-header">
      <h2 class="page-title">分类管理</h2>
      <el-button type="primary" @click="openAddDialog">
        <el-icon><Plus /></el-icon>添加分类
      </el-button>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <p class="stat-label">总分类</p>
          <p class="stat-value">{{ stats.total }}</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <p class="stat-label">已启用</p>
          <p class="stat-value" style="color: #67c23a;">{{ stats.active }}</p>
        </el-card>
      </el-col>
    </el-row>

    <el-card>
      <el-table :data="categoryList" v-loading="loading">
        <el-table-column prop="name" label="分类名称" min-width="200" align="left" />
        <el-table-column prop="sort" label="排序" width="100" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch 
              v-model="row.status" 
              :active-value="1" 
              :inactive-value="0"
              @change="(val: number) => toggleStatus(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="productCount" label="商品数量" width="100" align="center">
          <template #default="{ row }">
            {{ row.productCount || 0 }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="editCategory(row)">编辑</el-button>
            <el-button link type="danger" @click="deleteCategory(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 添加/编辑分类弹窗 -->
    <el-dialog v-model="showDialog" :title="isEdit ? '编辑分类' : '添加分类'" width="500px">
      <el-form :model="categoryForm" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="categoryForm.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="categoryForm.sort" :min="0" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="saveCategory" :loading="saving">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import request from '../../api/request'

interface Category {
  id: number
  name: string
  sort: number
  status: number
  productCount?: number
}

interface Stats {
  total: number
  active: number
}

const loading = ref(false)
const saving = ref(false)
const showDialog = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const categoryForm = reactive({
  id: null as number | null,
  name: '',
  sort: 0,
  status: 1
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

const categoryList = ref<Category[]>([])
const stats = ref<Stats>({ total: 0, active: 0 })

// 获取分类列表
const fetchCategoryList = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/admin/category/list')
    categoryList.value = res || []
  } catch (error) {
    console.error('获取分类列表失败:', error)
    ElMessage.error('获取分类列表失败')
  } finally {
    loading.value = false
  }
}

// 获取统计信息
const fetchStats = async () => {
  try {
    const res: any = await request.get('/admin/category/stats')
    stats.value = res || { total: 0, active: 0 }
  } catch (error) {
    console.error('获取统计失败:', error)
  }
}

const openAddDialog = () => {
  isEdit.value = false
  categoryForm.id = null
  categoryForm.name = ''
  categoryForm.sort = categoryList.value.length + 1
  categoryForm.status = 1
  showDialog.value = true
}

const editCategory = (row: Category) => {
  isEdit.value = true
  categoryForm.id = row.id
  categoryForm.name = row.name
  categoryForm.sort = row.sort
  categoryForm.status = row.status
  showDialog.value = true
}

const toggleStatus = async (row: Category, val: number) => {
  try {
    await request.put(`/admin/category/${row.id}/status`, { status: val })
    ElMessage.success(val === 1 ? '已启用' : '已禁用')
    fetchStats()
  } catch (error) {
    console.error('切换状态失败:', error)
    ElMessage.error('操作失败')
    // 恢复原状态
    row.status = val === 1 ? 0 : 1
  }
}

const deleteCategory = (row: Category) => {
  ElMessageBox.confirm(`确定要删除分类 "${row.name}" 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await request.delete(`/admin/category/${row.id}`)
      ElMessage.success('删除成功')
      fetchCategoryList()
      fetchStats()
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  })
}

const saveCategory = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      saving.value = true
      
      try {
        if (isEdit.value && categoryForm.id) {
          await request.put(`/admin/category/${categoryForm.id}`, {
            name: categoryForm.name,
            sort: categoryForm.sort
          })
          ElMessage.success('修改成功')
          showDialog.value = false
          fetchCategoryList()
        } else {
          await request.post('/admin/category', {
            name: categoryForm.name,
            sort: categoryForm.sort,
            status: 1
          })
          ElMessage.success('添加成功')
          showDialog.value = false
          fetchCategoryList()
          fetchStats()
        }
      } catch (error) {
        console.error('保存失败:', error)
        ElMessage.error('保存失败')
      } finally {
        saving.value = false
      }
    }
  })
}

onMounted(() => {
  fetchCategoryList()
  fetchStats()
})
</script>

<style scoped>
.category-view {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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
  margin: 0 0 8px 0;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
</style>
