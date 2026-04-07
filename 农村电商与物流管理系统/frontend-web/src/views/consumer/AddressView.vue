<template>
  <div class="address-view">
    <div class="address-container">
      <h2 class="page-title">收货地址管理</h2>

      <div class="address-list">
        <div v-for="addr in addresses" :key="addr.id" class="address-card">
          <div class="address-content">
            <div class="address-header">
              <span class="address-name">{{ addr.receiverName }}</span>
              <span class="address-phone">{{ addr.receiverPhone }}</span>
              <el-tag v-if="addr.isDefault === 1" type="primary" size="small" effect="dark">默认</el-tag>
            </div>
            <p class="address-detail">
              {{ addr.province }}{{ addr.city }}{{ addr.district }}{{ addr.detailAddress }}
            </p>
          </div>
          <div class="address-actions">
            <el-button link type="primary" @click="editAddress(addr)">
              <el-icon><Edit /></el-icon>编辑
            </el-button>
            <el-button link type="danger" @click="deleteAddress(addr.id)">
              <el-icon><Delete /></el-icon>删除
            </el-button>
            <el-button v-if="addr.isDefault !== 1" link @click="setDefault(addr.id)">设为默认</el-button>
          </div>
        </div>

        <!-- 添加地址卡片 -->
        <div class="add-address-card" @click="openAddDialog">
          <el-icon size="32" color="#909399"><Plus /></el-icon>
          <span>添加新地址</span>
        </div>
      </div>

      <!-- 空状态 -->
      <el-empty v-if="addresses.length === 0 && !loading" description="暂无收货地址，请添加新地址" />
    </div>

    <!-- 添加/编辑地址弹窗 -->
    <el-dialog
      v-model="showAddDialog"
      :title="isEdit ? '编辑地址' : '添加地址'"
      width="500px"
    >
      <el-form :model="addressForm" label-width="80px">
        <el-form-item label="收货人">
          <el-input v-model="addressForm.receiverName" placeholder="请输入收货人姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="addressForm.receiverPhone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="所在地区">
          <el-cascader
            v-model="addressForm.region"
            :options="regionOptions"
            placeholder="请选择省/市/区"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="详细地址">
          <el-input
            v-model="addressForm.detailAddress"
            type="textarea"
            :rows="3"
            placeholder="请输入详细地址，如街道、门牌号等"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="locating"
            @click="getCurrentLocation"
          >
            <el-icon><Location /></el-icon>
            <span style="margin-left: 4px">定位当前位置</span>
          </el-button>
          <el-checkbox v-model="addressForm.isDefault" style="margin-left: 16px">设为默认地址</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="saveAddress" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Location } from '@element-plus/icons-vue'
import { addressApi, type Address, type AddressParams } from '@/api/address'
import { regionData, codeToText } from 'element-china-area-data'

// 构建文本到编码的映射
const textToCode: { [key: string]: string } = {}
const buildTextToCode = (data: any[]) => {
  data.forEach(item => {
    textToCode[item.label] = item.value
    if (item.children) {
      buildTextToCode(item.children)
    }
  })
}
buildTextToCode(regionData)

const addresses = ref<Address[]>([])
const loading = ref(false)
const saving = ref(false)
const locating = ref(false)
const showAddDialog = ref(false)
const isEdit = ref(false)
const currentId = ref<number | null>(null)

const addressForm = reactive({
  receiverName: '',
  receiverPhone: '',
  region: [] as string[],
  detailAddress: '',
  isDefault: false
})

// 获取地址列表
const fetchAddressList = async () => {
  loading.value = true
  try {
    const res: any = await addressApi.getAddressList()
    addresses.value = res || []
  } catch (error) {
    console.error('获取地址列表失败:', error)
    ElMessage.error('获取地址列表失败')
  } finally {
    loading.value = false
  }
}

const openAddDialog = () => {
  resetForm()
  showAddDialog.value = true
}

const editAddress = (addr: Address) => {
  isEdit.value = true
  currentId.value = addr.id || null
  addressForm.receiverName = addr.receiverName
  addressForm.receiverPhone = addr.receiverPhone
  // 将文本名称转换为编码
  addressForm.region = [
    textToCode[addr.province] || addr.province,
    textToCode[addr.city] || addr.city,
    textToCode[addr.district] || addr.district
  ]
  addressForm.detailAddress = addr.detailAddress
  addressForm.isDefault = addr.isDefault === 1
  showAddDialog.value = true
}

const deleteAddress = async (id: number | undefined) => {
  if (!id) return
  
  ElMessageBox.confirm('确定要删除该地址吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await addressApi.deleteAddress(id)
      ElMessage.success('删除成功')
      fetchAddressList()
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  })
}

const setDefault = async (id: number | undefined) => {
  if (!id) return
  
  try {
    await addressApi.setDefaultAddress(id)
    ElMessage.success('设置成功')
    fetchAddressList()
  } catch (error) {
    console.error('设置默认地址失败:', error)
    ElMessage.error('设置失败')
  }
}

const saveAddress = async () => {
  // 表单验证
  if (!addressForm.receiverName.trim()) {
    ElMessage.warning('请输入收货人姓名')
    return
  }
  if (!addressForm.receiverPhone.trim()) {
    ElMessage.warning('请输入手机号')
    return
  }
  if (addressForm.region.length !== 3) {
    ElMessage.warning('请选择完整的省市区')
    return
  }
  if (!addressForm.detailAddress.trim()) {
    ElMessage.warning('请输入详细地址')
    return
  }

  saving.value = true
  try {
    // 将地区编码转换为文本
    const provinceText = codeToText[addressForm.region[0]] || addressForm.region[0]
    const cityText = codeToText[addressForm.region[1]] || addressForm.region[1]
    const districtText = codeToText[addressForm.region[2]] || addressForm.region[2]
    
    const params: AddressParams = {
      receiverName: addressForm.receiverName,
      receiverPhone: addressForm.receiverPhone,
      province: provinceText,
      city: cityText,
      district: districtText,
      detailAddress: addressForm.detailAddress,
      isDefault: addressForm.isDefault ? 1 : 0
    }

    if (isEdit.value && currentId.value) {
      await addressApi.updateAddress(currentId.value, params)
      ElMessage.success('修改成功')
    } else {
      await addressApi.addAddress(params)
      ElMessage.success('添加成功')
    }
    
    showAddDialog.value = false
    resetForm()
    fetchAddressList()
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const resetForm = () => {
  addressForm.receiverName = ''
  addressForm.receiverPhone = ''
  addressForm.region = []
  addressForm.detailAddress = ''
  addressForm.isDefault = false
  isEdit.value = false
  currentId.value = null
}

// 获取当前位置 - 使用浏览器定位 + 腾讯地图逆地理编码
const getCurrentLocation = () => {
  if (!navigator.geolocation) {
    ElMessage.error('您的浏览器不支持地理定位')
    return
  }

  locating.value = true
  
  navigator.geolocation.getCurrentPosition(
    (position) => {
      const { latitude, longitude } = position.coords
      console.log('获取到经纬度:', latitude, longitude)
      
      // 使用腾讯地图逆地理编码（JSONP方式，避免跨域）
      const script = document.createElement('script')
      const callbackName = 'geocoderCallback_' + Date.now()
      
      // 设置全局回调函数
      ;(window as any)[callbackName] = (data: any) => {
        try {
          console.log('逆地理编码返回数据:', data)
          
          if (data && data.status === 0 && data.result) {
            const addressComponent = data.result.address_component
            const province = addressComponent.province
            const city = addressComponent.city
            const district = addressComponent.district
            
            console.log('解析到地址:', province, city, district)
            console.log('可用的编码:', Object.keys(textToCode).slice(0, 10))
            
            // 辅助函数：查找匹配的编码
            const findCode = (name: string): string | undefined => {
              // 直接匹配
              if (textToCode[name]) return textToCode[name]
              
              // 去掉"市"、"省"、"区"等后缀再匹配
              const nameWithoutSuffix = name.replace(/[省市自治区回族壮族维吾尔族特别行政区]$/, '')
              if (textToCode[nameWithoutSuffix]) return textToCode[nameWithoutSuffix]
              
              // 模糊匹配
              const matched = Object.keys(textToCode).find(key => {
                const keyWithoutSuffix = key.replace(/[省市自治区回族壮族维吾尔族特别行政区]$/, '')
                return name.includes(key) || key.includes(name) ||
                       nameWithoutSuffix.includes(keyWithoutSuffix) || 
                       keyWithoutSuffix.includes(nameWithoutSuffix)
              })
              return matched ? textToCode[matched] : undefined
            }
            
            // 设置省市区
            const provinceCode = findCode(province)
            const cityCode = findCode(city)
            const districtCode = findCode(district)
            
            console.log('匹配到的编码:', provinceCode, cityCode, districtCode)
            
            // 判断是否为直辖市（省编码和市编码相同，或市名和省名相同）
            const isMunicipality = provinceCode === cityCode || province === city
            
            if (provinceCode && districtCode) {
              // 使用 splice 确保响应式更新
              if (isMunicipality) {
                // 直辖市：需要找到"市辖区"的编码
                // 从 regionData 中找到对应的市辖区编码
                const provinceData = regionData.find(p => p.value === provinceCode)
                const cityData = provinceData?.children?.[0] // 市辖区通常是第一个
                const municipalityCityCode = cityData?.value || cityCode || provinceCode + '01'
                addressForm.region.splice(0, addressForm.region.length, provinceCode, municipalityCityCode, districtCode)
              } else if (cityCode) {
                // 非直辖市，且成功获取到市编码
                addressForm.region.splice(0, addressForm.region.length, provinceCode, cityCode, districtCode)
              } else {
                // 非直辖市，但未获取到市编码，只设置省和区
                addressForm.region.splice(0, addressForm.region.length, provinceCode, districtCode)
                ElMessage.warning('已获取省区信息，请手动选择市')
              }
              ElMessage.success('定位成功')
            } else if (provinceCode) {
              // 至少有省
              addressForm.region.splice(0, addressForm.region.length, provinceCode)
              ElMessage.warning('已获取省市信息，请手动选择区县')
            } else {
              ElMessage.warning('定位失败，请手动选择地区')
            }
          } else {
            throw new Error('逆地理编码返回数据异常')
          }
        } catch (error) {
          console.error('地址解析失败:', error)
          ElMessage.warning('定位失败，请手动选择地区')
        } finally {
          locating.value = false
          // 清理回调函数和script标签
          delete (window as any)[callbackName]
          if (script.parentNode) {
            script.parentNode.removeChild(script)
          }
        }
      }
      
      // 设置超时
      const timeoutId = setTimeout(() => {
        if (locating.value) {
          locating.value = false
          delete (window as any)[callbackName]
          if (script.parentNode) {
            script.parentNode.removeChild(script)
          }
          ElMessage.error('定位超时，请手动选择地区')
        }
      }, 10000)
      
      // 加载腾讯地图逆地理编码脚本
      // 使用公共密钥，无需申请
      script.src = `https://apis.map.qq.com/ws/geocoder/v1/?location=${latitude},${longitude}&key=OB4BZ-D4W3U-B7VVO-4PJWW-6TKDJ-WPB77&get_poi=0&output=jsonp&callback=${callbackName}`
      script.onerror = () => {
        clearTimeout(timeoutId)
        locating.value = false
        delete (window as any)[callbackName]
        ElMessage.error('定位服务加载失败，请手动选择地区')
      }
      
      document.head.appendChild(script)
    },
    (error) => {
      locating.value = false
      let message = '获取位置失败'
      switch (error.code) {
        case error.PERMISSION_DENIED:
          message = '请允许获取位置权限'
          break
        case error.POSITION_UNAVAILABLE:
          message = '位置信息不可用'
          break
        case error.TIMEOUT:
          message = '获取位置超时'
          break
      }
      ElMessage.error(message)
    },
    {
      enableHighAccuracy: true,
      timeout: 10000,
      maximumAge: 0
    }
  )
}

// 地区选项
// 使用 element-china-area-data 提供完整的省市区数据
const regionOptions = regionData

onMounted(() => {
  fetchAddressList()
})
</script>

<style scoped>
.address-view {
  max-width: 1000px;
  margin: 0 auto;
  padding: 24px;
}

.address-container {
  background: var(--apple-card-bg);
  border-radius: var(--apple-radius-lg);
  padding: 32px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--apple-dark-gray);
  margin-bottom: 24px;
}

.address-list {
  display: grid;
  gap: 16px;
}

.address-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: var(--apple-bg);
  border-radius: var(--apple-radius);
  border: 1px solid var(--apple-border);
  transition: all 0.3s ease;
}

.address-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.address-content {
  flex: 1;
}

.address-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.address-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--apple-dark-gray);
}

.address-phone {
  font-size: 14px;
  color: var(--apple-gray);
}

.address-detail {
  font-size: 14px;
  color: var(--apple-dark-gray);
  line-height: 1.5;
}

.address-actions {
  display: flex;
  gap: 8px;
}

.add-address-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: var(--apple-bg);
  border-radius: var(--apple-radius);
  border: 2px dashed var(--apple-border);
  cursor: pointer;
  transition: all 0.3s ease;
  gap: 8px;
}

.add-address-card:hover {
  border-color: var(--apple-blue);
  background: rgba(0, 122, 255, 0.05);
}

.add-address-card span {
  font-size: 14px;
  color: var(--apple-gray);
}
</style>
