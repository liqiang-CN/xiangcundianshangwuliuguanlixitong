import request from './request'

// 获取所有聊天会话
export const getChatSessions = (params: any) => {
  return request.get('/admin/chat/sessions', { params })
}

// 获取指定会话的聊天记录
export const getChatHistory = (params: { userId: number; targetId: number; page?: number; size?: number }) => {
  return request.get('/admin/chat/history', { params })
}

// 搜索聊天记录
export const searchChatMessages = (params: any) => {
  return request.get('/admin/chat/search', { params })
}

// 获取聊天统计
export const getChatStatistics = (params?: any) => {
  return request.get('/admin/chat/statistics', { params })
}

// 删除单条消息
export const deleteChatMessage = (id: number) => {
  return request.delete(`/admin/chat/message/${id}`)
}
