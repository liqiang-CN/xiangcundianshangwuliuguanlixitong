<template>
  <div class="product-list-view">
    <div class="page-header">
      <h2 class="page-title">商品管理</h2>
      <el-button type="primary" @click="handleAdd">+ 新增商品</el-button>
    </div>

    <!-- 搜索筛选 -->
    <el-card class="filter-card">
      <el-form :model="filterForm" inline class="filter-form">
        <el-form-item label="商品名称" class="filter-item">
          <el-input v-model="filterForm.name" placeholder="请输入商品名称" clearable class="filter-input" />
        </el-form-item>
        <el-form-item label="商品分类" class="filter-item">
          <el-select v-model="filterForm.categoryId" placeholder="请选择分类" clearable class="filter-select">
            <el-option v-for="cat in categoryList" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="商品状态" class="filter-item">
          <el-select v-model="filterForm.status" placeholder="请选择状态" clearable class="filter-select-sm">
            <el-option label="在售" :value="1" />
            <el-option label="下架" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item class="filter-item filter-buttons">
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 商品列表 -->
    <el-card>
      <el-table :data="productList" v-loading="loading">
        <el-table-column type="selection" width="55" />
        <el-table-column label="商品信息" min-width="300">
          <template #default="{ row }">
            <div class="product-info">
              <img :src="getImageUrl(row.mainImage || row.image)" class="product-image" />
              <div class="product-detail">
                <h4 class="product-name">{{ row.name }}</h4>
                <p class="product-category">{{ row.categoryName }}</p>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="price" label="价格" width="120" align="center">
          <template #default="{ row }">
            <span class="price">¥{{ row.price }}<span v-if="row.unit" class="unit">/{{ row.unit }}</span></span>
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="100" align="center" />
        <el-table-column prop="sales" label="销量" width="100" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="所属农户" width="120" align="center">
          <template #default="{ row }">
            <span class="farmer-link" @click="viewFarmerDetail(row.farmerId)">
              {{ row.farmerName }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link :type="row.status === 1 ? 'danger' : 'primary'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
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
    </el-card>

    <!-- 新增/编辑商品弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px">
      <el-form :model="productForm" :rules="productRules" ref="productFormRef" label-width="100px">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="productForm.name" placeholder="请输入商品名称" />
        </el-form-item>
        <el-form-item label="商品分类" prop="categoryId">
          <el-select v-model="productForm.categoryId" placeholder="请选择分类">
            <el-option v-for="cat in categoryList" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="商品价格" prop="price">
          <el-input-number v-model="productForm.price" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="productForm.unit" placeholder="如：斤、个、盒" maxlength="10" />
        </el-form-item>
        <el-form-item label="原价">
          <el-input-number v-model="productForm.originalPrice" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="productForm.stock" :min="0" :precision="0" />
        </el-form-item>
        <el-form-item label="商品图片" prop="images">
          <div class="image-upload-list">
            <!-- 已上传的图片列表 -->
            <div v-for="(img, index) in productImages" :key="index" class="image-preview-box">
              <img :src="getImageUrl(img)" class="preview-img" />
              <div class="image-actions">
                <el-button v-if="index !== 0" link type="primary" size="small" @click="setMainImage(index)">设为主图</el-button>
                <el-tag v-if="index === 0" type="success" size="small">主图</el-tag>
                <el-button link type="danger" size="small" @click="removeImage(index)">删除</el-button>
              </div>
            </div>
            <!-- 上传按钮（最多5张） -->
            <el-upload
              v-if="productImages.length < 5"
              class="avatar-uploader"
              action="/api/file/upload"
              :show-file-list="false"
              :on-success="handleImageSuccess"
              :on-error="handleUploadError"
              :before-upload="beforeImageUpload"
              accept="image/*"
              name="file"
            >
              <el-icon class="avatar-uploader-icon"><Plus /></el-icon>
            </el-upload>
          </div>
          <div class="upload-tip">最多上传5张图片，第一张自动设为主图</div>
        </el-form-item>
        <el-form-item label="商品描述">
          <el-input v-model="productForm.description" type="textarea" :rows="3" placeholder="请输入商品描述" />
        </el-form-item>
        
        <!-- 商品介绍 -->
        <el-form-item label="商品介绍">
          <div class="introduction-list">
            <div v-for="(item, index) in introductionList" :key="index" class="introduction-item">
              <div class="item-header">
                <span class="item-title">介绍 {{ index + 1 }}</span>
                <el-button link type="danger" @click="removeIntroduction(index)">
                  <el-icon><Delete /></el-icon>删除
                </el-button>
              </div>
              
              <!-- 文本+图片模式 -->
              <template v-if="item.type === 'text_image'">
                <el-form-item label="标题" class="intro-form-item">
                  <el-input v-model="item.title" placeholder="请输入标题（选填）" maxlength="100" show-word-limit />
                </el-form-item>
                <el-form-item label="文本内容" class="intro-form-item">
                  <el-input v-model="item.content" type="textarea" rows="3" placeholder="请输入介绍文本" maxlength="1000" show-word-limit />
                </el-form-item>
                <el-form-item label="图片" class="intro-form-item">
                  <div class="intro-image-upload">
                    <div v-if="item.imageUrl" class="image-preview-box">
                      <img :src="getImageUrl(item.imageUrl)" class="preview-img" />
                      <div class="image-actions">
                        <el-button link type="danger" size="small" @click="item.imageUrl = ''">删除</el-button>
                      </div>
                    </div>
                    <el-upload
                      v-else
                      class="avatar-uploader"
                      action="/api/file/upload"
                      :show-file-list="false"
                      :on-success="(res: any) => handleIntroImageSuccess(res, index)"
                      :on-error="handleUploadError"
                      :before-upload="beforeImageUpload"
                      accept="image/*"
                      name="file"
                    >
                      <el-icon class="avatar-uploader-icon"><Plus /></el-icon>
                    </el-upload>
                  </div>
                </el-form-item>
              </template>
              
              <!-- 仅图片模式 -->
              <template v-else>
                <el-form-item label="图片" class="intro-form-item">
                  <div class="intro-image-upload">
                    <div v-if="item.imageUrl" class="image-preview-box">
                      <img :src="getImageUrl(item.imageUrl)" class="preview-img" />
                      <div class="image-actions">
                        <el-button link type="danger" size="small" @click="item.imageUrl = ''">删除</el-button>
                      </div>
                    </div>
                    <el-upload
                      v-else
                      class="avatar-uploader"
                      action="/api/file/upload"
                      :show-file-list="false"
                      :on-success="(res: any) => handleIntroImageSuccess(res, index)"
                      :on-error="handleUploadError"
                      :before-upload="beforeImageUpload"
                      accept="image/*"
                      name="file"
                    >
                      <el-icon class="avatar-uploader-icon"><Plus /></el-icon>
                    </el-upload>
                  </div>
                </el-form-item>
              </template>
            </div>
          </div>
          
          <!-- 添加介绍按钮 -->
          <div class="add-intro-actions">
            <el-dropdown @command="addIntroduction">
              <el-button type="primary" plain>
                <el-icon><Plus /></el-icon>添加介绍
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="text_image">文本+图片</el-dropdown-item>
                  <el-dropdown-item command="image">仅图片</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-form-item>
        
        <el-form-item label="所属农户" prop="farmerId">
          <el-input-number v-model="productForm.farmerId" :min="1" :precision="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 农户详情弹窗 -->
    <el-dialog v-model="farmerDetailVisible" title="农户详情" width="500px">
      <el-descriptions :column="1" border v-if="currentFarmer">
        <el-descriptions-item label="农户ID">{{ currentFarmer.id }}</el-descriptions-item>
        <el-descriptions-item label="店铺名称">{{ currentFarmer.shopName || currentFarmer.nickname }}</el-descriptions-item>
        <el-descriptions-item label="店主名称">{{ currentFarmer.nickname }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ currentFarmer.username }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ currentFarmer.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ currentFarmer.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="所在地区">{{ currentFarmer.location || '-' }}</el-descriptions-item>
        <el-descriptions-item label="主营品类">{{ currentFarmer.mainCategory || '-' }}</el-descriptions-item>
        <el-descriptions-item label="店铺评分">{{ currentFarmer.rating || 5 }} 分</el-descriptions-item>
        <el-descriptions-item label="关注人数">{{ currentFarmer.followers || 0 }} 人</el-descriptions-item>
        <el-descriptions-item label="店铺状态">
          <el-tag :type="currentFarmer.status === 1 ? 'success' : 'info'">
            {{ currentFarmer.status === 1 ? '营业中' : '已关闭' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentFarmer.createTime || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import { getProductList, getProductDetail, createProduct, updateProduct, deleteProduct, toggleProductStatus, getCategories, type Product } from '@/api/product'
import { getShopDetail } from '@/api/shop'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增商品')
const isEdit = ref(false)
const currentId = ref<number>(0)

const filterForm = reactive({
  name: '',
  categoryId: undefined as number | undefined,
  status: undefined as number | undefined
})

const productForm = reactive({
  name: '',
  description: '',
  price: 0,
  unit: '',
  originalPrice: 0,
  stock: 0,
  mainImage: '',
  images: [] as string[],
  categoryId: undefined as number | undefined,
  farmerId: 1
})

const productImages = ref<string[]>([])

// 商品介绍列表
interface IntroductionItem {
  type: 'text_image' | 'image'
  title: string
  content: string
  imageUrl: string
}
const introductionList = ref<IntroductionItem[]>([])

// 添加商品介绍
const addIntroduction = (type: 'text_image' | 'image') => {
  introductionList.value.push({
    type,
    title: '',
    content: '',
    imageUrl: ''
  })
}

// 删除商品介绍
const removeIntroduction = (index: number) => {
  introductionList.value.splice(index, 1)
}

// 商品介绍图片上传成功
const handleIntroImageSuccess = (res: any, index: number) => {
  // 处理两种可能的响应格式
  let url = ''
  
  if (res && res.code === 200 && res.data && res.data.url) {
    url = res.data.url
  } else if (res && res.url) {
    url = res.url
  }
  
  if (url) {
    introductionList.value[index].imageUrl = url
    ElMessage.success('上传成功')
  } else {
    ElMessage.error('上传失败')
  }
}

const productRules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择商品分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入商品价格', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'blur' }],
  farmerId: [{ required: true, message: '请输入所属农户ID', trigger: 'blur' }]
}

const productFormRef = ref()

const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const productList = ref<Product[]>([])
const categoryList = ref<any[]>([])

// 农户详情弹窗
const farmerDetailVisible = ref(false)
const currentFarmer = ref<any>(null)

// 查看农户详情
const viewFarmerDetail = async (farmerId: number) => {
  if (!farmerId) {
    ElMessage.warning('该商品暂无农户信息')
    return
  }
  try {
    const res: any = await getShopDetail(farmerId)
    currentFarmer.value = res
    farmerDetailVisible.value = true
  } catch (error) {
    console.error('获取农户详情失败:', error)
    ElMessage.error('获取农户详情失败')
  }
}

// 获取分类列表
const fetchCategories = async () => {
  try {
    const res: any = await getCategories()
    categoryList.value = res || []
  } catch (error) {
    console.error('获取分类失败:', error)
  }
}

// 获取商品列表
const fetchProductList = async () => {
  loading.value = true
  try {
    const params: any = {
      page: page.value,
      size: pageSize.value,
      keyword: filterForm.name || undefined,
      categoryId: filterForm.categoryId
    }
    // 只有选择了具体状态时才传递 status 参数
    // 不传递 status 时后端返回所有状态的商品
    if (filterForm.status !== undefined) {
      params.status = filterForm.status
    }
    const res: any = await getProductList(params)
    productList.value = res || []
    // 如果有分页信息，更新总数
    if (res && res.total) {
      total.value = res.total
    } else {
      total.value = productList.value.length
    }
  } catch (error) {
    console.error('获取商品列表失败:', error)
    ElMessage.error('获取商品列表失败')
  } finally {
    loading.value = false
  }
}

const getStatusType = (status: number) => {
  const map: Record<number, string> = { 0: 'info', 1: 'success', 2: 'warning' }
  return map[status] || 'info'
}

const getStatusText = (status: number) => {
  const map: Record<number, string> = { 0: '已下架', 1: '在售', 2: '审核中' }
  return map[status] || '未知'
}

// 处理图片URL - 拼接完整路径
const getImageUrl = (url?: string) => {
  if (!url) return 'https://via.placeholder.com/60?text=No+Image'
  // 如果已经是完整URL，直接返回
  if (url.startsWith('http')) return url
  // 如果是 /images 开头的旧数据，使用默认图片
  if (url.startsWith('/images/')) {
    return 'https://via.placeholder.com/60?text=No+Image'
  }
  // /uploads/ 开头的路径，使用相对路径让代理处理
  if (url.startsWith('/uploads/')) {
    return url
  }
  return url
}

const handleSearch = () => {
  page.value = 1
  fetchProductList()
}

const resetFilter = () => {
  filterForm.name = ''
  filterForm.categoryId = undefined
  filterForm.status = undefined
  page.value = 1
  fetchProductList()
}

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增商品'
  currentId.value = 0
  Object.assign(productForm, {
    name: '',
    description: '',
    price: 0,
    originalPrice: 0,
    stock: 0,
    mainImage: '',
    images: [],
    categoryId: undefined,
    farmerId: 1
  })
  productImages.value = []
  introductionList.value = []
  dialogVisible.value = true
}

const handleEdit = async (row: Product) => {
  isEdit.value = true
  dialogTitle.value = '编辑商品'
  currentId.value = row.id
  
  // 调用详情接口获取完整数据（包含商品介绍）
  try {
    const detailRes: any = await getProductDetail(row.id)
    const detail = detailRes || row
    
    // 解析图片列表
    let otherImages: string[] = []
    if (detail.images) {
      try {
        otherImages = JSON.parse(detail.images)
      } catch {
        otherImages = []
      }
    }
    
    // 设置图片列表（主图放第一个）
    const mainImg = detail.mainImage || detail.image
    productImages.value = mainImg ? [mainImg, ...otherImages.filter((img: string) => img !== mainImg)] : otherImages
    
    Object.assign(productForm, {
      name: detail.name,
      description: detail.description,
      price: detail.price,
      unit: detail.unit || '',
      originalPrice: detail.originalPrice,
      stock: detail.stock,
      mainImage: mainImg,
      images: otherImages,
      categoryId: detail.categoryId,
      farmerId: detail.farmerId
    })
    
    // 加载商品介绍
    introductionList.value = []
    if (detail.introductionList && detail.introductionList.length > 0) {
      introductionList.value = detail.introductionList.map((item: any) => ({
        type: item.type,
        title: item.title || '',
        content: item.content || '',
        imageUrl: item.imageUrl || ''
      }))
    }
  } catch (error) {
    console.error('获取商品详情失败:', error)
    ElMessage.error('获取商品详情失败')
    
    // 使用列表数据作为后备
    let otherImages: string[] = []
    if (row.images) {
      try {
        otherImages = JSON.parse(row.images)
      } catch {
        otherImages = []
      }
    }
    const mainImg = row.mainImage || row.image
    productImages.value = mainImg ? [mainImg, ...otherImages.filter((img: string) => img !== mainImg)] : otherImages
    
    Object.assign(productForm, {
      name: row.name,
      description: row.description,
      price: row.price,
      unit: row.unit || '',
      originalPrice: row.originalPrice,
      stock: row.stock,
      mainImage: mainImg,
      images: otherImages,
      categoryId: row.categoryId,
      farmerId: row.farmerId
    })
    introductionList.value = []
  }
  
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await productFormRef.value?.validate()
  if (!valid) return

  // 检查是否有图片
  if (productImages.value.length === 0) {
    ElMessage.warning('请至少上传一张商品图片')
    return
  }

  submitting.value = true
  try {
    // 处理商品介绍数据
    const introductionData = introductionList.value
      .filter(item => {
        if (item.type === 'text_image') {
          return item.content.trim() || item.imageUrl
        }
        return item.imageUrl
      })
      .map(item => ({
        type: item.type,
        title: item.title,
        content: item.content,
        imageUrl: item.imageUrl
      }))
    
    const params = {
      name: productForm.name,
      description: productForm.description,
      price: productForm.price,
      originalPrice: productForm.originalPrice,
      stock: productForm.stock,
      mainImage: productImages.value[0] || '',
      images: productImages.value.length > 1 ? JSON.stringify(productImages.value.slice(1)) : '',
      categoryId: productForm.categoryId!,
      farmerId: productForm.farmerId,
      status: 1,
      introductionList: introductionData
    }
    if (isEdit.value) {
      await updateProduct(currentId.value, params)
      ElMessage.success('更新成功')
    } else {
      await createProduct(params)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchProductList()
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  } finally {
    submitting.value = false
  }
}

// 图片上传相关方法
const beforeImageUpload = (file: File) => {
  if (productImages.value.length >= 5) {
    ElMessage.warning('最多只能上传5张图片')
    return false
  }
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('请上传图片文件')
    return false
  }
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过5MB')
    return false
  }
  return true
}

const handleImageSuccess = (response: any) => {
  // 处理两种可能的响应格式
  // 格式1: { code: 200, data: { url: "..." } }
  // 格式2: { url: "..." }
  let url = ''
  
  if (response && response.code === 200 && response.data && response.data.url) {
    url = response.data.url
  } else if (response && response.url) {
    url = response.url
  }
  
  if (url) {
    productImages.value.push(url)
    ElMessage.success('图片上传成功')
  } else {
    ElMessage.error(response?.message || '上传失败')
  }
}

const handleUploadError = () => {
  ElMessage.error('图片上传失败，请重试')
}

const removeImage = (index: number) => {
  productImages.value.splice(index, 1)
}

const setMainImage = (index: number) => {
  if (index === 0) return
  const img = productImages.value.splice(index, 1)[0]
  productImages.value.unshift(img)
  ElMessage.success('已设为主图')
}

const toggleStatus = async (row: Product) => {
  const action = row.status === 1 ? '下架' : '上架'
  const newStatus = row.status === 1 ? 0 : 1
  ElMessageBox.confirm(`确定要${action}该商品吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await toggleProductStatus(row.id, newStatus)
      ElMessage.success(`${action}成功`)
      fetchProductList()
    } catch (error) {
      console.error('操作失败:', error)
      ElMessage.error('操作失败')
    }
  })
}

const handleDelete = (row: Product) => {
  ElMessageBox.confirm('确定要删除该商品吗？删除后不可恢复！', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteProduct(row.id)
      ElMessage.success('删除成功')
      fetchProductList()
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  })
}

const handlePageChange = (val: number) => {
  page.value = val
  fetchProductList()
}

onMounted(() => {
  fetchCategories()
  fetchProductList()
})
</script>

<style scoped>
.product-list-view {
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

.filter-card {
  margin-bottom: 20px;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-item {
  margin-bottom: 0;
  margin-right: 0;
}

.filter-input {
  width: 160px;
}

.filter-select {
  width: 140px;
}

.filter-select-sm {
  width: 120px;
}

.filter-buttons {
  margin-left: auto;
}

.product-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.product-image {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
}

.product-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.product-category {
  font-size: 12px;
  color: #909399;
}

.price {
  color: #f56c6c;
  font-weight: 600;
}

.unit {
  font-size: 12px;
  color: #909399;
  margin-left: 2px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.farmer-link {
  color: #303133;
  cursor: pointer;
}

.farmer-link:hover {
  color: #409eff;
}

/* 图片上传样式 */
.image-upload-list {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.image-preview-box {
  position: relative;
  width: 100px;
  height: 100px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  overflow: hidden;
}

.preview-img {
  width: 100%;
  height: 70px;
  object-fit: cover;
}

.image-actions {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 30px;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
}

.image-actions .el-button + .el-button {
  margin-left: 0;
}

.avatar-uploader {
  width: 100px;
  height: 100px;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
  transition: border-color 0.3s;
}

.avatar-uploader:hover {
  border-color: #409eff;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}

/* 商品介绍样式 */
.introduction-list {
  width: 100%;
}

.introduction-item {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
  background: #f5f7fa;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e4e7ed;
}

.item-title {
  font-weight: 600;
  color: #303133;
}

.intro-form-item {
  margin-bottom: 12px;
}

.intro-form-item :deep(.el-form-item__label) {
  font-weight: normal;
  color: #606266;
}

.intro-image-upload {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.add-intro-actions {
  margin-top: 12px;
}
</style>
