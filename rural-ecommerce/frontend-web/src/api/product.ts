import request from './request'

export interface ProductQuery {
  page?: number
  size?: number
  categoryId?: number
  keyword?: string
  minPrice?: number
  maxPrice?: number
  origin?: string
  sortBy?: string
  sortOrder?: string
}

export const productApi = {
  getProductList: (params: ProductQuery) => request.get('/product/list', { params }),
  getProductDetail: (id: number) => request.get(`/product/${id}`),
  getCategories: () => request.get('/product/category'),
  getRecommendProducts: () => request.get('/product/recommend'),
  getHotProducts: () => request.get('/product/hot'),
  searchProducts: (keyword: string) => request.get('/product/search', { params: { keyword } })
}
