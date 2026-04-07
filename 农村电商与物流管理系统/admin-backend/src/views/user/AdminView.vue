<template>
  <div class="admin-view">
    <div class="page-header">
      <h2 class="page-title">管理员管理</h2>
      <el-button type="primary" @click="showAddDialog = true">
        <el-icon><Plus /></el-icon>添加管理员
      </el-button>
    </div>

    <el-card>
      <el-table :data="adminList" v-loading="loading">
        <el-table-column label="管理员信息" min-width="250">
          <template #default="{ row }">
            <div class="admin-info">
              <el-avatar :size="50" :src="getImageUrl(row.avatar)" />
              <div class="admin-detail">
                <p class="admin-name">{{ row.nickname || row.username }}</p>
                <p class="admin-username">{{ row.username }}</p>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="role" label="角色" width="120" align="center">
          <template #default="{ row }">
            {{ getRoleText(row.role) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="(val: any) => handleStatusChange(row, val as number)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录" width="160" align="center">
          <template #default="{ row }">
            {{ row.lastLoginTime || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="editAdmin(row)">编辑</el-button>
            <el-button link type="primary" @click="setPermission(row)">权限</el-button>
            <el-button link type="danger" @click="deleteAdmin(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 添加/编辑管理员弹窗 -->
    <el-dialog v-model="showAddDialog" :title="isEdit ? '编辑管理员' : '添加管理员'" width="500px">
      <el-form :model="adminForm" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="adminForm.username" placeholder="请输入用户名" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="adminForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!isEdit">
          <el-input v-model="adminForm.password" type="password" placeholder="请输入密码，默认123456" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="adminForm.role" placeholder="请选择角色" style="width: 100%">
            <el-option label="超级管理员" value="super" />
            <el-option label="运营管理员" value="operator" />
            <el-option label="客服" value="service" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="saveAdmin" :loading="saving">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '@/api/request'
import { getImageUrl } from '@/utils/image'
import type { FormInstance, FormRules } from 'element-plus'

const loading = ref(false)
const saving = ref(false)
const showAddDialog = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const adminForm = reactive({
  id: null as number | null,
  username: '',
  nickname: '',
  password: '',
  role: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const adminList = ref<any[]>([])

// 获取角色文本
const getRoleText = (role: string) => {
  const roleMap: Record<string, string> = {
    'super': '超级管理员',
    'operator': '运营管理员',
    'service': '客服'
  }
  return roleMap[role] || role
}

// 获取管理员列表
const fetchAdminList = async () => {
  loading.value = true
  try {
    const data = await request.get('/admin/admins') as any[]
    adminList.value = data || []
  } catch (error) {
    console.error('获取管理员列表失败:', error)
    ElMessage.error('获取管理员列表失败')
  } finally {
    loading.value = false
  }
}

// 状态切换
const handleStatusChange = async (row: any, val: number) => {
  try {
    await request.put(`/admin/admins/${row.id}/status`, { status: val })
    ElMessage.success('状态更新成功')
  } catch (error) {
    row.status = val === 1 ? 0 : 1
    ElMessage.error('状态更新失败')
  }
}

const editAdmin = (row: any) => {
  isEdit.value = true
  adminForm.id = row.id
  adminForm.username = row.username
  adminForm.nickname = row.nickname
  adminForm.role = row.role
  showAddDialog.value = true
}

const setPermission = (row: any) => {
  ElMessage.info(`设置${row.nickname || row.username}的权限`)
}

const deleteAdmin = (row: any) => {
  ElMessageBox.confirm(`确定要删除管理员"${row.nickname || row.username}"吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await request.delete(`/admin/admins/${row.id}`)
      ElMessage.success('删除成功')
      fetchAdminList()
    } catch (error) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

const saveAdmin = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      saving.value = true
      try {
        if (isEdit.value) {
          await request.put(`/admin/admins/${adminForm.id}`, {
            username: adminForm.username,
            nickname: adminForm.nickname,
            role: adminForm.role
          })
          ElMessage.success('修改成功')
        } else {
          await request.post('/admin/admins', {
            username: adminForm.username,
            nickname: adminForm.nickname,
            password: adminForm.password,
            role: adminForm.role
          })
          ElMessage.success('添加成功')
        }
        showAddDialog.value = false
        resetForm()
        fetchAdminList()
      } catch (error: any) {
        ElMessage.error(error.message || '操作失败')
      } finally {
        saving.value = false
      }
    }
  })
}

const resetForm = () => {
  adminForm.id = null
  adminForm.username = ''
  adminForm.nickname = ''
  adminForm.password = ''
  adminForm.role = ''
  isEdit.value = false
  formRef.value?.resetFields()
}

onMounted(() => {
  fetchAdminList()
})
</script>

<style scoped>
.admin-view {
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

.admin-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-name {
  font-weight: 500;
  color: #303133;
}

.admin-username {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
