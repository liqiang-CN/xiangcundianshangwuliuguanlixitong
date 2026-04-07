<template>
  <div class="product-manage-view">
    <div class="page-header">
      <h2 class="page-title">商品管理</h2>
      <el-button type="primary" @click="$router.push('/farmer/product/add')">
        <el-icon><Plus /></el-icon>发布商品
      </el-button>
    </div>

    <!-- 搜索筛选 -->
    <div class="filter-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索商品名称"
        style="width: 240px"
        clearable
        @keyup.enter="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-select v-model="filterStatus" placeholder="商品状态" clearable style="width: 140px">
        <el-option label="全部" value="" />
        <el-option label="在售" :value="1" />
        <el-option label="下架" :value="0" />
      </el-select>
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="resetFilter">重置</el-button>
    </div>

    <!-- 商品列表 -->
    <el-table :data="productList" style="width: 100%" v-loading="loading">
      <el-table-column type="selection" width="55" />
      <el-table-column label="商品信息" min-width="300">
        <template #default="{ row }">
          <div class="product-info">
            <img :src="getImageUrl(row.mainImage || row.image)" class="product-image" />
            <div class="product-detail">
              <h4 class="product-name">{{ row.name }}</h4>
              <p class="product-category">{{ row.categoryName || '未分类' }}</p>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="price" label="价格" width="120">
        <template #default="{ row }">
          <span class="price">¥{{ row.price }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="stock" label="库存" width="100" />
      <el-table-column prop="sales" label="销量" width="100" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="editProduct(row)">编辑</el-button>
          <el-button link :type="row.status === 1 ? 'danger' : 'primary'" @click="toggleStatus(row)">
            {{ row.status === 1 ? '下架' : '上架' }}
          </el-button>
          <el-button link type="danger" @click="deleteProduct(row)">删除</el-button>
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { getFarmerProducts, deleteFarmerProduct, toggleFarmerProductStatus, type FarmerProductParams } from '@/api/farmer'
import { getImageUrl } from '@/utils/image'

const router = useRouter()
const loading = ref(false)
const searchKeyword = ref('')
const filterStatus = ref<number | ''>('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

interface Product {
  id: number
  name: string
  mainImage?: string
  image?: string
  categoryName?: string
  price: number
  stock: number
  sales: number
  status: number
}

const productList = ref<Product[]>([])

// 获取商品列表
const fetchProductList = async () => {
  loading.value = true
  try {
    const params: FarmerProductParams = {
      page: page.value,
      size: pageSize.value,
      keyword: searchKeyword.value || undefined,
      status: filterStatus.value !== '' ? filterStatus.value : undefined
    }
    const res: any = await getFarmerProducts(params)
    productList.value = res || []
    total.value = res?.length || 0
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

const handleSearch = () => {
  page.value = 1
  fetchProductList()
}

const resetFilter = () => {
  searchKeyword.value = ''
  filterStatus.value = ''
  page.value = 1
  fetchProductList()
}

const editProduct = (row: Product) => {
  router.push(`/farmer/product/edit/${row.id}`)
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
      await toggleFarmerProductStatus(row.id, newStatus)
      ElMessage.success(`${action}成功`)
      fetchProductList()
    } catch (error) {
      console.error('操作失败:', error)
      ElMessage.error('操作失败')
    }
  })
}

const deleteProduct = (row: Product) => {
  ElMessageBox.confirm('确定要删除该商品吗？删除后不可恢复！', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteFarmerProduct(row.id)
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
  fetchProductList()
})
</script>

<style scoped>
.product-manage-view {
  padding-bottom: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--apple-dark-gray);
}

.filter-bar {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
  padding: 20px;
  background: var(--apple-card-bg);
  border-radius: 12px;
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
  border-radius: 8px;
}

.product-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--apple-dark-gray);
  margin-bottom: 4px;
}

.product-category {
  font-size: 12px;
  color: var(--apple-gray);
}

.price {
  color: var(--apple-orange);
  font-weight: 600;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
}
</style>
