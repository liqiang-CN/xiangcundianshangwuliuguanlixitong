import request from './request'

export interface DeliveryStaff {
  id: number
  nickname: string
  phone: string
  avatar: string
  status: number
  location?: string
  createTime?: string
  todayOrders?: number
  totalOrders?: number
  rating?: number
}

export interface DeliveryStaffStats {
  total: number
  active: number
  todayDelivery: number
}

// 获取配送员列表
export const getDeliveryStaffList = (params: {
  page?: number
  size?: number
  nickname?: string
  phone?: string
}) => {
  return request.get('/admin/delivery-staff', { params })
}

// 获取配送员统计
export const getDeliveryStaffStats = () => {
  return request.get('/admin/delivery-staff/stats')
}

// 添加配送员
export const addDeliveryStaff = (data: Partial<DeliveryStaff>) => {
  return request.post('/admin/delivery-staff', data)
}

// 更新配送员
export const updateDeliveryStaff = (id: number, data: Partial<DeliveryStaff>) => {
  return request.put(`/admin/delivery-staff/${id}`, data)
}

// 删除配送员
export const deleteDeliveryStaff = (id: number) => {
  return request.delete(`/admin/delivery-staff/${id}`)
}

// 切换配送员状态
export const toggleDeliveryStaffStatus = (id: number, status: number) => {
  return request.put(`/admin/delivery-staff/${id}/status`, { status })
}
