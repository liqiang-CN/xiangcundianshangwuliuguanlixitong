<template>
  <div class="messages-view">
    <h2 class="page-title">消息中心</h2>

    <el-tabs v-model="activeTab" class="message-tabs">
      <el-tab-pane label="聊天消息" name="chat">
        <div v-loading="chatLoading" class="chat-list">
          <div v-if="conversations.length === 0" class="empty-state">
            <el-empty description="暂无聊天消息" />
          </div>
          <div
            v-for="conv in conversations"
            :key="conv.targetId"
            class="chat-item"
            @click="openChat(conv)"
          >
            <el-avatar :size="48" :src="getImageUrl(conv.targetAvatar)" />
            <div class="chat-content">
              <div class="chat-header">
                <h4 class="chat-name">{{ conv.targetName }}</h4>
                <span class="chat-time">{{ formatTime(conv.lastMessageTime) }}</span>
              </div>
              <p class="chat-preview">{{ getLastMessagePreview(conv) }}</p>
            </div>
            <el-badge v-if="conv.unreadCount > 0" :value="conv.unreadCount" class="unread-badge" />
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="系统通知" name="system">
        <div v-loading="loading" class="message-list">
          <div v-if="systemMessages.length === 0" class="empty-state">
            <el-empty description="暂无系统通知" />
          </div>
          <div
            v-for="msg in systemMessages"
            :key="msg.id"
            class="message-item"
            :class="{ unread: !msg.isRead }"
            @click="readSystemMessage(msg)"
          >
            <div class="message-icon system">
              <el-icon size="24"><InfoFilled /></el-icon>
            </div>
            <div class="message-content">
              <h4 class="message-title">{{ msg.title }}</h4>
              <p class="message-desc">{{ msg.content }}</p>
              <span class="message-time">{{ formatTime(msg.createTime) }}</span>
            </div>
            <el-badge v-if="!msg.isRead" is-dot class="unread-badge" />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 聊天窗口 -->
    <ChatWindow
      v-if="currentChat.targetId"
      ref="chatWindowRef"
      v-model:visible="chatVisible"
      :target-id="currentChat.targetId"
      :target-name="currentChat.targetName"
      :target-avatar="currentChat.targetAvatar"
      :is-buyer="false"
      :is-seller="true"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { InfoFilled } from '@element-plus/icons-vue'
import { getConversations, markAsRead } from '@/api/chat'
import { getMessageList, markAsRead as markSystemRead } from '@/api/message'
import { getImageUrl } from '@/utils/image'
import { useUserStore } from '@/stores/user'
import ChatWindow from '@/components/ChatWindow.vue'

const userStore = useUserStore()
userStore.initUserInfo()

const activeTab = ref('chat')
const chatLoading = ref(false)
const loading = ref(false)
const conversations = ref<any[]>([])
const systemMessages = ref<any[]>([])
const chatVisible = ref(false)
const currentChat = ref({
  targetId: 0,
  targetName: '',
  targetAvatar: ''
})
const chatWindowRef = ref<InstanceType<typeof ChatWindow>>()
const pollInterval = ref<number | null>(null)

// 启动轮询
const startPolling = () => {
  stopPolling()
  pollInterval.value = window.setInterval(() => {
    fetchConversations()
  }, 2000) // 每2秒轮询一次会话列表，确保消息及时显示
}

// 停止轮询
const stopPolling = () => {
  if (pollInterval.value) {
    clearInterval(pollInterval.value)
    pollInterval.value = null
  }
}

// 获取最后消息预览
const getLastMessagePreview = (conv: any) => {
  if (!conv.lastMessage) return ''
  switch (conv.lastMessageType) {
    case 1: return conv.lastMessage
    case 2: return '[图片]'
    case 3: return '[订单卡片]'
    case 4: return '[商品卡片]'
    default: return conv.lastMessage
  }
}

// 格式化时间
const formatTime = (time: string) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  // 小于1小时显示分钟前
  if (diff < 3600000) {
    const minutes = Math.floor(diff / 60000)
    return minutes < 1 ? '刚刚' : `${minutes}分钟前`
  }
  // 小于24小时显示小时前
  if (diff < 86400000) {
    return `${Math.floor(diff / 3600000)}小时前`
  }
  // 小于7天显示天数前
  if (diff < 604800000) {
    return `${Math.floor(diff / 86400000)}天前`
  }
  // 否则显示具体日期
  return date.toLocaleDateString('zh-CN')
}

// 获取聊天会话列表
const fetchConversations = async () => {
  chatLoading.value = true
  try {
    const res: any = await getConversations()
    conversations.value = res || []
  } catch (error) {
    console.error('获取会话列表失败:', error)
  } finally {
    chatLoading.value = false
  }
}

// 获取系统通知列表
const fetchSystemMessages = async () => {
  loading.value = true
  try {
    const res: any = await getMessageList()
    systemMessages.value = res || []
  } catch (error) {
    console.error('获取系统通知失败:', error)
  } finally {
    loading.value = false
  }
}

// 打开聊天窗口
const openChat = async (conv: any) => {
  // 先设置聊天对象信息
  currentChat.value = {
    targetId: conv.targetId,
    targetName: conv.targetName,
    targetAvatar: conv.targetAvatar
  }
  
  // 等待DOM更新后再显示窗口
  await nextTick()
  chatVisible.value = true

  // 标记消息为已读
  if (conv.unreadCount > 0) {
    try {
      await markAsRead(conv.targetId)
      conv.unreadCount = 0
    } catch (error) {
      console.error('标记已读失败:', error)
    }
  }
}

// 标记系统消息为已读
const readSystemMessage = async (msg: any) => {
  if (!msg.isRead) {
    try {
      await markSystemRead(msg.id)
      msg.isRead = 1
      ElMessage.success('已标记为已读')
    } catch (error) {
      console.error('标记已读失败:', error)
    }
  }
}

onMounted(() => {
  fetchConversations()
  fetchSystemMessages()
  startPolling()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.messages-view {
  padding-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--apple-dark-gray);
  margin-bottom: 24px;
}

.message-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

/* 聊天列表样式 */
.chat-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chat-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: var(--apple-card-bg);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
}

.chat-item:hover {
  box-shadow: var(--apple-shadow);
}

.chat-content {
  flex: 1;
  min-width: 0;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.chat-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--apple-dark-gray);
}

.chat-time {
  font-size: 12px;
  color: #999;
}

.chat-preview {
  font-size: 14px;
  color: var(--apple-gray);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 系统通知样式 */
.message-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.empty-state {
  padding: 40px 0;
}

.message-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 20px;
  background: var(--apple-card-bg);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
}

.message-item:hover {
  box-shadow: var(--apple-shadow);
}

.message-item.unread {
  background: linear-gradient(to right, rgba(0, 122, 255, 0.05), var(--apple-card-bg));
}

.message-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.message-icon.system {
  background: #ff9500;
}

.message-content {
  flex: 1;
}

.message-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--apple-dark-gray);
  margin-bottom: 8px;
}

.message-desc {
  font-size: 14px;
  color: var(--apple-gray);
  margin-bottom: 8px;
  line-height: 1.5;
}

.message-time {
  font-size: 12px;
  color: #999;
}

.unread-badge {
  position: absolute;
  top: 16px;
  right: 16px;
}
</style>
