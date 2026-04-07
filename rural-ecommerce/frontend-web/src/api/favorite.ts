import request from './request'

// 添加收藏
export const addFavorite = (productId: number) => {
  return request.post('/favorite/add', { productId })
}

// 取消收藏
export const removeFavorite = (productId: number) => {
  return request.delete(`/favorite/remove/${productId}`)
}

// 检查是否已收藏
export const checkFavorite = (productId: number) => {
  return request.get(`/favorite/check/${productId}`)
}

// 获取收藏列表
export const getFavorites = () => {
  return request.get('/favorite/list')
}
