import request from './request'

export interface FarmerProductParams {
  page?: number
  size?: number
  keyword?: string
  status?: number
}

export interface CreateFarmerProductParams {
  name: string
  description: string
  price: number
  unit?: string
  originalPrice?: number
  stock: number
  mainImage: string
  images?: string
  categoryId: number
  origin?: string
  status?: number
}

export interface FarmerStats {
  todayIncome: number
  todayOrders: number
  onSaleProducts: number
  productViews: number
  totalIncome: number
  totalOrders: number
  pendingOrders: number
  monthIncome: number
}

export interface FarmerOrder {
  id: number
  orderNo: string
  totalAmount: number
  status: number
  createTime: string
  receiverName: string
  receiverPhone: string
  receiverAddress: string
  items: {
    productName: string
    productImage: string
    price: number
    quantity: number
  }[]
}

// 获取农户统计数据
export const getFarmerStats = () => {
  return request.get('/farmer/stats')
}

// 获取农户商品列表
export const getFarmerProducts = (params: FarmerProductParams) => {
  return request.get('/farmer/products', { params })
}

// 创建商品
export const createFarmerProduct = (data: CreateFarmerProductParams) => {
  return request.post('/farmer/product', data)
}

// 更新商品
export const updateFarmerProduct = (id: number, data: CreateFarmerProductParams) => {
  return request.put(`/farmer/product/${id}`, data)
}

// 删除商品
export const deleteFarmerProduct = (id: number) => {
  return request.delete(`/farmer/product/${id}`)
}

// 修改商品状态
export const toggleFarmerProductStatus = (id: number, status: number) => {
  return request.put(`/farmer/product/${id}/status`, { status })
}

// 获取农户订单列表
export const getFarmerOrders = (params: { page?: number; size?: number; status?: number }) => {
  return request.get('/farmer/orders', { params })
}

// 获取订单详情
export const getFarmerOrderDetail = (id: number) => {
  return request.get(`/farmer/order/${id}`)
}

// 发货
export const shipFarmerOrder = (id: number, logisticsNo: string, logisticsCompany: string) => {
  return request.post(`/farmer/order/${id}/ship`, { logisticsNo, logisticsCompany })
}

// 获取收入统计
export const getFarmerIncome = (params: { startDate?: string; endDate?: string }) => {
  return request.get('/farmer/income', { params })
}
