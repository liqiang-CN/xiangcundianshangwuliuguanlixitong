import request from './request'

export interface LoginParams {
  username: string
  password: string
  userType: number
}

export interface RegisterParams {
  username: string
  password: string
  phone: string
  userType: number
  nickname?: string
}

export const userApi = {
  login: (params: LoginParams) => request.post('/user/login', params),
  register: (params: RegisterParams) => request.post('/user/register', params),
  getUserInfo: () => request.get('/user/info'),
  updateUserInfo: (data: any) => request.put('/user/info', data),
  updatePassword: (data: any) => request.put('/user/password', data),
  uploadAvatar: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/user/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}
