import { ElMessage } from 'element-plus'

class WebSocketService {
  private ws: WebSocket | null = null
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private reconnectDelay = 3000
  private farmerId: number | null = null
  private messageHandlers: Map<string, ((data: any) => void)[]> = new Map()
  private heartbeatInterval: number | null = null

  // 连接 WebSocket
  connect(farmerId: number) {
    this.farmerId = farmerId
    // 使用后端 WebSocket 地址
    // 使用当前页面的 host，避免硬编码 IP
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.host.replace(/:\d+$/, '') // 移除端口号
    const wsUrl = `${protocol}//${host}:8080/ws/order?farmerId=${farmerId}`

    try {
      this.ws = new WebSocket(wsUrl)

      this.ws.onopen = () => {
        console.log('WebSocket 连接成功')
        this.reconnectAttempts = 0
        this.startHeartbeat()
      }

      this.ws.onmessage = (event) => {
        this.handleMessage(event.data)
      }

      this.ws.onclose = () => {
        console.log('WebSocket 连接关闭')
        this.stopHeartbeat()
        this.attemptReconnect()
      }

      this.ws.onerror = (error) => {
        console.error('WebSocket 错误:', error)
      }
    } catch (error) {
      console.error('WebSocket 连接失败:', error)
      this.attemptReconnect()
    }
  }

  // 断开连接
  disconnect() {
    this.stopHeartbeat()
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
    this.farmerId = null
    this.reconnectAttempts = 0
  }

  // 重新连接
  private attemptReconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.log('WebSocket 重连次数已达上限')
      return
    }

    this.reconnectAttempts++
    console.log(`WebSocket ${this.reconnectAttempts}秒后尝试重连...`)

    setTimeout(() => {
      if (this.farmerId) {
        this.connect(this.farmerId)
      }
    }, this.reconnectDelay)
  }

  // 启动心跳
  private startHeartbeat() {
    this.heartbeatInterval = window.setInterval(() => {
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        this.ws.send('ping')
      }
    }, 30000) // 30秒发送一次心跳
  }

  // 停止心跳
  private stopHeartbeat() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval)
      this.heartbeatInterval = null
    }
  }

  // 处理消息
  private handleMessage(data: string) {
    if (data === 'pong') {
      return // 心跳响应
    }

    try {
      const message = JSON.parse(data)
      const handlers = this.messageHandlers.get(message.type)
      if (handlers) {
        handlers.forEach(handler => handler(message.data))
      }
    } catch (error) {
      console.error('解析 WebSocket 消息失败:', error)
    }
  }

  // 注册消息处理器
  on(type: string, handler: (data: any) => void) {
    if (!this.messageHandlers.has(type)) {
      this.messageHandlers.set(type, [])
    }
    this.messageHandlers.get(type)!.push(handler)
  }

  // 移除消息处理器
  off(type: string, handler: (data: any) => void) {
    const handlers = this.messageHandlers.get(type)
    if (handlers) {
      const index = handlers.indexOf(handler)
      if (index > -1) {
        handlers.splice(index, 1)
      }
    }
  }

  // 是否已连接
  isConnected() {
    return this.ws && this.ws.readyState === WebSocket.OPEN
  }
}

// 单例模式
export const wsService = new WebSocketService()
