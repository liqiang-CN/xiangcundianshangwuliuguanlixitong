<template>
  <div class="messages-page">
    <div class="messages-container">
      <!-- 左侧导航 -->
      <div class="sidebar">
        <div class="sidebar-header">
          <h2 class="sidebar-title">消息中心</h2>
          <p class="sidebar-subtitle">管理您的更新操作</p>
        </div>

        <div class="nav-menu">
          <div 
            class="nav-item" 
            :class="{ active: currentType === 0 }"
            @click="handleNavClick(0)"
          >
            <el-icon class="nav-icon"><Message /></el-icon>
            <span class="nav-text">全部消息</span>
            <el-badge v-if="unreadCount > 0" :value="unreadCount" class="nav-badge" type="primary" />
          </div>
          <div 
            class="nav-item" 
            :class="{ active: currentType === 1 }"
            @click="handleNavClick(1)"
          >
            <el-icon class="nav-icon"><Setting /></el-icon>
            <span class="nav-text">系统通知</span>
            <el-badge v-if="typeUnreadCount[1] > 0" :value="typeUnreadCount[1]" class="nav-badge" type="success" />
          </div>
          <div 
            class="nav-item" 
            :class="{ active: currentType === 2 }"
            @click="handleNavClick(2)"
          >
            <el-icon class="nav-icon"><Goods /></el-icon>
            <span class="nav-text">商品通知</span>
            <el-badge v-if="typeUnreadCount[2] > 0" :value="typeUnreadCount[2]" class="nav-badge" type="success" />
          </div>
          <div 
            class="nav-item" 
            :class="{ active: currentType === 3 }"
            @click="handleNavClick(3)"
          >
            <el-icon class="nav-icon"><Van /></el-icon>
            <span class="nav-text">订单通知</span>
            <el-badge v-if="typeUnreadCount[3] > 0" :value="typeUnreadCount[3]" class="nav-badge" type="success" />
          </div>
          <div 
            class="nav-item" 
            :class="{ active: currentType === 4 }"
            @click="handleNavClick(4)"
          >
            <el-icon class="nav-icon"><ChatDotRound /></el-icon>
            <span class="nav-text">消息通知</span>
            <el-badge v-if="typeUnreadCount[4] > 0" :value="typeUnreadCount[4]" class="nav-badge" type="success" />
          </div>
        </div>

        <div class="sidebar-footer">
          <div class="nav-item" @click="showSettings = true">
            <el-icon class="nav-icon"><Setting /></el-icon>
            <span class="nav-text">通知设置</span>
          </div>
        </div>
      </div>

      <!-- 右侧内容 -->
      <div class="content">
        <!-- 消息列表（非聊天模式） -->
        <template v-if="!showChatPanel">
          <div class="content-header">
            <h3 class="content-title">{{ currentTypeName }}</h3>
            <div class="content-actions">
              <el-button type="primary" text @click="handleMarkAllRead" :disabled="currentUnreadCount === 0">
                <el-icon><Check /></el-icon>
                全部已读
              </el-button>
              <el-button type="danger" text @click="handleClearAll" :disabled="filteredMessages.length === 0">
                <el-icon><Delete /></el-icon>
                清空
              </el-button>
            </div>
          </div>

          <!-- 消息列表 -->
          <div class="messages-list" v-if="filteredMessages.length > 0">
            <div 
              v-for="message in filteredMessages" 
              :key="message.id"
              class="message-item"
              :class="{ unread: message.isRead === 0 }"
              @click="handleMessageClick(message)"
            >
              <div class="message-icon">
                <el-icon v-if="message.type === 1" class="icon-system"><Setting /></el-icon>
                <el-icon v-else-if="message.type === 2" class="icon-product"><Goods /></el-icon>
                <el-icon v-else-if="message.type === 3" class="icon-order"><Van /></el-icon>
                <el-icon v-else class="icon-chat"><ChatDotRound /></el-icon>
              </div>
              <div class="message-content">
                <div class="message-header">
                  <h4 class="message-title">{{ message.title }}</h4>
                </div>
                <p class="message-text">{{ message.content }}</p>
              </div>
              <div class="message-actions">
                <span class="message-time">{{ formatMessageTime(message.createTime) }}</span>
                <el-badge v-if="message.isRead === 0" type="danger" class="unread-dot" />
                <el-button 
                  link 
                  type="danger" 
                  size="small"
                  @click.stop="handleDelete(message.id)"
                >
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>
          </div>

          <!-- 空状态 -->
          <div v-else class="empty-state">
            <el-icon :size="64" class="empty-icon"><Message /></el-icon>
            <p class="empty-text">暂无消息</p>
            <p class="empty-subtext">当有新消息时会在这里显示</p>
          </div>
        </template>

        <!-- 聊天窗口（聊天模式） -->
        <div v-else class="chat-window">
          <!-- 头部 -->
          <div class="chat-header">
            <div class="header-info">
              <el-avatar :size="36" :src="getImageUrl(currentChatTarget?.targetAvatar || '')" />
              <span class="target-name">{{ currentChatTarget?.targetName || '聊天' }}</span>
            </div>
            <div class="header-actions">
              <el-button link @click="closeChatPanel">
                <el-icon><Close /></el-icon>
              </el-button>
            </div>
          </div>

          <!-- 消息列表 -->
          <div class="chat-messages" ref="chatMessageContainer">
            <div
              v-for="msg in chatMessages"
              :key="msg.id"
              class="message-group"
            >
              <!-- 时间戳 -->
              <div class="message-time">{{ formatTime(msg.createTime) }}</div>
              
              <div
                class="message-item"
                :class="{ 'self': isSelfMessage(msg) }"
              >
                <el-avatar 
                  :size="36" 
                  :src="isSelfMessage(msg) ? getImageUrl(userStore.userInfo?.avatar || '') : getImageUrl(currentChatTarget?.targetAvatar || '')"
                >
                  <el-icon><User v-if="isSelfMessage(msg)" /><Shop v-else /></el-icon>
                </el-avatar>
                <div class="message-content">
                  <!-- 文本消息 -->
                  <div v-if="msg.type === 1 || !msg.type" class="text-message">{{ msg.content }}</div>
                  
                  <!-- 图片消息 -->
                  <div v-else-if="msg.type === 2" class="image-message">
                    <img :src="getImageUrl(msg.content)" @click="previewImage(msg.content)" />
                  </div>
                  
                  <!-- 订单卡片 -->
                  <div v-else-if="msg.type === 3" class="card-message order-card" @click="goToOrder(msg.extra?.orderId)">
                    <div class="card-header">
                      <img :src="getImageUrl(msg.extra?.productImage)" class="card-image" />
                      <div class="card-info">
                        <div class="card-title">{{ msg.extra?.productName }}</div>
                        <div class="card-desc" v-if="msg.extra?.productDesc">{{ msg.extra?.productDesc }}</div>
                      </div>
                    </div>
                    <div class="card-footer">
                      <span class="card-price">
                        ¥{{ msg.extra?.price }}<span v-if="msg.extra?.unit">/{{ msg.extra?.unit }}</span>
                      </span>
                      <span class="card-status">{{ msg.extra?.statusText }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 底部输入区 -->
          <div class="chat-input-area">
            <div class="input-toolbar">
              <el-dropdown trigger="click">
                <el-button link>
                  <el-icon size="20"><Plus /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="triggerImageUpload">
                      <el-icon><Picture /></el-icon>发送图片
                    </el-dropdown-item>
                    <el-dropdown-item @click="showOrderSelector = true">
                      <el-icon><Document /></el-icon>发送订单
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <input
                ref="imageInput"
                type="file"
                accept="image/*"
                style="display: none"
                @change="handleImageUpload"
              />
            </div>
            <div class="input-box">
              <el-input
                v-model="chatInputMessage"
                type="textarea"
                :rows="3"
                placeholder="请输入消息，按回车发送..."
                @keydown.enter.prevent="sendTextMessage"
              />
              <el-button type="primary" @click="sendTextMessage" :disabled="!chatInputMessage.trim()">
                发送
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 图片预览 -->
    <el-image-viewer
      v-if="previewVisible"
      :url-list="previewImageList"
      :initial-index="previewIndex"
      @close="previewVisible = false"
    />

    <!-- 订单选择器 -->
    <el-dialog
      v-model="showOrderSelector"
      title="选择订单"
      width="500px"
      destroy-on-close
    >
      <div class="order-list" v-if="myOrders.length > 0">
        <div
          v-for="order in myOrders"
          :key="order.id"
          class="order-item"
          @click="sendOrderCard(order)"
        >
          <img :src="getOrderImageUrl(order)" class="order-thumb" />
          <div class="order-info">
            <div class="order-name">{{ order.productName || order.items?.[0]?.productName }}</div>
            <div class="order-no">订单号: {{ order.orderNo }}</div>
            <div class="order-price">¥{{ order.totalAmount }}</div>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无订单" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Message, Check, Delete, Setting, Goods, Van, ChatDotRound, User, Shop, Picture, Document, Close, Plus } from '@element-plus/icons-vue'
import { messageApi, type Message as MessageType, type UnreadCount } from '@/api/message'
import { chatApi, type ChatMessage } from '@/api/chat'
import { orderApi } from '@/api/order'
import { useUserStore } from '@/stores/user'
import { getImageUrl } from '@/utils/image'

const router = useRouter()
const userStore = useUserStore()

// 消息列表数据
const messages = ref<MessageType[]>([])
const currentType = ref(0) // 0全部, 1系统, 2商品, 3订单, 4聊天
const loading = ref(false)
const showSettings = ref(false)

// 聊天相关
const showChatPanel = ref(false)
const currentChatTarget = ref<{
  targetId?: number
  targetName?: string
  targetAvatar?: string
  shopName?: string
} | null>(null)
const chatMessages = ref<ChatMessage[]>([])
const chatInputMessage = ref('')
const chatMessageContainer = ref<HTMLElement>()
const pollingInterval = ref<number>()
const imageInput = ref<HTMLInputElement>()
const previewVisible = ref(false)
const previewImageList = ref<string[]>([])
const previewIndex = ref(0)
const showOrderSelector = ref(false)
const myOrders = ref<any[]>([])

// 各类型未读数量（从后端获取）
const unreadCounts = ref<UnreadCount>({
  total: 0,
  system: 0,
  product: 0,
  order: 0,
  chat: 0
})

// 计算属性：各类型未读数量
const typeUnreadCount = computed(() => {
  return {
    0: unreadCounts.value.total,
    1: unreadCounts.value.system,
    2: unreadCounts.value.product,
    3: unreadCounts.value.order,
    4: unreadCounts.value.chat
  }
})

// 当前类型未读数量
const currentUnreadCount = computed(() => {
  if (currentType.value === 0) {
    return unreadCounts.value.total
  }
  const map: Record<number, number> = {
    1: unreadCounts.value.system,
    2: unreadCounts.value.product,
    3: unreadCounts.value.order,
    4: unreadCounts.value.chat
  }
  return map[currentType.value] || 0
})

// 总未读消息数量
const unreadCount = computed(() => unreadCounts.value.total)

// 当前类型名称
const currentTypeName = computed(() => {
  const names = ['所有消息', '系统通知', '商品通知', '订单通知', '消息通知']
  return names[currentType.value] || '所有消息'
})

// 筛选后的消息列表
const filteredMessages = computed(() => {
  if (currentType.value === 0) {
    return messages.value
  }
  return messages.value.filter(m => m.type === currentType.value)
})

// 获取消息列表
const fetchMessages = async () => {
  loading.value = true
  try {
    // 获取消息列表
    messages.value = await messageApi.getMessageList()
    
    // 获取未读数
    unreadCounts.value = await messageApi.getUnreadCount()
  } catch (error) {
    console.error('获取消息列表失败:', error)
    ElMessage.error('获取消息列表失败')
  } finally {
    loading.value = false
  }
}

// 导航点击处理
const handleNavClick = (type: number) => {
  currentType.value = type
  // 关闭聊天面板
  closeChatPanel()
}

// 关闭聊天面板
const closeChatPanel = () => {
  showChatPanel.value = false
  currentChatTarget.value = null
  chatMessages.value = []
  stopPolling()
}

// 标记消息已读
const handleMessageClick = async (message: MessageType) => {
  // 如果是聊天消息，打开右侧聊天面板
  if (message.type === 4 && message.targetId) {
    // 标记为已读
    if (message.isRead === 0) {
      try {
        await messageApi.markAsRead(message.id)
        message.isRead = 1
        // 更新未读数
        unreadCounts.value.chat = Math.max(0, unreadCounts.value.chat - 1)
        unreadCounts.value.total = Math.max(0, unreadCounts.value.total - 1)
      } catch (error) {
        console.error('标记已读失败:', error)
      }
    }
    // 打开聊天面板
    openChatPanel(message)
    return
  }

  if (message.isRead === 1) return

  try {
    await messageApi.markAsRead(message.id)
    message.isRead = 1
    // 更新本地未读数
    const typeMap: Record<number, keyof UnreadCount> = {
      1: 'system',
      2: 'product',
      3: 'order'
    }
    const countKey = typeMap[message.type]
    if (countKey) {
      unreadCounts.value[countKey] = Math.max(0, unreadCounts.value[countKey] - 1)
      unreadCounts.value.total = Math.max(0, unreadCounts.value.total - 1)
    }
  } catch (error) {
    console.error('标记已读失败:', error)
  }
}

// 打开聊天面板
const openChatPanel = async (message: MessageType) => {
  currentChatTarget.value = {
    targetId: message.targetId,
    targetName: message.targetName || message.title,
    targetAvatar: message.targetAvatar,
    shopName: message.extra?.shopName
  }
  showChatPanel.value = true
  
  // 等待DOM渲染
  await nextTick()
  
  // 加载聊天记录
  if (message.targetId) {
    await loadChatHistory(message.targetId)
    
    // 如果 targetAvatar 为空，尝试从聊天记录中获取商家头像
    if (!currentChatTarget.value?.targetAvatar && chatMessages.value.length > 0) {
      // 找到第一条商家发的消息（senderId 不等于当前用户的消息）
      const merchantMessage = chatMessages.value.find(msg => msg.senderId !== userStore.userInfo?.id)
      if (merchantMessage?.senderAvatar) {
        currentChatTarget.value = {
          ...currentChatTarget.value!,
          targetAvatar: merchantMessage.senderAvatar
        }
      }
    }
    
    // 再次等待DOM更新后滚动到底部
    await nextTick()
    scrollToBottom()
    
    startPolling()
    // 加载订单列表（用于发送订单卡片）
    loadMyOrders()
  }
}

// 加载聊天记录
const loadChatHistory = async (targetId: number) => {
  try {
    const res: any = await chatApi.getChatHistory(targetId, 1, 100)
    console.log('加载历史消息, 消息数量:', res?.length || 0)
    if (res && res.length > 0) {
      // 按ID排序
      res.sort((a: any, b: any) => a.id - b.id)
    }
    chatMessages.value = res || []
  } catch (error) {
    console.error('加载聊天记录失败:', error)
  }
}

// 轮询获取新消息
const startPolling = () => {
  stopPolling()
  pollingInterval.value = window.setInterval(async () => {
    if (currentChatTarget.value?.targetId) {
      try {
        const res: any = await chatApi.getChatHistory(currentChatTarget.value.targetId, 1, 100)
        if (res && res.length > 0) {
          // 获取当前已存在的消息ID集合
          const existingIds = new Set(chatMessages.value.map(m => m.id))
          // 过滤出不存在的消息
          const newMessages = res.filter((msg: any) => !existingIds.has(msg.id))
          if (newMessages.length > 0) {
            // 保存当前滚动位置
            const container = chatMessageContainer.value
            const isAtBottom = container ? (container.scrollTop + container.clientHeight >= container.scrollHeight - 50) : true
            
            // 合并消息并按ID排序
            const allMessages = [...chatMessages.value, ...newMessages]
            allMessages.sort((a: any, b: any) => a.id - b.id)
            chatMessages.value = allMessages
            
            // 只有在底部或新消息是自己发的才滚动到底部
            nextTick(() => {
              if (isAtBottom || newMessages.some((msg: ChatMessage) => msg.senderId === userStore.userInfo?.id)) {
                scrollToBottom()
              }
            })
          }
        }
      } catch (error) {
        console.error('轮询消息失败:', error)
      }
    }
  }, 3000)
}

// 停止轮询
const stopPolling = () => {
  if (pollingInterval.value) {
    clearInterval(pollingInterval.value)
    pollingInterval.value = undefined
  }
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    requestAnimationFrame(() => {
      if (chatMessageContainer.value) {
        chatMessageContainer.value.scrollTop = chatMessageContainer.value.scrollHeight
      }
    })
  })
}

// 判断是否是自己发的消息
const isSelfMessage = (msg: ChatMessage) => {
  return msg.senderId === userStore.userInfo?.id
}

// 发送文本消息
const sendTextMessage = async () => {
  if (!chatInputMessage.value.trim() || !currentChatTarget.value?.targetId) return
  
  const content = chatInputMessage.value.trim()
  const tempId = Date.now()
  const targetId = currentChatTarget.value.targetId
  
  // 1. 立即在本地显示发送的消息（无感渲染）
  chatMessages.value = [...chatMessages.value, {
    id: tempId,
    senderId: userStore.userInfo?.id || 0,
    receiverId: targetId,
    type: 1,
    content: content,
    createTime: new Date().toISOString()
  }]
  chatInputMessage.value = ''
  scrollToBottom()
  
  // 2. 后台异步发送消息
  try {
    const res: any = await chatApi.sendMessage({
      receiverId: targetId,
      content: content,
      type: 1
    })
    // 更新本地消息的ID（将临时ID替换为服务器返回的真实ID）
    if (res && res.id) {
      const msgIndex = chatMessages.value.findIndex(m => m.id === tempId)
      if (msgIndex !== -1) {
        chatMessages.value[msgIndex].id = res.id
      }
    }
  } catch (error) {
    console.error('发送消息失败:', error)
    ElMessage.error('发送失败')
    // 可以选择移除本地消息或标记为发送失败
  }
}

// 触发图片上传
const triggerImageUpload = () => {
  imageInput.value?.click()
}

// 处理图片上传
const handleImageUpload = async (event: Event) => {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file || !currentChatTarget.value?.targetId) return
  
  try {
    const formData = new FormData()
    formData.append('file', file)
    
    // 上传图片
    const uploadRes = await fetch('/api/common/upload', {
      method: 'POST',
      body: formData
    }).then(res => res.json())
    
    if (uploadRes.code === 200 && uploadRes.data) {
      // 发送图片消息
      await chatApi.sendMessage({
        receiverId: currentChatTarget.value.targetId,
        content: uploadRes.data,
        type: 2
      })
      // 重新加载消息
      await loadChatHistory(currentChatTarget.value.targetId)
    }
  } catch (error) {
    console.error('发送图片失败:', error)
    ElMessage.error('发送图片失败')
  }
  
  // 清空input
  if (imageInput.value) {
    imageInput.value.value = ''
  }
}

// 预览图片
const previewImage = (url: string) => {
  previewImageList.value = [getImageUrl(url)]
  previewIndex.value = 0
  previewVisible.value = true
}

// 加载我的订单
const loadMyOrders = async () => {
  try {
    const res = await orderApi.getOrderList({ page: 1, size: 50 })
    myOrders.value = res.data?.list || []
  } catch (error) {
    console.error('加载订单失败:', error)
  }
}

// 发送订单卡片
const sendOrderCard = async (order: any) => {
  if (!currentChatTarget.value?.targetId) return
  
  try {
    await chatApi.sendMessage({
      receiverId: currentChatTarget.value.targetId,
      content: '订单卡片',
      type: 3,
      extra: {
        orderId: order.id,
        orderNo: order.orderNo,
        productName: order.productName || order.items?.[0]?.productName,
        productImage: order.productImage || order.items?.[0]?.productImage,
        price: order.totalAmount,
        statusText: getOrderStatusText(order.status)
      }
    })
    showOrderSelector.value = false
    // 重新加载消息
    await loadChatHistory(currentChatTarget.value.targetId)
  } catch (error) {
    console.error('发送订单卡片失败:', error)
    ElMessage.error('发送失败')
  }
}

// 获取订单图片URL
const getOrderImageUrl = (order: any): string => {
  const image = order.productImage || order.items?.[0]?.productImage
  return image ? getImageUrl(image) : ''
}

// 获取订单状态文本
const getOrderStatusText = (status: number) => {
  const statusMap: Record<number, string> = {
    0: '待支付',
    1: '待发货',
    2: '待收货',
    3: '已完成',
    4: '已取消',
    5: '退款中',
    6: '已退款'
  }
  return statusMap[status] || '未知状态'
}

// 跳转到订单详情
const goToOrder = (orderId?: number) => {
  if (orderId) {
    router.push(`/orders/${orderId}`)
  }
}

// 标记全部已读
const handleMarkAllRead = async () => {
  try {
    await ElMessageBox.confirm('确定要将所有消息标记为已读吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await messageApi.markAllAsRead()
    messages.value.forEach(m => m.isRead = 1)
    ElMessage.success('已全部标记为已读')
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('标记全部已读失败:', error)
    }
  }
}

// 删除消息
const handleDelete = async (id: number | string) => {
  try {
    await ElMessageBox.confirm('确定要删除这条消息吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await messageApi.deleteMessage(id)
    messages.value = messages.value.filter(m => m.id !== id)
    ElMessage.success('删除成功')
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除消息失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 清空所有消息
const handleClearAll = async () => {
  try {
    await ElMessageBox.confirm('确定要清空所有消息吗？此操作不可恢复！', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    // 逐个删除所有消息
    for (const message of filteredMessages.value) {
      await messageApi.deleteMessage(message.id)
    }
    messages.value = messages.value.filter(m => {
      if (currentType.value === 0) return false
      return m.type !== currentType.value
    })
    ElMessage.success('消息已清空')
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('清空消息失败:', error)
      ElMessage.error('清空失败')
    }
  }
}

// 格式化时间（显示时分）- 用于聊天消息
const formatTime = (time: string | undefined) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

// 格式化消息列表时间
const formatMessageTime = (time: string | undefined) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  // 小于1小时显示"X分钟前"
  if (diff < 60 * 60 * 1000) {
    const minutes = Math.floor(diff / (60 * 1000))
    return minutes < 1 ? '刚刚' : `${minutes}分钟前`
  }
  
  // 小于24小时显示"X小时前"
  if (diff < 24 * 60 * 60 * 1000) {
    const hours = Math.floor(diff / (60 * 60 * 1000))
    return `${hours}小时前`
  }
  
  // 小于7天显示"X天前"
  if (diff < 7 * 24 * 60 * 60 * 1000) {
    const days = Math.floor(diff / (24 * 60 * 60 * 1000))
    return `${days}天前`
  }
  
  // 否则显示具体日期
  return date.toLocaleDateString('zh-CN')
}

onMounted(() => {
  fetchMessages()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.messages-page {
  min-height: 100vh;
  background: white;
  padding: 0;
}

.messages-container {
  max-width: 100%;
  margin: 0;
  display: flex;
  gap: 0;
  min-height: calc(100vh - 70px);
}

/* 左侧导航 */
.sidebar {
  width: 290px;
  background: white;
  border-radius: 0;
  padding: 24px;
  display: flex;
  flex-direction: column;
  background-color: #f5f5f5;
}

.sidebar-header {
  margin-bottom: 24px;
}

.sidebar-title {
  font-size: 20px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0 0 4px;
}

.sidebar-subtitle {
  font-size: 13px;
  color: #86868b;
  margin: 0;
}

.nav-menu {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
}

.nav-item:hover {
  background: #f5f5f7;
}

.nav-item.active {
  background: #ffffff;
  color: #007aff;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  border-radius: 12px;
}

.nav-icon {
  font-size: 20px;
}

.nav-text {
  font-size: 15px;
  flex: 1;
}

.nav-badge {
  position: absolute;
  right: 12px;
}

.nav-badge :deep(.el-badge__content) {
  border: none;
}

.sidebar-footer {
  padding-top: 24px;
  border-top: 1px solid #e5e5e5;
}

/* 右侧内容 */
.content {
  flex: 1;
  background: white;
  border-radius: 0;
  padding: 24px;
  display: flex;
  flex-direction: column;
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e5e5e5;
}

.content-title {
  font-size: 18px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0;
}

.content-actions {
  display: flex;
  gap: 8px;
}

.messages-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex: 1;
  overflow-y: auto;
}

.message-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid transparent;
}

.message-item.unread {
  background: #f0f7ff;
  border-color: #007aff;
}

.message-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.icon-system {
  background: #e3f2fd;
  color: #1976d2;
}

.icon-product {
  background: #e8f5e9;
  color: #388e3c;
}

.icon-order {
  background: #fff3e0;
  color: #f57c00;
}

.icon-chat {
  background: #fce4ec;
  color: #e91e63;
}

.message-icon .el-icon {
  font-size: 22px;
}

.message-content {
  flex: 1;
  min-width: 0;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.message-title {
  font-size: 15px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-actions .message-time {
  font-size: 13px;
  color: #86868b;
  white-space: nowrap;
}

.message-text {
  font-size: 14px;
  color: #666;
  line-height: 1.5;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.message-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  flex-shrink: 0;
  margin-left: auto;
}

.unread-dot :deep(.el-badge__content) {
  width: 8px;
  height: 8px;
  padding: 0;
  border-radius: 50%;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  flex: 1;
}

.empty-icon {
  color: #c7c7cc;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 18px;
  color: #1d1d1f;
  margin: 0 0 8px;
}

.empty-subtext {
  font-size: 14px;
  color: #86868b;
  margin: 0;
}

/* 聊天面板样式 - 完全复制 ChatWindow 组件 */
.chat-window {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fff;
  overflow: hidden;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #e5e5e5;
  background: #f8f9fa;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.target-name {
  font-weight: 600;
  font-size: 16px;
  color: #333;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.chat-messages {
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  height: calc(100vh - 310px);
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.chat-messages::-webkit-scrollbar {
  display: none;
}

.message-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.message-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.message-item.self {
  flex-direction: row-reverse;
}

.message-content {
  max-width: 70%;
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-start;
}

.message-item.self .message-content {
  align-items: flex-end;
}

.text-message {
  padding: 10px 14px;
  background: #f5f5f5;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.5;
  color: #333;
  word-break: break-word;
}

.message-item.self .text-message {
  background: #007aff;
  color: #fff;
}

.image-message img {
  max-width: 200px;
  max-height: 200px;
  border-radius: 8px;
  cursor: pointer;
  object-fit: cover;
}

.card-message {
  width: 260px;
  background: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
}

.card-message:hover {
  border-color: #007aff;
}

.order-card .card-header {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: #f8f9fa;
}

.card-image {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
}

.card-info {
  flex: 1;
  min-width: 0;
}

.card-title {
  font-weight: 500;
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-desc {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border-top: 1px solid #e5e5e5;
}

.order-card .card-footer {
  margin-top: 0;
  padding: 8px 12px;
}

.card-price {
  font-weight: 600;
  color: #ff6b6b;
  font-size: 14px;
}

.card-status {
  font-size: 12px;
  color: #007aff;
}

.message-time {
  font-size: 12px;
  color: #999;
  text-align: center;
  padding: 8px 0;
}

.chat-input-area {
  border-top: 1px solid #e5e5e5;
  padding: 12px;
}

.input-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.input-box {
  display: flex;
  gap: 8px;
}

.input-box .el-textarea {
  flex: 1;
}

/* 订单选择器样式 */
.order-list {
  max-height: 400px;
  overflow-y: auto;
}

.order-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-bottom: 1px solid #e5e5e5;
  cursor: pointer;
  transition: background 0.3s;
}

.order-item:hover {
  background: #f5f5f5;
}

.order-item .order-thumb {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
}

.order-item .order-info {
  flex: 1;
}

.order-item .order-name {
  font-size: 14px;
  color: #1d1d1f;
  margin-bottom: 4px;
}

.order-item .order-no {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.order-item .order-price {
  font-size: 14px;
  color: #ff6b6b;
  font-weight: 600;
}

@media (max-width: 768px) {
  .messages-page {
    padding: 16px;
  }

  .messages-container {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
    padding: 16px;
  }

  .nav-menu {
    flex-direction: row;
    flex-wrap: wrap;
  }

  .nav-item {
    flex: 1;
    min-width: 120px;
  }

  .sidebar-footer {
    display: none;
  }

  .content {
    padding: 16px;
  }

  .content-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .message-item {
    padding: 12px;
  }

  .message-icon {
    width: 36px;
    height: 36px;
  }

  .message-icon .el-icon {
    font-size: 18px;
  }

  .message-bubble-content {
    max-width: 75%;
  }
}
</style>
