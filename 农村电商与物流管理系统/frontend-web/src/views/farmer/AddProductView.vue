<template>
  <div class="add-product-view">
    <div class="page-header">
      <h2 class="page-title">{{ isEdit ? '编辑商品' : '发布商品' }}</h2>
    </div>

    <el-form :model="productForm" :rules="rules" ref="formRef" label-width="100px" class="product-form">
      <el-card class="form-card">
        <template #header>
          <span>基本信息</span>
        </template>
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="productForm.name" placeholder="请输入商品名称" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="商品分类" prop="categoryId">
          <el-select v-model="productForm.categoryId" placeholder="请选择商品分类" style="width: 100%">
            <el-option v-for="cat in categoryList" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="商品描述" prop="description">
          <el-input
            v-model="productForm.description"
            type="textarea"
            rows="4"
            placeholder="请输入商品描述"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-card>

      <el-card class="form-card">
        <template #header>
          <span>价格库存</span>
        </template>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="商品价格" prop="price">
              <el-input-number v-model="productForm.price" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单位" prop="unit">
              <el-input v-model="productForm.unit" placeholder="如：斤、个、盒" maxlength="10" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="商品库存" prop="stock">
              <el-input-number v-model="productForm.stock" :min="0" :precision="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="产地" prop="origin">
          <el-cascader
            v-model="originSelected"
            :options="regionOptions"
            :props="{ value: 'label', label: 'label' }"
            placeholder="请选择产地"
            style="width: 100%"
            @change="handleOriginChange"
          />
        </el-form-item>
      </el-card>

      <el-card class="form-card">
        <template #header>
          <span>商品图片</span>
        </template>
        <el-form-item label="商品图片" prop="mainImage">
          <div class="image-upload-list">
            <!-- 已上传的图片列表 -->
            <div v-for="(img, index) in productImages" :key="index" class="image-preview-box">
              <img :src="img" class="preview-img" />
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
      </el-card>

      <!-- 商品介绍 -->
      <el-card class="form-card">
        <template #header>
          <div class="intro-header">
            <span>商品介绍</span>
            <span class="intro-tip">（选填）</span>
          </div>
        </template>

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
              <el-form-item label="标题">
                <el-input
                  v-model="item.title"
                  placeholder="请输入标题（选填）"
                  maxlength="100"
                  show-word-limit
                />
              </el-form-item>
              <el-form-item :label="`文本内容`">
                <el-input
                  v-model="item.content"
                  type="textarea"
                  rows="4"
                  placeholder="请输入介绍文本"
                  maxlength="1000"
                  show-word-limit
                />
              </el-form-item>
              <el-form-item label="图片">
                <div class="intro-image-upload">
                  <div v-if="item.imageUrl" class="image-preview-box">
                    <img :src="item.imageUrl" class="preview-img" />
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
                    :before-upload="beforeIntroImageUpload"
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
              <el-form-item label="图片">
                <div class="intro-image-upload">
                  <div v-if="item.imageUrl" class="image-preview-box">
                    <img :src="item.imageUrl" class="preview-img" />
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
                    :before-upload="beforeIntroImageUpload"
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
      </el-card>

      <div class="form-actions">
        <el-button size="large" @click="$router.back()">取消</el-button>
        <el-button type="primary" size="large" @click="submitForm" :loading="submitting">{{ isEdit ? '保存修改' : '立即发布' }}</el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import { productApi } from '@/api/product'
import { createFarmerProduct, updateFarmerProduct, getFarmerProducts } from '@/api/farmer'

const route = useRoute()
const router = useRouter()
const isEdit = ref(!!route.params.id)
const formRef = ref()
const submitting = ref(false)
const categoryList = ref<any[]>([])
const productImages = ref<string[]>([]) // 商品图片列表（最多5张）
const originSelected = ref<string[]>([]) // 产地选择

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

// 处理产地选择变化
const handleOriginChange = (value: string[]) => {
  if (value && value.length > 0) {
    productForm.origin = value.join(' ')
  } else {
    productForm.origin = ''
  }
}

const productForm = reactive({
  name: '',
  categoryId: undefined as number | undefined,
  description: '',
  price: 0,
  unit: '',
  stock: 0,
  origin: '',
  mainImage: '',
  images: [] as string[], // 商品图片列表
  status: 1
})

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

// 上传介绍图片前处理
const beforeIntroImageUpload = (file: File) => {
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

// 介绍图片上传成功
const handleIntroImageSuccess = (response: any, index: number) => {
  if (response && response.code === 200 && response.data && response.data.url) {
    introductionList.value[index].imageUrl = response.data.url
    ElMessage.success('图片上传成功')
  } else {
    ElMessage.error(response?.message || '上传失败')
  }
}

const rules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择商品分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入商品价格', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入商品库存', trigger: 'blur' }],
  mainImage: [{ required: true, message: '请上传商品主图', trigger: 'change' }]
}

// 获取分类列表
const fetchCategories = async () => {
  try {
    const res: any = await productApi.getCategories()
    categoryList.value = res || []
  } catch (error) {
    console.error('获取分类失败:', error)
  }
}

// 如果是编辑模式，获取商品详情
const fetchProductDetail = async () => {
  if (!isEdit.value) return
  try {
    const res: any = await getFarmerProducts({ page: 1, size: 100 })
    const product = res?.find((p: any) => p.id === Number(route.params.id))
    if (product) {
      Object.assign(productForm, {
        name: product.name,
        categoryId: product.categoryId,
        description: product.description,
        price: product.price,
        unit: product.unit || '',
        stock: product.stock,
        origin: product.origin,
        mainImage: product.mainImage || product.image,
        images: product.images || [],
        status: product.status
      })
      // 设置产地选择器
      if (product.origin) {
        originSelected.value = product.origin.split(' ')
      }
      // 设置图片列表（主图放第一个）
      const mainImg = product.mainImage || product.image
      let otherImgs: string[] = []
      if (product.images) {
        try {
          otherImgs = JSON.parse(product.images)
        } catch {
          otherImgs = []
        }
      }
      productImages.value = mainImg ? [mainImg, ...otherImgs.filter((img: string) => img !== mainImg)] : otherImgs

      // 加载商品介绍
      if (product.introductionList && product.introductionList.length > 0) {
        introductionList.value = product.introductionList.map((item: any) => ({
          type: item.type,
          title: item.title || '',
          content: item.content || '',
          imageUrl: item.imageUrl || ''
        }))
      }
    }
  } catch (error) {
    console.error('获取商品详情失败:', error)
  }
}

// 上传前处理
const beforeImageUpload = (file: File) => {
  // 检查是否超过5张
  if (productImages.value.length >= 5) {
    ElMessage.warning('最多只能上传5张图片')
    return false
  }
  // 检查文件类型
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('请上传图片文件')
    return false
  }
  // 检查文件大小（最大5MB）
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过5MB')
    return false
  }
  return true
}

// 上传成功回调
const handleImageSuccess = (response: any) => {
  // 后端返回格式: { code: 200, data: { url: "xxx", name: "xxx" }, message: "success" }
  if (response && response.code === 200 && response.data && response.data.url) {
    const imageUrl = response.data.url
    productImages.value.push(imageUrl)
    // 如果是第一张图片，自动设为主图
    if (productImages.value.length === 1) {
      productForm.mainImage = imageUrl
    }
    // 更新图片列表（除主图外的其他图片）
    productForm.images = productImages.value.slice(1)
    ElMessage.success('图片上传成功')
  } else {
    ElMessage.error(response?.message || '上传失败')
  }
}

// 上传失败回调
const handleUploadError = () => {
  ElMessage.error('图片上传失败，请重试')
}

// 删除图片
const removeImage = (index: number) => {
  productImages.value.splice(index, 1)
  // 更新主图（第一张）
  productForm.mainImage = productImages.value[0] || ''
  // 更新图片列表（除主图外的其他图片）
  productForm.images = productImages.value.slice(1)
}

// 设为主图
const setMainImage = (index: number) => {
  if (index === 0) return
  // 将指定图片移到第一个位置
  const img = productImages.value.splice(index, 1)[0]
  productImages.value.unshift(img)
  // 更新主图
  productForm.mainImage = productImages.value[0]
  // 更新图片列表
  productForm.images = productImages.value.slice(1)
  ElMessage.success('已设为主图')
}

const submitForm = async () => {
  const valid = await formRef.value?.validate()
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
        // 文本+图片模式：至少要有文本或图片
        if (item.type === 'text_image') {
          return item.content.trim() || item.imageUrl
        }
        // 图片模式：必须要有图片
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
      unit: productForm.unit,
      stock: productForm.stock,
      mainImage: productForm.mainImage,
      images: productForm.images.length > 0 ? JSON.stringify(productForm.images) : '',
      categoryId: productForm.categoryId!,
      origin: productForm.origin,
      farmerId: 1, // 当前登录农户ID
      status: productForm.status,
      introductionList: introductionData
    }

    if (isEdit.value) {
      await updateFarmerProduct(Number(route.params.id), params)
      ElMessage.success('修改成功')
    } else {
      await createFarmerProduct(params)
      ElMessage.success('发布成功，等待审核')
    }
    router.push('/farmer/products')
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error('提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchCategories()
  fetchProductDetail()
})
</script>

<style scoped>
.add-product-view {
  padding-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--apple-dark-gray);
  margin-bottom: 24px;
}

.form-card {
  margin-bottom: 24px;
}

.image-upload-list {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.avatar-uploader {
  border: 1px dashed var(--apple-border);
  border-radius: 8px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 150px;
  height: 150px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.avatar-uploader:hover {
  border-color: var(--apple-blue);
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
}

.image-preview-box {
  width: 150px;
  border: 1px solid var(--apple-border);
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
}

.image-preview-box:first-child {
  border-color: var(--apple-blue);
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.preview-img {
  width: 150px;
  height: 150px;
  object-fit: cover;
  display: block;
}

.image-actions {
  padding: 8px;
  text-align: center;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.upload-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.form-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 32px;
}

/* 商品介绍样式 */
.intro-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.intro-tip {
  font-size: 12px;
  color: #909399;
  font-weight: normal;
}

.introduction-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.introduction-item {
  border: 1px solid var(--apple-border);
  border-radius: 8px;
  padding: 16px;
  background: #fafafa;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--apple-border);
}

.item-title {
  font-weight: 600;
  color: var(--apple-dark-gray);
}

.intro-image-upload {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.add-intro-actions {
  display: flex;
  justify-content: center;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px dashed var(--apple-border);
}
</style>
