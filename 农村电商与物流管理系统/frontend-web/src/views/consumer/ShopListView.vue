<template>
  <div class="shop-list-view">
    <!-- 筛选区 -->
    <div class="filter-section">
      <div class="search-box">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索店铺名称"
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
          <span class="filter-label">主营：</span>
          <div class="filter-options">
            <span 
              v-for="cat in categories" 
              :key="cat.id"
              class="filter-option"
              :class="{ active: selectedCategory === cat.name }"
              @click="selectCategory(cat.name)"
            >
              {{ cat.name }}
            </span>
          </div>
        </div>
        <div class="filter-row">
          <span class="filter-label">地区：</span>
          <div class="filter-options origin-filter">
            <span 
              class="filter-option"
              :class="{ active: !selectedOrigin }"
              @click="selectOrigin('')"
            >
              全部
            </span>
            <el-cascader
              v-model="originSelected"
              :options="regionOptions"
              :props="{ value: 'label', label: 'label', checkStrictly: true }"
              placeholder="选择地区"
              clearable
              style="width: 200px"
              @change="handleOriginChange"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 列表区 -->
    <div class="list-section">
      <div class="sort-bar">
        <div class="sort-options">
          <span 
            v-for="sort in sortOptions" 
            :key="sort.value"
            class="sort-option"
            :class="{ active: selectedSort === sort.value }"
            @click="selectSort(sort.value)"
          >
            {{ sort.label }}
          </span>
        </div>
        <span class="result-count">共 {{ filteredShops.length }} 家店铺</span>
      </div>

      <!-- 店铺列表 -->
      <div v-loading="loading" class="shop-grid">
        <div
          v-for="shop in paginatedShops"
          :key="shop.id"
          class="shop-card"
          @click="goToShop(shop.id)"
        >
          <div class="shop-image">
            <img :src="shop.avatar || '/images/default-avatar.jpg'" :alt="shop.name" />
          </div>
          <div class="shop-info">
            <h4 class="shop-name">{{ shop.name }}</h4>
            <p class="shop-location">
              <el-icon><Location /></el-icon>
              {{ shop.location || '暂无位置' }}
            </p>
            <div class="shop-footer">
              <span class="shop-products">{{ shop.productCount || 0 }} 件商品</span>
              <span class="shop-rating">
                <el-icon><StarFilled /></el-icon>
                {{ shop.rating || 5.0 }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="!loading && filteredShops.length === 0" class="empty-state">
        <el-empty description="暂无店铺" />
      </div>

      <!-- 分页 -->
      <div v-if="filteredShops.length > pageSize" class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="filteredShops.length"
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
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Location, StarFilled, Search } from '@element-plus/icons-vue'
import { getFeaturedShops } from '@/api/shop'
import { productApi } from '@/api/product'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const shopList = ref<any[]>([])

// 搜索和筛选
const searchKeyword = ref('')
const selectedCategory = ref('全部')
const selectedOrigin = ref('')
const selectedSort = ref('default')
const originSelected = ref<string[]>([])

// 分页
const page = ref(1)
const pageSize = ref(12)

// 分类列表
const categories = ref<{ id: number; name: string }[]>([{ id: 0, name: '全部' }])

// 排序选项
const sortOptions = [
  { label: '默认', value: 'default' },
  { label: '销量最高', value: 'sales' },
  { label: '评分最高', value: 'rating' },
  { label: '商品最多', value: 'products' }
]

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
      { label: '周口' }, { label: '驻马店' }, { label: '济源' }
    ]
  },
  {
    label: '湖北',
    children: [
      { label: '武汉' }, { label: '黄石' }, { label: '十堰' },
      { label: '宜昌' }, { label: '襄阳' }, { label: '鄂州' },
      { label: '荆门' }, { label: '孝感' }, { label: '荆州' },
      { label: '黄冈' }, { label: '咸宁' }, { label: '随州' },
      { label: '恩施' }, { label: '仙桃' }, { label: '潜江' },
      { label: '天门' }, { label: '神农架' }
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
      { label: '儋州' }, { label: '五指山' }, { label: '琼海' },
      { label: '文昌' }, { label: '万宁' }, { label: '东方' },
      { label: '定安' }, { label: '屯昌' }, { label: '澄迈' },
      { label: '临高' }, { label: '白沙' }, { label: '昌江' },
      { label: '乐东' }, { label: '陵水' }, { label: '保亭' },
      { label: '琼中' }
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
      { label: '塔城' }, { label: '阿勒泰' }, { label: '石河子' },
      { label: '阿拉尔' }, { label: '图木舒克' }, { label: '五家渠' },
      { label: '北屯' }, { label: '铁门关' }, { label: '双河' },
      { label: '可克达拉' }, { label: '昆玉' }, { label: '胡杨河' },
      { label: '新星' }
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

// 获取分类列表
const fetchCategories = async () => {
  try {
    const data = await productApi.getCategories()
    const list = Array.isArray(data) ? data : []
    categories.value = [{ id: 0, name: '全部' }, ...list.map((cat: any) => ({
      id: Number(cat.id),
      name: String(cat.name)
    }))]
  } catch (error) {
    console.error('获取分类失败:', error)
  }
}

// 获取店铺列表
const fetchShopList = async () => {
  loading.value = true
  try {
    const res: any = await getFeaturedShops(100)
    shopList.value = res || []
  } catch (error) {
    console.error('获取店铺列表失败:', error)
    ElMessage.error('获取店铺列表失败')
  } finally {
    loading.value = false
  }
}

// 筛选后的店铺列表
const filteredShops = computed(() => {
  let result = [...shopList.value]
  
  // 搜索关键词
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(shop => 
      shop.name?.toLowerCase().includes(keyword)
    )
  }
  
  // 主营分类筛选
  if (selectedCategory.value && selectedCategory.value !== '全部') {
    result = result.filter(shop => 
      shop.mainCategory === selectedCategory.value
    )
  }
  
  // 地区筛选
  if (selectedOrigin.value) {
    result = result.filter(shop => 
      shop.location?.includes(selectedOrigin.value)
    )
  }
  
  // 排序
  switch (selectedSort.value) {
    case 'sales':
      result.sort((a, b) => (b.sales || 0) - (a.sales || 0))
      break
    case 'rating':
      result.sort((a, b) => (b.rating || 0) - (a.rating || 0))
      break
    case 'products':
      result.sort((a, b) => (b.productCount || 0) - (a.productCount || 0))
      break
  }
  
  return result
})

// 分页后的店铺列表
const paginatedShops = computed(() => {
  const start = (page.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredShops.value.slice(start, end)
})

// 搜索
const handleSearch = () => {
  page.value = 1
}

// 选择分类
const selectCategory = (name: string) => {
  selectedCategory.value = name
  page.value = 1
}

// 选择地区
const selectOrigin = (origin: string) => {
  selectedOrigin.value = origin
  originSelected.value = []
  page.value = 1
}

// 地区级联选择变化
const handleOriginChange = (value: string[]) => {
  if (value && value.length > 0) {
    selectedOrigin.value = value[value.length - 1]
  } else {
    selectedOrigin.value = ''
  }
  page.value = 1
}

// 选择排序
const selectSort = (value: string) => {
  selectedSort.value = value
  page.value = 1
}

// 分页变化
const handlePageChange = (val: number) => {
  page.value = val
}

// 每页数量变化
const handleSizeChange = (val: number) => {
  pageSize.value = val
  page.value = 1
}

// 跳转到店铺详情
const goToShop = (id: number) => {
  router.push(`/consumer/shop/${id}`)
}

onMounted(() => {
  // 从 URL 读取搜索关键词
  const keyword = route.query.keyword as string
  if (keyword) {
    searchKeyword.value = keyword
  }
  fetchCategories()
  fetchShopList()
})
</script>

<style scoped>
.shop-list-view {
  min-height: 100vh;
  background: #f5f5f7;
}

/* 筛选区 */
.filter-section {
  background: #fff;
  padding: 24px;
  margin-bottom: 20px;
}

.search-box {
  max-width: 1200px;
  margin: 0 auto 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #e5e5e5;
}

.search-box :deep(.el-input__wrapper) {
  border-radius: 24px 0 0 24px;
  box-shadow: none;
  border: 1px solid #e5e5e5;
  border-right: none;
  padding-left: 16px;
}

.search-box :deep(.el-input__inner) {
  font-size: 15px;
}

.search-box :deep(.el-input-group__append) {
  border-radius: 0 24px 24px 0;
  background: #007aff;
  border-color: #007aff;
}

.search-box :deep(.el-input-group__append .el-button) {
  color: white;
  font-weight: 500;
}

.filter-container {
  max-width: 1200px;
  margin: 0 auto;
}

.filter-row {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.filter-row:last-child {
  margin-bottom: 0;
}

.filter-label {
  font-size: 14px;
  color: #666;
  margin-right: 16px;
  white-space: nowrap;
}

.filter-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.filter-option {
  padding: 6px 16px;
  font-size: 14px;
  color: #333;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.3s;
}

.filter-option:hover {
  color: #409eff;
}

.filter-option.active {
  background: #409eff;
  color: #fff;
}

.origin-filter {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 列表区 */
.list-section {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px 60px;
}

.sort-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 12px 0;
}

.sort-options {
  display: flex;
  gap: 24px;
}

.sort-option {
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: color 0.3s;
  display: flex;
  align-items: center;
  gap: 4px;
}

.sort-option:hover {
  color: #409eff;
}

.sort-option.active {
  color: #409eff;
  font-weight: 500;
}

.result-count {
  font-size: 14px;
  color: #999;
}

/* 店铺网格 */
.shop-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 32px;
}

.shop-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
}

.shop-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.shop-image {
  height: 200px;
  overflow: hidden;
}

.shop-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.shop-info {
  padding: 16px;
}

.shop-name {
  font-size: 15px;
  font-weight: 600;
  color: #1d1d1f;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shop-location {
  font-size: 13px;
  color: #86868b;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.shop-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.shop-products {
  font-size: 13px;
  color: #666;
}

.shop-rating {
  font-size: 13px;
  color: #ff9500;
  display: flex;
  align-items: center;
  gap: 4px;
}

.empty-state {
  padding: 80px 0;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 40px;
}

@media (max-width: 1200px) {
  .shop-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 900px) {
  .shop-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .shop-grid {
    grid-template-columns: 1fr;
  }
  
  .filter-row {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .filter-label {
    margin-bottom: 8px;
  }
}
</style>
