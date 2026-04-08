import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const request: AxiosInstance = axios.create({
  baseURL: 'https://xiangcundianshangwuliuguanlixitong-production.up.railway.app',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

request.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  (response: AxiosResponse) => {
    console.log('Response:', response.data)
    const { code, message, data } = response.data
    // 使用 == 进行宽松比较，处理数字和字符串类型
    if (code == 200) {
      return data
    } else {
      ElMessage.error(message || '请求失败')
      return Promise.reject(new Error(message))
    }
  },
  (error) => {
    if (error.response) {
      const { status } = error.response
      const userStore = useUserStore()
      
      if (status === 401) {
        userStore.logout()
        ElMessage.warning('登录已过期，请重新登录')
      } else if (status === 403) {
        if (userStore.isLoggedIn) {
          userStore.logout()
          ElMessage.warning('登录已过期，请重新登录')
        }
      } else {
        ElMessage.error(error.response.data?.message || '服务器错误')
      }
    } else {
      ElMessage.error('网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
