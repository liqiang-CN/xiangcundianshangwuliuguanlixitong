import request from './request'

export interface ChatMessage {
  id?: number
  senderId: number
  receiverId: number
  content: string
  type: number
  extra?: any
  createTime?: string
  senderAvatar?: string
  senderName?: string
}

// 获取聊天记录
export const getChatHistory = (targetId: number, page = 1, size = 20) => {
  return request.get(`/chat/history/${targetId}?page=${page}&size=${size}`)
}

// 获取会话列表
export const getConversations = () => {
  return request.get('/chat/conversations')
}

// 获取未读消息数
export const getUnreadCount = () => {
  return request.get('/chat/unread-count')
}

// 标记消息为已读
export const markAsRead = (targetId: number) => {
  return request.put(`/chat/read/${targetId}`)
}

// 上传图片
export const uploadChatImage = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/file/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 发送消息（HTTP方式，用于WebSocket不可用时）
export const sendMessage = (data: {
  receiverId: number
  type: number
  content: string
  extra?: any
}) => {
  return request.post('/chat/message', data)
}

export const chatApi = {
  getChatHistory,
  getConversations,
  getUnreadCount,
  markAsRead,
  uploadChatImage,
  sendMessage
}
