<template>
  <div class="shop-manage-view">
    <div class="page-header">
      <h2 class="page-title">店铺管理</h2>
      <el-button
        v-if="hasShop"
        :type="shopStatus === 1 ? 'danger' : 'success'"
        size="large"
        @click="toggleShopStatus"
      >
        {{ shopStatus === 1 ? '关店' : '营业' }}
      </el-button>
    </div>

    <el-form :model="shopForm" :rules="shopRules" ref="shopFormRef" label-width="120px" class="shop-form">
      <el-card class="form-card">
        <template #header>
          <span>店铺信息</span>
        </template>

        <el-form-item label="店铺头像">
          <el-upload
            class="avatar-uploader"
            action="/api/file/upload"
            :show-file-list="false"
            :on-success="handleAvatarSuccess"
            name="file"
          >
            <img v-if="shopForm.avatar" :src="getImageUrl(shopForm.avatar)" class="avatar" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>

        <el-form-item label="店铺名称" prop="shopName">
          <el-input v-model="shopForm.shopName" placeholder="请输入店铺名称" />
        </el-form-item>

        <el-form-item label="店铺简介">
          <el-input
            v-model="shopForm.description"
            type="textarea"
            rows="4"
            placeholder="请输入店铺简介"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="shopForm.phone" placeholder="请输入联系电话" />
        </el-form-item>

        <el-form-item label="所在地区" prop="location">
          <el-cascader
            v-model="locationSelected"
            :options="regionOptions"
            :props="{ value: 'label', label: 'label', checkStrictly: true }"
            placeholder="请选择所在地区"
            clearable
            style="width: 100%"
            @change="handleLocationChange"
          />
        </el-form-item>

        <el-form-item label="主营品类" prop="mainCategory">
          <el-select v-model="shopForm.mainCategory" placeholder="请选择主营品类" style="width: 100%">
            <el-option
              v-for="cat in categoryList"
              :key="cat.id"
              :label="cat.name"
              :value="cat.name"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="店主名称" prop="ownerName">
          <el-input v-model="shopForm.ownerName" placeholder="请输入店主名称" />
        </el-form-item>

        <el-form-item label="店铺标签">
          <el-select
            v-model="shopForm.tags"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="请输入店铺标签"
            style="width: 100%"
          >
            <el-option
              v-for="tag in defaultTags"
              :key="tag"
              :label="tag"
              :value="tag"
            />
          </el-select>
          <div class="form-tip">可输入自定义标签或选择推荐标签</div>
        </el-form-item>
      </el-card>

      <el-card class="form-card">
        <template #header>
          <span>店铺背景图</span>
        </template>
        <el-form-item>
          <el-upload
            class="bg-uploader"
            action="/api/file/upload"
            :show-file-list="false"
            :on-success="handleBgSuccess"
            name="file"
          >
            <img v-if="shopForm.background" :src="getImageUrl(shopForm.background)" class="bg-image" />
            <div v-else class="bg-placeholder">
              <el-icon size="48"><Plus /></el-icon>
              <span>点击上传店铺背景图</span>
              <p class="bg-tip">建议尺寸：1920x400像素，用于店铺详情页头部背景</p>
            </div>
          </el-upload>
        </el-form-item>
      </el-card>

      <el-card class="form-card">
        <template #header>
          <span>店铺封面</span>
        </template>
        <el-form-item>
          <el-upload
            class="cover-uploader"
            action="/api/file/upload"
            :show-file-list="false"
            :on-success="handleCoverSuccess"
            name="file"
          >
            <img v-if="shopForm.cover" :src="getImageUrl(shopForm.cover)" class="cover-image" />
            <div v-else class="cover-placeholder">
              <el-icon size="48"><Plus /></el-icon>
              <span>点击上传店铺封面</span>
            </div>
          </el-upload>
        </el-form-item>
      </el-card>

      <div class="form-actions">
        <el-button
          v-if="!hasShop"
          type="primary"
          size="large"
          @click="createShop"
          :loading="loading"
        >
          开店铺
        </el-button>
        <el-button
          v-else
          type="primary"
          size="large"
          @click="saveShop"
          :loading="loading"
        >
          保存设置
        </el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useUserStore, type UserInfo } from '@/stores/user'
import { productApi } from '@/api/product'
import { userApi } from '@/api/user'
import request from '@/api/request'
import { getImageUrl } from '@/utils/image'

const userStore = useUserStore()
const loading = ref(false)
const hasShop = ref(false)
const shopStatus = ref(1)
const shopFormRef = ref()

const shopForm = reactive({
  avatar: '',
  shopName: '',
  description: '',
  phone: '',
  location: '',
  mainCategory: '',
  ownerName: '',
  tags: [] as string[],
  background: '',
  cover: ''
})

// 表单验证规则
const shopRules = {
  shopName: [{ required: true, message: '请输入店铺名称', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
  location: [{ required: true, message: '请选择所在地区', trigger: 'change' }],
  mainCategory: [{ required: true, message: '请选择主营品类', trigger: 'change' }],
  ownerName: [{ required: true, message: '请输入店主名称', trigger: 'blur' }]
}

// 分类列表
const categoryList = ref<any[]>([])

// 地区选择
const locationSelected = ref<string[]>([])

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

// 处理地区选择变化
const handleLocationChange = (value: string[]) => {
  if (value && value.length > 0) {
    shopForm.location = value.join(' ')
  } else {
    shopForm.location = ''
  }
}

// 默认标签选项
const defaultTags = [
  '有机认证',
  '产地直发',
  '品质保证',
  '绿色无公害',
  '新鲜直达',
  '百年传承',
  '正宗产地',
  '手工制作',
  '草原放养',
  '纯天然',
  '无污染'
]

// 获取店铺信息
const fetchShopInfo = async () => {
  try {
    // 先从后端获取最新用户信息（刷新页面后store数据会丢失）
    const res: any = await userApi.getUserInfo()
    const userInfo = res
    
    // 更新 store 中的用户信息
    if (userInfo) {
      userStore.setUserInfo(userInfo as UserInfo)
    }
    
    if (userInfo) {
      // 检查是否已开店铺：通过 shopName 字段判断
      if (userInfo.shopName) {
        hasShop.value = true
        shopStatus.value = userInfo.status ?? 1
        shopForm.shopName = userInfo.shopName || ''
        shopForm.avatar = userInfo.avatar || ''
        shopForm.description = userInfo.description || ''
        shopForm.phone = userInfo.phone || ''
        shopForm.location = userInfo.location || ''
        // 将location字符串转换为级联选择器数组格式
        if (userInfo.location) {
          locationSelected.value = userInfo.location.split(' ')
        } else {
          locationSelected.value = []
        }
        shopForm.mainCategory = userInfo.mainCategory || ''
        shopForm.ownerName = userInfo.nickname || ''
        shopForm.background = userInfo.background || ''
        if (userInfo.tags) {
          shopForm.tags = userInfo.tags.split(',').filter((tag: string) => tag)
        }
      } else {
        // 未开店铺，所有字段保持为空
        hasShop.value = false
        // 清空表单
        shopForm.shopName = ''
        shopForm.avatar = ''
        shopForm.description = ''
        shopForm.phone = ''
        shopForm.location = ''
        locationSelected.value = []
        shopForm.mainCategory = ''
        shopForm.ownerName = ''
        shopForm.background = ''
        shopForm.cover = ''
        shopForm.tags = []
      }
    }
  } catch (error) {
    console.error('获取店铺信息失败:', error)
    ElMessage.error('获取店铺信息失败')
  }
}

// 开店铺
const createShop = async () => {
  const valid = await shopFormRef.value?.validate()
  if (!valid) return

  loading.value = true
  try {
    const data = {
      shopName: shopForm.shopName,
      nickname: shopForm.ownerName,
      avatar: shopForm.avatar,
      description: shopForm.description,
      phone: shopForm.phone,
      location: shopForm.location,
      mainCategory: shopForm.mainCategory,
      tags: shopForm.tags.join(','),
      background: shopForm.background
    }

    await request.put('/user/info', { ...data, id: userStore.userInfo?.id })
    hasShop.value = true

    // 更新本地用户信息
    userStore.setUserInfo({
      ...(userStore.userInfo || {}),
      ...data
    } as UserInfo)

    ElMessage.success('店铺开通成功！')
  } catch (error: any) {
    ElMessage.error(error.message || '开通店铺失败')
  } finally {
    loading.value = false
  }
}

// 保存设置
const saveShop = async () => {
  const valid = await shopFormRef.value?.validate()
  if (!valid) return

  loading.value = true
  try {
    const data = {
      shopName: shopForm.shopName,
      nickname: shopForm.ownerName,
      avatar: shopForm.avatar,
      description: shopForm.description,
      phone: shopForm.phone,
      location: shopForm.location,
      mainCategory: shopForm.mainCategory,
      tags: shopForm.tags.join(','),
      background: shopForm.background
    }

    await request.put('/user/info', { ...data, id: userStore.userInfo?.id })

    // 更新本地用户信息
    userStore.setUserInfo({
      ...(userStore.userInfo || {}),
      ...data
    } as UserInfo)

    ElMessage.success('保存成功！')
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    loading.value = false
  }
}

// 切换店铺状态
const toggleShopStatus = async () => {
  const newStatus = shopStatus.value === 1 ? 0 : 1
  const actionText = newStatus === 1 ? '营业' : '关店'
  
  try {
    await ElMessageBox.confirm(`确定要${actionText}吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await request.put('/farmer/shop/status', { status: newStatus })
    shopStatus.value = newStatus
    ElMessage.success(`${actionText}成功`)
    
    // 更新本地用户信息
    userStore.setUserInfo({
      ...(userStore.userInfo || {}),
      status: newStatus
    } as UserInfo)
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('操作失败:', error)
      ElMessage.error('操作失败')
    }
  }
}



const handleAvatarSuccess = (res: any) => {
  // el-upload 接收的是原始响应，需要根据后端返回结构获取 url
  const url = res.data?.url || res.url
  if (url) {
    shopForm.avatar = url
    ElMessage.success('上传成功')
  } else {
    ElMessage.error('上传失败')
  }
}

const handleBgSuccess = (res: any) => {
  const url = res.data?.url || res.url
  if (url) {
    shopForm.background = url
    ElMessage.success('背景图上传成功')
  } else {
    ElMessage.error('上传失败')
  }
}

const handleCoverSuccess = (res: any) => {
  const url = res.data?.url || res.url
  if (url) {
    shopForm.cover = url
    ElMessage.success('封面上传成功')
  } else {
    ElMessage.error('上传失败')
  }
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

onMounted(() => {
  fetchCategories()
  fetchShopInfo()
})
</script>

<style scoped>
.shop-manage-view {
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
  margin: 0;
}

.form-card {
  margin-bottom: 24px;
}

.avatar-uploader {
  border: 1px dashed var(--apple-border);
  border-radius: 50%;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 100px;
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: var(--apple-transition);
}

.avatar-uploader:hover {
  border-color: var(--apple-primary);
}

.avatar-uploader-icon {
  font-size: 28px;
  color: var(--apple-light-gray);
}

.avatar {
  width: 100px;
  height: 100px;
  object-fit: cover;
  border-radius: 50%;
}

.form-tip {
  font-size: 12px;
  color: var(--apple-light-gray);
  margin-top: 4px;
}

.bg-uploader {
  border: 1px dashed var(--apple-border);
  border-radius: var(--apple-radius);
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 100%;
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: var(--apple-transition);
}

.bg-uploader:hover {
  border-color: var(--apple-primary);
}

.bg-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: var(--apple-light-gray);
}

.bg-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.bg-tip {
  font-size: 12px;
  color: var(--apple-light-gray);
}

.cover-uploader {
  border: 1px dashed var(--apple-border);
  border-radius: var(--apple-radius);
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 300px;
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: var(--apple-transition);
}

.cover-uploader:hover {
  border-color: var(--apple-primary);
}

.cover-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: var(--apple-light-gray);
}

.cover {
  width: 300px;
  height: 200px;
  object-fit: cover;
  border-radius: var(--apple-radius);
}

.form-actions {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}

.form-actions .el-button {
  min-width: 200px;
}
</style>
