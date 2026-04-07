<template>
  <div class="product-list-view">
    <div class="filter-section">
      <div class="search-box">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索商品名称"
          clearable
          size="large"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
          <template #append>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
          </template>
        </el-input>
      </div>
      <div class="filter-container">
        <div class="filter-row">
          <span class="filter-label">分类：</span>
          <div class="filter-options">
            <span 
              v-for="cat in categories" 
              :key="cat.id"
              class="filter-option"
              :class="{ active: queryParams.categoryId === cat.id }"
              @click="selectCategory(cat.id)"
            >
              {{ cat.name }}
            </span>
          </div>
        </div>
        <div class="filter-row">
          <span class="filter-label">价格：</span>
          <div class="filter-options">
            <span 
              v-for="price in priceRanges" 
              :key="price.value"
              class="filter-option"
              :class="{ active: selectedPrice === price.value }"
              @click="selectPrice(price.value)"
            >
              {{ price.label }}
            </span>
          </div>
        </div>
        <div class="filter-row">
          <span class="filter-label">产地：</span>
          <div class="filter-options origin-filter">
            <span 
              class="filter-option"
              :class="{ active: !queryParams.origin }"
              @click="selectOrigin('')"
            >
              全部
            </span>
            <el-cascader
              v-model="originSelected"
              :options="regionOptions"
              :props="{ value: 'label', label: 'label', checkStrictly: true }"
              placeholder="选择产地"
              clearable
              style="width: 200px"
              @change="handleOriginChange"
            />
          </div>
        </div>
      </div>
    </div>

    <div class="list-section">
      <div class="sort-bar">
        <div class="sort-options">
          <span 
            v-for="sort in sortOptions" 
            :key="sort.value"
            class="sort-option"
            :class="{ active: queryParams.sortBy === sort.value }"
            @click="selectSort(sort.value)"
          >
            {{ sort.label }}
            <el-icon v-if="queryParams.sortBy === sort.value"><ArrowUp v-if="queryParams.sortOrder === 'asc'" /><ArrowDown v-else /></el-icon>
          </span>
        </div>
        <span class="result-count">共 {{ total }} 件商品</span>
      </div>

      <div class="product-grid">
        <div v-for="product in products" :key="product.id" class="product-card" @click="goToDetail(product.id)">
          <div class="product-image">
            <img :src="getImageUrl(product.mainImage || product.image)" :alt="product.name" />
          </div>
          <div class="product-info">
            <h4 class="product-name">{{ product.name }}</h4>
            <p class="product-origin">
              <el-icon><Location /></el-icon>
              {{ product.origin }}
            </p>
            <div class="product-footer">
              <span class="product-price">¥{{ product.price }}</span>
              <span class="product-sales">已售 {{ product.sales }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="queryParams.page"
          v-model:page-size="queryParams.size"
          :total="total"
          :page-sizes="[12, 24, 36, 48]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ArrowUp, ArrowDown, Location, Search } from '@element-plus/icons-vue'
import { getImageUrl } from '@/utils/image'
import { productApi } from '@/api/product'

const router = useRouter()
const route = useRoute()

const categories = ref<{ id: number; name: string }[]>([{ id: 0, name: '全部' }])

// 获取分类列表
const fetchCategories = async () => {
  try {
    const data = await productApi.getCategories()
    const list = Array.isArray(data) ? data : []
    // 添加"全部"选项到开头
    categories.value = [{ id: 0, name: '全部' }, ...list.map((cat: any) => ({
      id: Number(cat.id),
      name: String(cat.name)
    }))]
  } catch (error) {
    console.error('获取分类失败:', error)
  }
}

const priceRanges = [
  { label: '全部', value: '' },
  { label: '0-20元', value: '0-20' },
  { label: '20-50元', value: '20-50' },
  { label: '50-100元', value: '50-100' },
  { label: '100元以上', value: '100-' }
]

const originSelected = ref<string[]>([]) // 产地级联选择

// 省市区数据
const regionOptions = [
  {
    label: '北京',
    children: [{ label: '北京市' }]
  },
  {
    label: '天津',
    children: [{ label: '天津市' }]
  },
  {
    label: '河北',
    children: [
      { label: '石家庄' }, { label: '唐山' }, { label: '秦皇岛' },
      { label: '邯郸' }, { label: '邢台' }, { label: '保定' },
      { label: '张家口' }, { label: '承德' }, { label: '沧州' },
      { label: '廊坊' }, { label: '衡水' }
    ]
  },
  {
    label: '山西',
    children: [
      { label: '太原' }, { label: '大同' }, { label: '阳泉' },
      { label: '长治' }, { label: '晋城' }, { label: '朔州' },
      { label: '晋中' }, { label: '运城' }, { label: '忻州' },
      { label: '临汾' }, { label: '吕梁' }
    ]
  },
  {
    label: '内蒙古',
    children: [
      { label: '呼和浩特' }, { label: '包头' }, { label: '乌海' },
      { label: '赤峰' }, { label: '通辽' }, { label: '鄂尔多斯' },
      { label: '呼伦贝尔' }, { label: '巴彦淖尔' }, { label: '乌兰察布' },
      { label: '兴安盟' }, { label: '锡林郭勒盟' }, { label: '阿拉善盟' }
    ]
  },
  {
    label: '辽宁',
    children: [
      { label: '沈阳' }, { label: '大连' }, { label: '鞍山' },
      { label: '抚顺' }, { label: '本溪' }, { label: '丹东' },
      { label: '锦州' }, { label: '营口' }, { label: '阜新' },
      { label: '辽阳' }, { label: '盘锦' }, { label: '铁岭' },
      { label: '朝阳' }, { label: '葫芦岛' }
    ]
  },
  {
    label: '吉林',
    children: [
      { label: '长春' }, { label: '吉林' }, { label: '四平' },
      { label: '辽源' }, { label: '通化' }, { label: '白山' },
      { label: '松原' }, { label: '白城' }, { label: '延边' }
    ]
  },
  {
    label: '黑龙江',
    children: [
      { label: '哈尔滨' }, { label: '齐齐哈尔' }, { label: '鸡西' },
      { label: '鹤岗' }, { label: '双鸭山' }, { label: '大庆' },
      { label: '伊春' }, { label: '佳木斯' }, { label: '七台河' },
      { label: '牡丹江' }, { label: '黑河' }, { label: '绥化' },
      { label: '大兴安岭' }
    ]
  },
  {
    label: '上海',
    children: [{ label: '上海市' }]
  },
  {
    label: '江苏',
    children: [
      { label: '南京' }, { label: '无锡' }, { label: '徐州' },
      { label: '常州' }, { label: '苏州' }, { label: '南通' },
      { label: '连云港' }, { label: '淮安' }, { label: '盐城' },
      { label: '扬州' }, { label: '镇江' }, { label: '泰州' },
      { label: '宿迁' }
    ]
  },
  {
    label: '浙江',
    children: [
      { label: '杭州' }, { label: '宁波' }, { label: '温州' },
      { label: '嘉兴' }, { label: '湖州' }, { label: '绍兴' },
      { label: '金华' }, { label: '衢州' }, { label: '舟山' },
      { label: '台州' }, { label: '丽水' }
    ]
  },
  {
    label: '安徽',
    children: [
      { label: '合肥' }, { label: '芜湖' }, { label: '蚌埠' },
      { label: '淮南' }, { label: '马鞍山' }, { label: '淮北' },
      { label: '铜陵' }, { label: '安庆' }, { label: '黄山' },
      { label: '滁州' }, { label: '阜阳' }, { label: '宿州' },
      { label: '六安' }, { label: '亳州' }, { label: '池州' },
      { label: '宣城' }
    ]
  },
  {
    label: '福建',
    children: [
      { label: '福州' }, { label: '厦门' }, { label: '莆田' },
      { label: '三明' }, { label: '泉州' }, { label: '漳州' },
      { label: '南平' }, { label: '龙岩' }, { label: '宁德' }
    ]
  },
  {
    label: '江西',
    children: [
      { label: '南昌' }, { label: '景德镇' }, { label: '萍乡' },
      { label: '九江' }, { label: '新余' }, { label: '鹰潭' },
      { label: '赣州' }, { label: '吉安' }, { label: '宜春' },
      { label: '抚州' }, { label: '上饶' }
    ]
  },
  {
    label: '山东',
    children: [
      { label: '济南' }, { label: '青岛' }, { label: '淄博' },
      { label: '枣庄' }, { label: '东营' }, { label: '烟台' },
      { label: '潍坊' }, { label: '济宁' }, { label: '泰安' },
      { label: '威海' }, { label: '日照' }, { label: '莱芜' },
      { label: '临沂' }, { label: '德州' }, { label: '聊城' },
      { label: '滨州' }, { label: '菏泽' }
    ]
  },
  {
    label: '河南',
    children: [
      { label: '郑州' }, { label: '开封' }, { label: '洛阳' },
      { label: '平顶山' }, { label: '安阳' }, { label: '鹤壁' },
      { label: '新乡' }, { label: '焦作' }, { label: '濮阳' },
      { label: '许昌' }, { label: '漯河' }, { label: '三门峡' },
      { label: '南阳' }, { label: '商丘' }, { label: '信阳' },
      { label: '周口' }, { label: '驻马店' }
    ]
  },
  {
    label: '湖北',
    children: [
      { label: '武汉' }, { label: '黄石' }, { label: '十堰' },
      { label: '宜昌' }, { label: '襄阳' }, { label: '鄂州' },
      { label: '荆门' }, { label: '孝感' }, { label: '荆州' },
      { label: '黄冈' }, { label: '咸宁' }, { label: '随州' },
      { label: '恩施' }
    ]
  },
  {
    label: '湖南',
    children: [
      { label: '长沙' }, { label: '株洲' }, { label: '湘潭' },
      { label: '衡阳' }, { label: '邵阳' }, { label: '岳阳' },
      { label: '常德' }, { label: '张家界' }, { label: '益阳' },
      { label: '郴州' }, { label: '永州' }, { label: '怀化' },
      { label: '娄底' }, { label: '湘西' }
    ]
  },
  {
    label: '广东',
    children: [
      { label: '广州' }, { label: '韶关' }, { label: '深圳' },
      { label: '珠海' }, { label: '汕头' }, { label: '佛山' },
      { label: '江门' }, { label: '湛江' }, { label: '茂名' },
      { label: '肇庆' }, { label: '惠州' }, { label: '梅州' },
      { label: '汕尾' }, { label: '河源' }, { label: '阳江' },
      { label: '清远' }, { label: '东莞' }, { label: '中山' },
      { label: '潮州' }, { label: '揭阳' }, { label: '云浮' }
    ]
  },
  {
    label: '广西',
    children: [
      { label: '南宁' }, { label: '柳州' }, { label: '桂林' },
      { label: '梧州' }, { label: '北海' }, { label: '防城港' },
      { label: '钦州' }, { label: '贵港' }, { label: '玉林' },
      { label: '百色' }, { label: '贺州' }, { label: '河池' },
      { label: '来宾' }, { label: '崇左' }
    ]
  },
  {
    label: '海南',
    children: [
      { label: '海口' }, { label: '三亚' }, { label: '三沙' },
      { label: '儋州' }
    ]
  },
  {
    label: '重庆',
    children: [{ label: '重庆市' }]
  },
  {
    label: '四川',
    children: [
      { label: '成都' }, { label: '自贡' }, { label: '攀枝花' },
      { label: '泸州' }, { label: '德阳' }, { label: '绵阳' },
      { label: '广元' }, { label: '遂宁' }, { label: '内江' },
      { label: '乐山' }, { label: '南充' }, { label: '眉山' },
      { label: '宜宾' }, { label: '广安' }, { label: '达州' },
      { label: '雅安' }, { label: '巴中' }, { label: '资阳' },
      { label: '阿坝' }, { label: '甘孜' }, { label: '凉山' }
    ]
  },
  {
    label: '贵州',
    children: [
      { label: '贵阳' }, { label: '六盘水' }, { label: '遵义' },
      { label: '安顺' }, { label: '毕节' }, { label: '铜仁' },
      { label: '黔西南' }, { label: '黔东南' }, { label: '黔南' }
    ]
  },
  {
    label: '云南',
    children: [
      { label: '昆明' }, { label: '曲靖' }, { label: '玉溪' },
      { label: '保山' }, { label: '昭通' }, { label: '丽江' },
      { label: '普洱' }, { label: '临沧' }, { label: '楚雄' },
      { label: '红河' }, { label: '文山' }, { label: '西双版纳' },
      { label: '大理' }, { label: '德宏' }, { label: '怒江' },
      { label: '迪庆' }
    ]
  },
  {
    label: '西藏',
    children: [
      { label: '拉萨' }, { label: '日喀则' }, { label: '昌都' },
      { label: '林芝' }, { label: '山南' }, { label: '那曲' },
      { label: '阿里' }
    ]
  },
  {
    label: '陕西',
    children: [
      { label: '西安' }, { label: '铜川' }, { label: '宝鸡' },
      { label: '咸阳' }, { label: '渭南' }, { label: '延安' },
      { label: '汉中' }, { label: '榆林' }, { label: '安康' },
      { label: '商洛' }
    ]
  },
  {
    label: '甘肃',
    children: [
      { label: '兰州' }, { label: '嘉峪关' }, { label: '金昌' },
      { label: '白银' }, { label: '天水' }, { label: '武威' },
      { label: '张掖' }, { label: '平凉' }, { label: '酒泉' },
      { label: '庆阳' }, { label: '定西' }, { label: '陇南' },
      { label: '临夏' }, { label: '甘南' }
    ]
  },
  {
    label: '青海',
    children: [
      { label: '西宁' }, { label: '海东' }, { label: '海北' },
      { label: '黄南' }, { label: '海南' }, { label: '果洛' },
      { label: '玉树' }, { label: '海西' }
    ]
  },
  {
    label: '宁夏',
    children: [
      { label: '银川' }, { label: '石嘴山' }, { label: '吴忠' },
      { label: '固原' }, { label: '中卫' }
    ]
  },
  {
    label: '新疆',
    children: [
      { label: '乌鲁木齐' }, { label: '克拉玛依' }, { label: '吐鲁番' },
      { label: '哈密' }, { label: '昌吉' }, { label: '博尔塔拉' },
      { label: '巴音郭楞' }, { label: '阿克苏' }, { label: '克孜勒苏' },
      { label: '喀什' }, { label: '和田' }, { label: '伊犁' },
      { label: '塔城' }, { label: '阿勒泰' }
    ]
  },
  {
    label: '台湾',
    children: [{ label: '台湾' }]
  },
  {
    label: '香港',
    children: [{ label: '香港' }]
  },
  {
    label: '澳门',
    children: [{ label: '澳门' }]
  }
]

const sortOptions = [
  { label: '综合', value: 'default' },
  { label: '销量', value: 'sales' },
  { label: '价格', value: 'price' },
  { label: '新品', value: 'new' }
]

const queryParams = reactive({
  page: 1,
  size: 12,
  categoryId: 0,
  keyword: '',
  minPrice: undefined as number | undefined,
  maxPrice: undefined as number | undefined,
  origin: '',
  sortBy: 'default',
  sortOrder: 'desc'
})

const selectedPrice = ref('')
const products = ref<any[]>([])
const total = ref(0)
const searchKeyword = ref('') // 搜索关键词

const selectCategory = (id: number) => {
  queryParams.categoryId = id
  queryParams.page = 1
  fetchProducts()
}

const selectPrice = (value: string) => {
  selectedPrice.value = value
  if (value) {
    const [min, max] = value.split('-')
    queryParams.minPrice = min ? Number(min) : undefined
    queryParams.maxPrice = max ? Number(max) : undefined
  } else {
    queryParams.minPrice = undefined
    queryParams.maxPrice = undefined
  }
  queryParams.page = 1
  fetchProducts()
}

const selectOrigin = (origin: string) => {
  queryParams.origin = origin
  originSelected.value = []
  queryParams.page = 1
  fetchProducts()
}

// 处理级联选择器变化
const handleOriginChange = (value: string[]) => {
  if (value && value.length > 0) {
    // 使用最后一个选中的值作为筛选条件
    queryParams.origin = value[value.length - 1]
  } else {
    queryParams.origin = ''
  }
  queryParams.page = 1
  fetchProducts()
}

// 处理搜索
const handleSearch = () => {
  queryParams.keyword = searchKeyword.value
  queryParams.page = 1
  fetchProducts()
}

const selectSort = (value: string) => {
  if (queryParams.sortBy === value) {
    queryParams.sortOrder = queryParams.sortOrder === 'asc' ? 'desc' : 'asc'
  } else {
    queryParams.sortBy = value
    queryParams.sortOrder = 'desc'
  }
  fetchProducts()
}

const handleSizeChange = (size: number) => {
  queryParams.size = size
  fetchProducts()
}

const handlePageChange = (page: number) => {
  queryParams.page = page
  fetchProducts()
}

const fetchProducts = async () => {
  try {
    // 构建请求参数
    const params: any = {
      page: queryParams.page,
      size: queryParams.size,
      keyword: queryParams.keyword || undefined
    }
    // 分类筛选
    if (queryParams.categoryId && queryParams.categoryId !== 0) {
      params.categoryId = queryParams.categoryId
    }
    // 价格筛选
    if (queryParams.minPrice !== undefined) {
      params.minPrice = queryParams.minPrice
    }
    if (queryParams.maxPrice !== undefined) {
      params.maxPrice = queryParams.maxPrice
    }
    // 产地筛选
    if (queryParams.origin) {
      params.origin = queryParams.origin
    }
    // 排序
    if (queryParams.sortBy && queryParams.sortBy !== 'default') {
      params.sortBy = queryParams.sortBy
      params.sortOrder = queryParams.sortOrder
    }

    const res: any = await productApi.getProductList(params)
    // 适配后端返回格式
    if (Array.isArray(res)) {
      products.value = res
      total.value = res.length
    } else {
      products.value = res.list || []
      total.value = res.total || 0
    }
  } catch (error) {
    console.error('获取商品列表失败:', error)
    // 使用默认数据
    products.value = [
      { id: 1, name: '山东烟台红富士苹果', image: '/images/apple.jpg', price: 29.9, sales: 1234, origin: '山东烟台' },
      { id: 2, name: '新疆库尔勒香梨', image: '/images/pear.jpg', price: 39.9, sales: 892, origin: '新疆库尔勒' },
      { id: 3, name: '云南普洱茶', image: '/images/tea.jpg', price: 128.0, sales: 567, origin: '云南普洱' },
      { id: 4, name: '东北五常大米', image: '/images/rice.jpg', price: 68.0, sales: 2341, origin: '黑龙江五常' },
      { id: 5, name: '赣南脐橙', image: '/images/orange.jpg', price: 35.8, sales: 1567, origin: '江西赣州' },
      { id: 6, name: '阳澄湖大闸蟹', image: '/images/crab.jpg', price: 288.0, sales: 432, origin: '江苏苏州' }
    ]
    total.value = products.value.length
  }
}

const goToDetail = (id: number) => {
  router.push(`/product/${id}`)
}

onMounted(() => {
  const categoryId = route.query.category as string
  if (categoryId) {
    queryParams.categoryId = Number(categoryId)
  }
  // 从 URL 读取搜索关键词
  const keyword = route.query.keyword as string
  if (keyword) {
    searchKeyword.value = keyword
    queryParams.keyword = keyword
  }
  fetchCategories()
  fetchProducts()
})
</script>

<style scoped>
.product-list-view {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
}

.filter-section {
  background: var(--apple-card-bg);
  border-radius: var(--apple-radius-lg);
  padding: 24px;
  margin-bottom: 24px;
}

.search-box {
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--apple-light-gray);
}

.search-box :deep(.el-input__wrapper) {
  border-radius: 24px 0 0 24px;
  box-shadow: none;
  border: 1px solid var(--apple-light-gray);
  border-right: none;
  padding-left: 16px;
}

.search-box :deep(.el-input__inner) {
  font-size: 15px;
}

.search-box :deep(.el-input-group__append) {
  border-radius: 0 24px 24px 0;
  background: var(--apple-blue);
  border-color: var(--apple-blue);
}

.search-box :deep(.el-input-group__append .el-button) {
  color: white;
  font-weight: 500;
}

.filter-row {
  display: flex;
  align-items: flex-start;
  padding: 12px 0;
  border-bottom: 1px solid var(--apple-light-gray);
}

.filter-row:last-child {
  border-bottom: none;
}

.filter-label {
  width: 60px;
  font-size: 14px;
  color: var(--apple-gray);
  flex-shrink: 0;
  margin-top: 4px;
}

.filter-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  flex: 1;
}

.filter-option {
  padding: 4px 16px;
  font-size: 14px;
  color: var(--apple-dark-gray);
  cursor: pointer;
  border-radius: 16px;
  transition: all 0.3s;
}

.filter-option:hover,
.filter-option.active {
  color: var(--apple-blue);
  background: rgba(0, 122, 255, 0.1);
}

.origin-filter {
  display: flex;
  align-items: center;
  gap: 12px;
}

.list-section {
  background: var(--apple-card-bg);
  border-radius: var(--apple-radius-lg);
  padding: 24px;
}

.sort-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--apple-light-gray);
}

.sort-options {
  display: flex;
  gap: 24px;
}

.sort-option {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  color: var(--apple-gray);
  cursor: pointer;
  transition: color 0.3s;
}

.sort-option:hover,
.sort-option.active {
  color: var(--apple-blue);
}

.result-count {
  font-size: 14px;
  color: var(--apple-gray);
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.product-card {
  background: var(--apple-bg);
  border-radius: var(--apple-radius);
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.3s ease, box-shadow 0.3s ease, border-radius 0.3s ease;
}

.product-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--apple-shadow);
}

.product-image {
  height: 200px;
  overflow: hidden;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.product-info {
  padding: 16px;
}

.product-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--apple-dark-gray);
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-origin {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--apple-gray);
  margin-bottom: 12px;
}

.product-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.product-price {
  font-size: 18px;
  font-weight: 700;
  color: #ff6b6b;
}

.product-sales {
  font-size: 12px;
  color: var(--apple-gray);
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
}
</style>
