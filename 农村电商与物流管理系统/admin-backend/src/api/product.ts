import request from './request'

export interface ProductIntroduction {
  id?: number
  type: 'text_image' | 'image'
  title?: string
  content?: string
  imageUrl?: string
  sortOrder?: number
}

export interface Product {
  id: number
  name: string
  description: string
  price: number
  unit?: string
  originalPrice: number
  stock: number
  sales: number
  image: string
  mainImage: string
  images: string
  categoryId: number
  categoryName: string
  farmerId: number
  farmerName: string
  status: number
  isRecommend: number
  isHot: number
  createTime: string
  updateTime: string
  introductionList?: ProductIntroduction[]
}

export interface ProductParams {
  page?: number
  size?: number
  categoryId?: number
  keyword?: string
  status?: number
}

export interface CreateProductParams {
  name: string
  description: string
  price: number
  originalPrice?: number
  stock: number
  mainImage: string
  images?: string
  categoryId: number
  farmerId: number
  status?: number
  isRecommend?: number
  isHot?: number
}

// 获取商品列表
export const getProductList = (params: ProductParams) => {
  return request.get('/product/list', { params })
}

// 获取商品详情
export const getProductDetail = (id: number) => {
  return request.get(`/product/${id}`)
}

// 创建商品
export const createProduct = (data: CreateProductParams) => {
  return request.post('/product', data)
}

// 更新商品
export const updateProduct = (id: number, data: CreateProductParams) => {
  return request.put(`/product/${id}`, data)
}

// 删除商品
export const deleteProduct = (id: number) => {
  return request.delete(`/product/${id}`)
}

// 上架/下架商品
export const toggleProductStatus = (id: number, status: number) => {
  return request.put(`/product/${id}/status`, { status })
}

// 获取分类列表
export const getCategories = () => {
  return request.get('/product/category')
}

// 创建分类
export const createCategory = (data: { name: string; icon?: string; sort?: number }) => {
  return request.post('/category', data)
}

// 更新分类
export const updateCategory = (id: number, data: { name: string; icon?: string; sort?: number }) => {
  return request.put(`/category/${id}`, data)
}

// 删除分类
export const deleteCategory = (id: number) => {
  return request.delete(`/category/${id}`)
}
