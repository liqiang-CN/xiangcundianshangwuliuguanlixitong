import request from './request'

export interface LoginParams {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  userInfo: {
    id: number
    username: string
    nickname: string
    avatar: string
    role: string
    permissions: string[]
  }
}

// 登录
export const login = (data: LoginParams): Promise<LoginResult> => {
  return request.post('/admin/login', data)
}

// 获取用户信息
export const getUserInfo = () => {
  return request.get('/admin/info')
}

// 退出登录
export const logout = () => {
  return request.post('/admin/logout')
}
