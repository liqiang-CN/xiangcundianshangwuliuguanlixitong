import request from './request'

// 获取评价列表
export const getReviewList = (params: {
  page?: number
  size?: number
  productId?: number
  userId?: number
  status?: number
  keyword?: string
}): Promise<any> => {
  return request.get('/admin/review/list', { params })
}

// 获取评价详情
export const getReviewDetail = (id: number | string): Promise<any> => {
  return request.get(`/admin/review/detail/${id}`)
}

// 删除评价
export const deleteReview = (id: number | string): Promise<any> => {
  return request.post(`/admin/review/delete/${id}`)
}

// 批量删除评价
export const batchDeleteReviews = (ids: (number | string)[]): Promise<any> => {
  return request.post('/admin/review/batch-delete', { ids })
}

// 获取评价统计
export const getReviewStats = (): Promise<any> => {
  return request.get('/admin/review/stats')
}

// 获取举报列表
export const getReportList = (params: {
  page?: number
  size?: number
  status?: number
}): Promise<any> => {
  return request.get('/admin/review/report/list', { params })
}

// 处理举报
export const handleReport = (data: {
  reportId: number | string
  status: number
  adminRemark?: string
}): Promise<any> => {
  return request.post('/admin/review/report/handle', data)
}
