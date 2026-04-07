import request from './request'

// 获取特色店铺列表
export function getFeaturedShops(limit?: number) {
  return request({
    url: '/shop/featured',
    method: 'get',
    params: { limit }
  })
}

// 获取店铺详情
export function getShopDetail(id: number | string) {
  return request({
    url: `/shop/${id}`,
    method: 'get'
  })
}

// 获取店铺商品列表
export function getShopProducts(
  id: number | string,
  params: {
    page?: number
    size?: number
    sortBy?: string
  } = {}
) {
  return request({
    url: `/shop/${id}/products`,
    method: 'get',
    params
  })
}
