import request from './request'

export interface Address {
  id?: number
  receiverName: string
  receiverPhone: string
  province: string
  city: string
  district: string
  detailAddress: string
  isDefault?: number
  createTime?: string
}

export interface AddressParams {
  receiverName: string
  receiverPhone: string
  province: string
  city: string
  district: string
  detailAddress: string
  isDefault?: number
}

export const addressApi = {
  // 获取收货地址列表
  getAddressList: () => request.get<Address[]>('/api/address/list'),
  
  // 获取默认地址
  getDefaultAddress: () => request.get<Address>('/api/address/default'),
  
  // 添加收货地址
  addAddress: (params: AddressParams) => request.post<Address>('/api/address/add', params),
  
  // 更新收货地址
  updateAddress: (id: number, params: AddressParams) => request.put<Address>(`/api/address/update/${id}`, params),
  
  // 删除收货地址
  deleteAddress: (id: number) => request.delete(`/api/address/delete/${id}`),
  
  // 设置默认地址
  setDefaultAddress: (id: number) => request.put(`/api/address/setDefault/${id}`)
}
