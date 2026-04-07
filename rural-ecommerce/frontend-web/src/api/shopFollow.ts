import request from './request'

export interface ShopFollow {
  id: number
  userId: number
  shopId: number
  shopName: string
  createTime: string
}

export const shopFollowApi = {
  // 关注店铺
  followShop: (shopId: number) => {
    return request.post(`/shop-follow/${shopId}`)
  },

  // 取消关注
  unfollowShop: (shopId: number) => {
    return request.delete(`/shop-follow/${shopId}`)
  },

  // 检查是否已关注
  checkFollow: (shopId: number) => {
    return request.get(`/shop-follow/check/${shopId}`)
  },

  // 获取我关注的店铺列表
  getMyFollows: () => {
    return request.get('/shop-follow/my-follows')
  },

  // 获取店铺粉丝数
  getShopFansCount: (shopId: number) => {
    return request.get(`/shop-follow/shop-fans/${shopId}`)
  },

  // 获取我关注的店铺详细信息
  getMyFollowedShops: () => {
    return request.get('/shop-follow/my-followed-shops')
  }
}
