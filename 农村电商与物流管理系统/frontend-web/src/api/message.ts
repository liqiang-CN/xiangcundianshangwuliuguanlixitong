import request from './request'

export interface Message {
  id: number | string
  userId: number
  title: string
  content: string
  type: number
  isRead: number
  createTime: string
  targetId?: number
  targetName?: string
  targetAvatar?: string
  extra?: any
}

export interface UnreadCount {
  total: number
  system: number
  product: number
  order: number
  chat: number
}

// 获取消息列表
export const getMessageList = (): Promise<Message[]> => {
  return request.get('/message/list') as Promise<Message[]>
}

// 获取未读消息数量
export const getUnreadCount = (): Promise<UnreadCount> => {
  return request.get('/message/unread-count') as Promise<UnreadCount>
}

// 标记消息已读
export const markAsRead = (id: number | string): Promise<void> => {
  return request.post(`/message/read/${id}`) as Promise<void>
}

// 标记所有消息已读
export const markAllAsRead = (): Promise<void> => {
  return request.post('/message/read-all') as Promise<void>
}

// 删除消息
export const deleteMessage = (id: number | string): Promise<void> => {
  return request.delete(`/message/${id}`) as Promise<void>
}

export const messageApi = {
  getMessageList,
  getUnreadCount,
  markAsRead,
  markAllAsRead,
  deleteMessage
}
