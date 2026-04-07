<template>
  <div class="chat-window" v-if="visible">
    <!-- 头部 -->
    <div class="chat-header">
      <div class="header-info">
        <el-avatar :size="36" :src="getImageUrl(targetAvatar)" />
        <span class="target-name">{{ shopName || targetName }}</span>
      </div>
      <div class="header-actions">
        <el-button link @click="close">
          <el-icon><Close /></el-icon>
        </el-button>
      </div>
    </div>

    <!-- 消息列表 -->
    <div class="chat-messages" ref="messageContainer">
      <div
        v-for="msg in messages"
        :key="msg.id"
        class="message-group"
      >
        <!-- 时间戳 -->
        <div class="message-time">{{ formatTime(msg.createTime) }}</div>
        
        <div
          class="message-item"
          :class="{ 'self': msg.senderId == currentUserId }"
        >
          <el-avatar :size="36" :src="getImageUrl(msg.senderId == currentUserId ? currentUserAvatar : targetAvatar)" />
          <div class="message-content">
            <!-- 文本消息 -->
            <div v-if="msg.type === 1" class="text-message">{{ msg.content }}</div>
            
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
            
            <!-- 商品卡片 -->
            <div v-else-if="msg.type === 4" class="card-message product-card" @click="goToProduct(msg.extra?.productId)">
              <img :src="getImageUrl(msg.extra?.productImage)" class="card-image" />
              <div class="card-info">
                <div class="card-title">{{ msg.extra?.productName }}</div>
                <div class="card-desc" v-if="msg.extra?.productDesc">{{ msg.extra?.productDesc }}</div>
                <div class="card-footer">
                  <span class="card-price">
                    ¥{{ msg.extra?.price }}<span v-if="msg.extra?.unit">/{{ msg.extra?.unit }}</span>
                  </span>
                  <span class="card-sales">已售{{ msg.extra?.sales }}</span>
                </div>
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
              <el-dropdown-item @click="selectImage">
                <el-icon><Picture /></el-icon>发送图片
              </el-dropdown-item>
              <el-dropdown-item v-if="isBuyer" @click="selectOrder">
                <el-icon><Document /></el-icon>发送订单
              </el-dropdown-item>
              <el-dropdown-item v-if="isSeller" @click="selectProduct">
                <el-icon><Goods /></el-icon>发送商品
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <input
          ref="imageInput"
          type="file"
          accept="image/*"
          style="display: none"
          @change="handleImageSelect"
        />
      </div>
      <div class="input-box">
        <el-input
          v-model="inputMessage"
          type="textarea"
          :rows="3"
          placeholder="请输入消息，按回车发送..."
          @keydown.enter.prevent="handleEnterKey"
        />
        <el-button type="primary" @click="sendTextMessage" :loading="sending">
          发送
        </el-button>
      </div>
    </div>
  </div>

  <!-- 图片预览 -->
  <el-image-viewer
    v-if="previewVisible"
    :url-list="[previewImageUrl]"
    @close="previewVisible = false"
  />
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Close, Plus, Picture, Document, Goods } from '@element-plus/icons-vue'
import { getChatHistory, uploadChatImage, sendMessage } from '@/api/chat'
import { getImageUrl } from '@/utils/image'
import { useUserStore } from '@/stores/user'

const props = defineProps<{
  visible: boolean
  targetId: number
  targetName: string
  targetAvatar: string
  shopName?: string
  isBuyer: boolean
  isSeller: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'selectOrder'): void
  (e: 'selectProduct'): void
}>()

const router = useRouter()
const userStore = useUserStore()
const currentUserId = computed(() => userStore.userInfo?.id || 0)
const currentUserAvatar = computed(() => userStore.userInfo?.avatar || '')

const messages = ref<any[]>([])
const inputMessage = ref('')
const sending = ref(false)
const unreadCount = ref(0)
const messageContainer = ref<HTMLElement>()
const imageInput = ref<HTMLInputElement>()

const previewVisible = ref(false)
const previewImageUrl = ref('')
const pollInterval = ref<number | null>(null)

// 组件挂载
onMounted(() => {
  console.log('ChatWindow onMounted, targetId:', props.targetId, 'visible:', props.visible)
  // 延迟等待userInfo加载完成
  setTimeout(() => {
    console.log('ChatWindow setTimeout, currentUserId:', currentUserId.value)
    if (currentUserId.value && props.targetId && props.visible) {
      // 如果窗口可见，加载历史消息并启动轮询
      loadHistory().then(() => {
        startPolling()
      })
    }
  }, 100)
})

onUnmounted(() => {
  stopPolling()
})

// 监听visible变化
watch(() => props.visible, (newVal) => {
  if (newVal) {
    // 先加载历史消息，然后启动轮询
    loadHistory().then(() => {
      startPolling()
    })
  } else {
    stopPolling()
  }
})

// 启动轮询
const startPolling = () => {
  stopPolling()
  pollInterval.value = window.setInterval(() => {
    if (props.targetId) {
      loadNewMessages()
    }
  }, 1000) // 每1秒轮询一次，确保消息及时显示
}

// 停止轮询
const stopPolling = () => {
  if (pollInterval.value) {
    clearInterval(pollInterval.value)
    pollInterval.value = null
  }
}

// 加载新消息（只加载比当前最新消息更新的）
const loadNewMessages = async () => {
  console.log('轮询新消息, targetId:', props.targetId, '当前消息数:', messages.value.length)
  try {
    const res: any = await getChatHistory(props.targetId, 1, 100)
    if (res && res.length > 0) {
      console.log('API返回消息数:', res.length, '第一条ID:', res[0]?.id, '最后一条ID:', res[res.length-1]?.id)
      
      // 获取当前已存在的消息ID集合
      const existingIds = new Set(messages.value.map(m => m.id))
      console.log('本地已有消息ID:', Array.from(existingIds).sort((a, b) => a - b))
      
      // 过滤出不存在的消息
      const newMessages = res.filter((msg: any) => !existingIds.has(msg.id))
      
      console.log('新消息数量:', newMessages.length, '新消息ID:', newMessages.map((m: any) => m.id))
      
      if (newMessages.length > 0) {
        // 合并消息并按ID排序
        const allMessages = [...messages.value, ...newMessages]
        allMessages.sort((a: any, b: any) => a.id - b.id)
        messages.value = allMessages
        console.log('轮询添加新消息后，总消息数:', messages.value.length)
        // 强制刷新视图
        await nextTick()
        scrollToBottom()
      }
    }
  } catch (error) {
    console.error('轮询新消息失败:', error)
  }
}

// 监听targetId变化
watch(() => props.targetId, (newVal) => {
  if (newVal && props.visible) {
    loadHistory()
  }
})

// 监听targetId变化（当切换聊天对象时）
watch(() => props.targetId, (newVal, oldVal) => {
  if (newVal && newVal !== oldVal && props.visible) {
    console.log('targetId变化，重新加载消息:', newVal)
    messages.value = [] // 清空当前消息
    loadHistory()
  }
})

// 加载历史消息
const loadHistory = async () => {
  console.log('加载历史消息, targetId:', props.targetId)
  try {
    const res: any = await getChatHistory(props.targetId, 1, 100)
    console.log('历史消息加载成功, 消息数量:', res?.length || 0)
    if (res && res.length > 0) {
      // 按ID排序
      res.sort((a: any, b: any) => a.id - b.id)
    }
    // 强制触发响应式更新
    messages.value = [...(res || [])]
    // 强制刷新视图
    await nextTick()
    scrollToBottom()
  } catch (error) {
    console.error('加载历史消息失败:', error)
  }
}

// 处理回车键
const handleEnterKey = (e: KeyboardEvent) => {
  // 如果按下了Shift键，则插入换行
  if (e.shiftKey) {
    return
  }
  // 否则发送消息
  e.preventDefault()
  sendTextMessage()
}

// 发送文本消息
const sendTextMessage = async () => {
  if (!inputMessage.value.trim()) {
    return
  }

  const content = inputMessage.value.trim()
  const tempId = Date.now()
  
  // 1. 立即在本地显示发送的消息（无感渲染）
  messages.value = [...messages.value, {
    id: tempId,
    senderId: currentUserId.value,
    receiverId: props.targetId,
    type: 1,
    content: content,
    createTime: new Date().toISOString(),
    isRead: 0
  }]
  inputMessage.value = ''
  nextTick(() => {
    scrollToBottom()
  })
  
  // 2. 后台异步发送消息
  sendMessageAsync({
    receiverId: props.targetId,
    type: 1,
    content: content
  }, tempId)
}

// 异步发送消息（使用HTTP API）
const sendMessageAsync = async (message: any, tempId: number) => {
  try {
    const res: any = await sendMessage(message)
    console.log('消息发送成功，服务器返回:', res)
    // 更新本地消息的ID（将临时ID替换为服务器返回的真实ID）
    if (res && res.id) {
      const msgIndex = messages.value.findIndex(m => m.id === tempId)
      if (msgIndex !== -1) {
        messages.value[msgIndex].id = res.id
        messages.value[msgIndex].createTime = res.createTime
        console.log('更新消息ID:', tempId, '->', res.id)
      }
    }
  } catch (error) {
    console.error('消息发送失败:', error)
    ElMessage.error('消息发送失败，请检查网络')
  }
}

// 选择图片
const selectImage = () => {
  imageInput.value?.click()
}

// 处理图片选择
const handleImageSelect = async (event: Event) => {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return

  try {
    sending.value = true
    const res: any = await uploadChatImage(file)
    const imageUrl = res.data?.url || res.url
    const tempId = Date.now()
    
    // 1. 立即在本地显示发送的图片（无感渲染）
    messages.value = [...messages.value, {
      id: tempId,
      senderId: currentUserId.value,
      receiverId: props.targetId,
      type: 2,
      content: imageUrl,
      createTime: new Date().toISOString(),
      isRead: 0
    }]
    nextTick(() => {
      scrollToBottom()
    })
    
    // 2. 后台异步发送消息
    sendMessageAsync({
      receiverId: props.targetId,
      type: 2,
      content: imageUrl
    }, tempId)
  } catch (error) {
    ElMessage.error('图片发送失败')
  } finally {
    sending.value = false
    if (imageInput.value) {
      imageInput.value.value = ''
    }
  }
}

// 选择订单
const selectOrder = () => {
  emit('selectOrder')
}

// 选择商品
const selectProduct = () => {
  emit('selectProduct')
}

// 预览图片
const previewImage = (url: string) => {
  previewImageUrl.value = getImageUrl(url)
  previewVisible.value = true
}

// 跳转到订单详情
const goToOrder = (orderId?: number) => {
  if (orderId) {
    router.push(`/order/${orderId}`)
  }
}

// 跳转到商品详情
const goToProduct = (productId?: number) => {
  if (productId) {
    router.push(`/product/${productId}`)
  }
}

// 滚动到底部
const scrollToBottom = () => {
  if (messageContainer.value) {
    messageContainer.value.scrollTop = messageContainer.value.scrollHeight
  }
}

// 格式化时间
const formatTime = (time: string) => {
  const date = new Date(time)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

// 关闭
const close = () => {
  emit('update:visible', false)
  // 注意：不要断开WebSocket，保持连接以便接收消息
}

// 暴露方法给父组件
defineExpose({
  sendOrderCard: async (order: any) => {
    const tempId = Date.now()
    
    // 1. 立即在本地显示发送的订单卡片（无感渲染）
    messages.value = [...messages.value, {
      id: tempId,
      senderId: currentUserId.value,
      receiverId: props.targetId,
      type: 3,
      content: `订单：${order.orderNo}`,
      extra: order,
      createTime: new Date().toISOString(),
      isRead: 0
    }]
    nextTick(() => {
      scrollToBottom()
    })
    
    // 2. 后台异步发送消息
    sendMessageAsync({
      receiverId: props.targetId,
      type: 3,
      content: `订单：${order.orderNo}`,
      extra: order
    }, tempId)
  },
  sendProductCard: async (product: any) => {
    const tempId = Date.now()
    
    // 1. 立即在本地显示发送的商品卡片（无感渲染）
    messages.value = [...messages.value, {
      id: tempId,
      senderId: currentUserId.value,
      receiverId: props.targetId,
      type: 4,
      content: product.productName,
      extra: product,
      createTime: new Date().toISOString(),
      isRead: 0
    }]
    nextTick(() => {
      scrollToBottom()
    })
    
    // 2. 后台异步发送消息
    sendMessageAsync({
      receiverId: props.targetId,
      type: 4,
      content: product.productName,
      extra: product
    }, tempId)
  }
})
</script>

<style scoped>
.chat-window {
  position: fixed;
  right: 24px;
  bottom: 24px;
  width: 400px;
  height: 600px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  z-index: 1000;
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
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
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

.product-card {
  padding: 12px;
}

.product-card .card-image {
  width: 100%;
  height: 120px;
  object-fit: cover;
  border-radius: 4px;
  margin-bottom: 8px;
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

.card-subtitle {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
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

.card-sales {
  font-size: 12px;
  color: #999;
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

/* 响应式 */
@media (max-width: 768px) {
  .chat-window {
    right: 0;
    bottom: 0;
    width: 100%;
    height: 100%;
    border-radius: 0;
  }
}
</style>
