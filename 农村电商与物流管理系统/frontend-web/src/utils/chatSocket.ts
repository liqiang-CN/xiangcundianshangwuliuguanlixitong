import { ElMessage } from 'element-plus'

class ChatSocketService {
  private ws: WebSocket | null = null
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private reconnectDelay = 3000
  private userId: number | null = null
  private messageHandlers: ((data: any) => void)[] = []
  private heartbeatInterval: number | null = null

  // 连接 WebSocket
  connect(userId: number) {
    // 如果已经连接，不再重复连接
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      console.log('聊天WebSocket已连接')
      return
    }

    this.userId = userId
    
    // 构建WebSocket地址
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    // 使用当前host，但在开发环境下需要去掉端口号（因为前端端口和后端端口不同）
    const host = window.location.hostname
    const port = window.location.port === '5173' ? '8080' : (window.location.port || '')
    const wsUrl = port 
      ? `${protocol}//${host}:${port}/ws/chat?userId=${userId}`
      : `${protocol}//${host}/ws/chat?userId=${userId}`

    console.log('连接WebSocket:', wsUrl)

    try {
      this.ws = new WebSocket(wsUrl)

      this.ws.onopen = () => {
        console.log('聊天WebSocket连接成功')
        this.reconnectAttempts = 0
        this.startHeartbeat()
      }

      this.ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          if (data.error) {
            console.error('WebSocket消息错误:', data.error)
            return
          }
          console.log('WebSocket收到消息:', data)
          // 通知所有消息处理器
          this.messageHandlers.forEach((handler, index) => {
            console.log(`调用消息处理器 ${index}`)
            handler(data)
          })
        } catch (e) {
          // 忽略非JSON消息（如pong）
        }
      }

      this.ws.onclose = (event) => {
        console.log('聊天WebSocket连接关闭', event.code, event.reason)
        this.stopHeartbeat()
        // 非正常关闭才重连
        if (event.code !== 1000 && event.code !== 1001) {
          this.attemptReconnect()
        }
      }

      this.ws.onerror = (error) => {
        console.error('聊天WebSocket错误:', error)
      }
    } catch (error) {
      console.error('聊天WebSocket连接失败:', error)
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
    this.userId = null
    this.reconnectAttempts = 0
    this.messageHandlers = []
  }

  // 重新连接
  private attemptReconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.log('聊天WebSocket重连次数已达上限')
      return
    }

    this.reconnectAttempts++
    console.log(`聊天WebSocket ${this.reconnectAttempts}秒后尝试重连...`)

    setTimeout(() => {
      if (this.userId) {
        this.connect(this.userId)
      }
    }, this.reconnectDelay)
  }

  // 启动心跳
  private startHeartbeat() {
    this.heartbeatInterval = window.setInterval(() => {
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        this.ws.send('ping')
      }
    }, 30000)
  }

  // 停止心跳
  private stopHeartbeat() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval)
      this.heartbeatInterval = null
    }
  }

  // 检查是否已连接
  isConnected(): boolean {
    return this.ws !== null && this.ws.readyState === WebSocket.OPEN
  }

  // 检查是否正在连接
  isConnecting(): boolean {
    return this.ws !== null && this.ws.readyState === WebSocket.CONNECTING
  }

  // 发送消息
  sendMessage(message: {
    receiverId: number
    type: number
    content: string
    extra?: any
  }, userId?: number): boolean {
    // 如果已连接，直接发送
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message))
      return true
    }
    
    // 如果正在连接中，等待连接成功后发送
    if (this.ws && this.ws.readyState === WebSocket.CONNECTING) {
      const checkAndSend = () => {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
          this.ws.send(JSON.stringify(message))
        } else if (this.ws && this.ws.readyState === WebSocket.CONNECTING) {
          setTimeout(checkAndSend, 100)
        }
      }
      setTimeout(checkAndSend, 100)
      return true
    }
    
    // 未连接，尝试重新连接
    if (userId) {
      console.log('WebSocket未连接，尝试重新连接...')
      this.connect(userId)
      
      // 等待连接成功后发送
      const checkAndSend = () => {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
          this.ws.send(JSON.stringify(message))
        } else if (!this.ws || this.ws.readyState === WebSocket.CONNECTING) {
          setTimeout(checkAndSend, 100)
        }
      }
      setTimeout(checkAndSend, 500) // 给连接一些时间
      return true
    }
    
    return false
  }

  // 添加消息处理器
  onMessage(handler: (data: any) => void) {
    // 检查是否已存在相同的处理器
    if (!this.messageHandlers.includes(handler)) {
      this.messageHandlers.push(handler)
      console.log('添加消息处理器，当前处理器数量:', this.messageHandlers.length)
    }
  }

  // 移除消息处理器
  offMessage(handler: (data: any) => void) {
    const index = this.messageHandlers.indexOf(handler)
    if (index > -1) {
      this.messageHandlers.splice(index, 1)
      console.log('移除消息处理器，当前处理器数量:', this.messageHandlers.length)
    }
  }
}

export const chatSocket = new ChatSocketService()
