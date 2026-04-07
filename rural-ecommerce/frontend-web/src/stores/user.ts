import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface UserInfo {
  id: number
  username: string
  nickname: string
  avatar: string
  phone: string
  email: string
  userType: 'consumer' | 'farmer' | 'logistics' | 'admin'
  status: number
  // 店铺相关字段
  shopName?: string
  description?: string
  location?: string
  mainCategory?: string
  tags?: string
  background?: string
  rating?: number
  followers?: number
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo | null>(null)

  const isLoggedIn = computed(() => !!token.value)
  const isConsumer = computed(() => userInfo.value?.userType === 'consumer')
  const isFarmer = computed(() => userInfo.value?.userType === 'farmer')
  const isLogistics = computed(() => userInfo.value?.userType === 'logistics')

  const setToken = (newToken: string) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  const setUserInfo = (info: UserInfo) => {
    userInfo.value = info
    localStorage.setItem('userInfo', JSON.stringify(info))
  }

  const logout = () => {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  const initUserInfo = () => {
    const savedUserInfo = localStorage.getItem('userInfo')
    if (savedUserInfo) {
      userInfo.value = JSON.parse(savedUserInfo)
    }
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    isConsumer,
    isFarmer,
    isLogistics,
    setToken,
    setUserInfo,
    logout,
    initUserInfo
  }
})
