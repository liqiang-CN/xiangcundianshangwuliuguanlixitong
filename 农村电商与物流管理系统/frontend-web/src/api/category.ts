import request from './request'

export interface Category {
  id: number
  name: string
  parentId: number
  level: number
  sort: number
  status: number
  icon?: string
  productCount?: number
  createTime?: string
  updateTime?: string
}

export const categoryApi = {
  // 获取分类列表
  getCategoryList: (): Promise<Category[]> => {
    return request.get('/admin/category/list') as Promise<Category[]>
  },

  // 获取分类详情
  getCategoryDetail: (id: number): Promise<Category> => {
    return request.get(`/admin/category/${id}`) as Promise<Category>
  },

  // 添加分类
  addCategory: (category: Partial<Category>): Promise<Category> => {
    return request.post('/admin/category', category) as Promise<Category>
  },

  // 更新分类
  updateCategory: (id: number, category: Partial<Category>): Promise<Category> => {
    return request.put(`/admin/category/${id}`, category) as Promise<Category>
  },

  // 删除分类
  deleteCategory: (id: number): Promise<void> => {
    return request.delete(`/admin/category/${id}`) as Promise<void>
  }
}
