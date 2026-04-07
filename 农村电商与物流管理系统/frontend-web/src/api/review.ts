import request from './request'

// 获取商品评价列表
export const getProductReviews = (productId: number | string, page = 1, size = 10, filter?: string): Promise<any> => {
  const params: any = { page, size }
  if (filter && filter !== 'all') {
    params.filter = filter
  }
  return request.get(`/review/product/${productId}`, { params })
}

// 获取商品评价统计
export const getReviewStats = (productId: number | string): Promise<any> => {
  return request.get(`/review/stats/${productId}`)
}

// 发布评价
export const addReview = (data: {
  productId: number | string
  orderId?: number | string
  rating: number
  content: string
  images?: string[]
  video?: string
}): Promise<any> => {
  return request.post('/review/add', data)
}

// 举报评价
export const reportReview = (reviewId: number | string, reportType: number, reportContent?: string): Promise<any> => {
  return request.post('/review/report', {
    reviewId,
    reportType,
    reportContent
  })
}
