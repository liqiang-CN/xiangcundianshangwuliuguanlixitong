import request from './request'

export interface CreateOrderParams {
  addressId: number
  items: {
    productId: number
    quantity: number
  }[]
  remark?: string
  receiverName: string
  receiverPhone: string
  receiverAddress: string
  totalAmount: number
  freightAmount: number
  payAmount: number
}

export const orderApi = {
  createOrder: (params: CreateOrderParams) => request.post('/order', params),
  getOrderList: (params: any) => request.get('/order/list', { params }),
  getOrderDetail: (id: number) => request.get(`/order/${id}`),
  cancelOrder: (id: number) => request.put(`/order/${id}/cancel`),
  payOrder: (id: number, payType: string) => request.post(`/order/${id}/pay`, { payType }),
  confirmReceive: (id: number) => request.put(`/order/${id}/confirm`),
  getLogistics: (id: number) => request.get(`/order/${id}/logistics`)
}
