<template>
  <div class="banner-view">
    <div class="page-header">
      <h2 class="page-title">轮播图管理</h2>
      <el-button type="primary" @click="openAddDialog">
        <el-icon><Plus /></el-icon>添加轮播图
      </el-button>
    </div>

    <el-card>
      <el-table :data="bannerList" v-loading="loading">
        <el-table-column label="轮播图" width="200">
          <template #default="{ row }">
            <img :src="getImageUrl(row.image)" class="banner-image" />
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="200" align="center" />
        <el-table-column prop="link" label="链接" min-width="250" align="center" />
        <el-table-column prop="sort" label="排序" width="80" align="center" />
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
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="editBanner(row)">编辑</el-button>
            <el-button link type="danger" @click="deleteBanner(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 添加/编辑弹窗 -->
    <el-dialog v-model="showAddDialog" :title="isEdit ? '编辑轮播图' : '添加轮播图'" width="600px">
      <el-form :model="bannerForm" label-width="100px">
        <el-form-item label="标题">
          <el-input v-model="bannerForm.title" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="图片">
          <el-upload 
            class="banner-uploader" 
            action="/api/file/upload" 
            :show-file-list="false" 
            :on-success="handleImageSuccess"
            :before-upload="beforeImageUpload"
          >
            <img v-if="bannerForm.image" :src="getImageUrl(bannerForm.image)" class="banner-preview" />
            <el-icon v-else class="banner-uploader-icon"><Plus /></el-icon>
          </el-upload>
          <div class="upload-tip">建议尺寸：750x300像素</div>
        </el-form-item>
        <el-form-item label="链接">
          <el-input v-model="bannerForm.link" placeholder="请输入链接地址" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="bannerForm.sort" :min="0" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="saveBanner" :loading="submitting">确定</el-button>
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

const loading = ref(false)
const submitting = ref(false)
const showAddDialog = ref(false)
const isEdit = ref(false)

const bannerForm = reactive({
  id: null as number | null,
  title: '',
  image: '',
  link: '',
  sort: 0,
  status: 1
})

const bannerList = ref<any[]>([])

// 获取轮播图列表
const fetchBannerList = async () => {
  loading.value = true
  try {
    const data = await request.get('/admin/banners') as any[]
    bannerList.value = data || []
  } catch (error) {
    console.error('获取轮播图列表失败:', error)
    ElMessage.error('获取轮播图列表失败')
  } finally {
    loading.value = false
  }
}

// 打开添加弹窗
const openAddDialog = () => {
  isEdit.value = false
  bannerForm.id = null
  bannerForm.title = ''
  bannerForm.image = ''
  bannerForm.link = ''
  bannerForm.sort = 0
  bannerForm.status = 1
  showAddDialog.value = true
}

// 上传前检查
const beforeImageUpload = (file: File) => {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB!')
    return false
  }
  return true
}

// 图片上传成功
const handleImageSuccess = (res: any) => {
  // 后端返回 Result 包装格式：{ code: 200, data: { url: "..." } }
  if (res.code === 200 && res.data && res.data.url) {
    bannerForm.image = res.data.url
    ElMessage.success('上传成功')
  } else {
    ElMessage.error('上传失败')
  }
}

// 编辑轮播图
const editBanner = (row: any) => {
  isEdit.value = true
  bannerForm.id = row.id
  bannerForm.title = row.title
  bannerForm.image = row.image
  bannerForm.link = row.link
  bannerForm.sort = row.sort
  bannerForm.status = row.status
  showAddDialog.value = true
}

// 删除轮播图
const deleteBanner = async (row: any) => {
  try {
    await ElMessageBox.confirm('确定要删除该轮播图吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await request.delete(`/admin/banners/${row.id}`)
    ElMessage.success('删除成功')
    fetchBannerList()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 保存轮播图
const saveBanner = async () => {
  if (!bannerForm.title) {
    ElMessage.warning('请输入标题')
    return
  }
  if (!bannerForm.image) {
    ElMessage.warning('请上传图片')
    return
  }

  submitting.value = true
  try {
    if (isEdit.value && bannerForm.id) {
      await request.put(`/admin/banners/${bannerForm.id}`, bannerForm)
    } else {
      await request.post('/admin/banners', bannerForm)
    }
    
    ElMessage.success(isEdit.value ? '修改成功' : '添加成功')
    showAddDialog.value = false
    fetchBannerList()
  } catch (error) {
    console.error(isEdit.value ? '修改失败:' : '添加失败:', error)
    ElMessage.error(isEdit.value ? '修改失败' : '添加失败')
  } finally {
    submitting.value = false
  }
}

// 状态变更
const handleStatusChange = async (row: any, status: number) => {
  try {
    await request.put(`/admin/banners/${row.id}/status`, { status })
    ElMessage.success('状态更新成功')
  } catch (error) {
    console.error('状态更新失败:', error)
    ElMessage.error('状态更新失败')
    // 恢复状态
    row.status = status === 1 ? 0 : 1
  }
}

onMounted(() => {
  fetchBannerList()
})
</script>

<style scoped>
.banner-view {
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

.banner-image {
  width: 160px;
  height: 80px;
  object-fit: cover;
  border-radius: 4px;
}

.banner-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 360px;
  height: 180px;
}

.banner-uploader:hover {
  border-color: #409eff;
}

.banner-preview {
  width: 360px;
  height: 180px;
  display: block;
  object-fit: cover;
}

.banner-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 360px;
  height: 180px;
  text-align: center;
  line-height: 180px;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}
</style>
