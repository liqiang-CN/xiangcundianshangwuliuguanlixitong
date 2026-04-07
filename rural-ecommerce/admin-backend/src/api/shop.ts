import request from './request'

// 获取店铺列表
export const getShopList = (params: {
  page?: number
  size?: number
  name?: string
  category?: string
  status?: number | null
}) => {
  return request.get('/shop/admin/shop/list', { params })
}

// 获取店铺详情
export const getShopDetail = (id: number) => {
  return request.get(`/shop/admin/shop/${id}`)
}

// 更新店铺信息
export const updateShop = (id: number, data: {
  nickname?: string
  location?: string
  mainCategory?: string
  tags?: string
  description?: string
  rating?: number
  status?: number
}) => {
  return request.put(`/shop/admin/shop/${id}`, data)
}

// 切换店铺状态
export const toggleShopStatus = (id: number, status: number) => {
  return request.put(`/shop/admin/shop/${id}/status`, { status })
}

// 获取店铺统计
export const getShopStats = () => {
  return request.get('/shop/admin/shop/stats')
}

// 删除店铺
export const deleteShop = (id: number) => {
  return request.delete(`/shop/admin/shop/${id}`)
}
