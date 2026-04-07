package com.rural.ecommerce.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rural.ecommerce.entity.ChatConversation;
import com.rural.ecommerce.entity.ChatMessage;
import com.rural.ecommerce.mapper.ChatConversationMapper;
import com.rural.ecommerce.mapper.ChatMessageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 存储用户ID与WebSocket会话的映射
    private static final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private ChatConversationMapper chatConversationMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            userSessions.put(userId, session);
            log.info("用户 {} 已连接聊天WebSocket，当前在线用户数: {}", userId, userSessions.size());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            userSessions.remove(userId);
            log.info("用户 {} 已断开聊天WebSocket，当前在线用户数: {}", userId, userSessions.size());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        
        // 处理心跳消息
        if ("ping".equals(payload)) {
            session.sendMessage(new TextMessage("pong"));
            return;
        }

        // 解析聊天消息
        try {
            Map<String, Object> msgData = objectMapper.readValue(payload, Map.class);
            Long senderId = getUserIdFromSession(session);
            Long receiverId = Long.valueOf(msgData.get("receiverId").toString());
            Integer type = Integer.valueOf(msgData.get("type").toString());
            String content = (String) msgData.get("content");
            String extra = msgData.get("extra") != null ? objectMapper.writeValueAsString(msgData.get("extra")) : null;

            // 保存消息到数据库
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setSenderId(senderId);
            chatMessage.setReceiverId(receiverId);
            chatMessage.setType(type);
            chatMessage.setContent(content);
            chatMessage.setExtra(extra);
            chatMessage.setIsRead(0);
            chatMessage.setCreateTime(LocalDateTime.now());
            chatMessageMapper.insert(chatMessage);

            // 更新或创建会话
            updateConversation(senderId, receiverId, chatMessage.getId());
            updateConversation(receiverId, senderId, chatMessage.getId());

            // 构建响应消息
            Map<String, Object> response = Map.of(
                "id", chatMessage.getId(),
                "senderId", senderId,
                "receiverId", receiverId,
                "type", type,
                "content", content,
                "extra", msgData.get("extra"),
                "createTime", chatMessage.getCreateTime().toString()
            );

            String responseJson = objectMapper.writeValueAsString(response);

            // 发送给接收者（如果在线）
            WebSocketSession receiverSession = userSessions.get(receiverId);
            if (receiverSession != null && receiverSession.isOpen()) {
                receiverSession.sendMessage(new TextMessage(responseJson));
            }

            // 发送确认给发送者
            session.sendMessage(new TextMessage(responseJson));

        } catch (Exception e) {
            log.error("处理聊天消息失败", e);
            session.sendMessage(new TextMessage("{\"error\":\"消息处理失败\"}"));
        }
    }

    // 更新会话信息
    private void updateConversation(Long userId, Long targetId, Long messageId) {
        ChatConversation conversation = chatConversationMapper.selectByUserAndTarget(userId, targetId);
        if (conversation == null) {
            conversation = new ChatConversation();
            conversation.setUserId(userId);
            conversation.setTargetId(targetId);
            conversation.setTargetType(1);
            conversation.setLastMessageId(messageId);
            conversation.setLastMessageTime(LocalDateTime.now());
            conversation.setUnreadCount(1);
            chatConversationMapper.insert(conversation);
        } else {
            // 更新最后消息ID和时间，并增加未读数
            conversation.setLastMessageId(messageId);
            conversation.setLastMessageTime(LocalDateTime.now());
            conversation.setUnreadCount(conversation.getUnreadCount() + 1);
            chatConversationMapper.updateById(conversation);
        }
    }

    // 从session中获取用户ID
    private Long getUserIdFromSession(WebSocketSession session) {
        try {
            String query = session.getUri().getQuery();
            if (query != null && query.contains("userId=")) {
                String userIdStr = query.substring(query.indexOf("userId=") + 7);
                if (userIdStr.contains("&")) {
                    userIdStr = userIdStr.substring(0, userIdStr.indexOf("&"));
                }
                return Long.valueOf(userIdStr);
            }
        } catch (Exception e) {
            log.error("获取用户ID失败", e);
        }
        return null;
    }

    // 发送消息给指定用户（供其他服务调用）
    public void sendMessageToUser(Long userId, String message) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                log.error("发送消息给用户 {} 失败", userId, e);
            }
        }
    }
}
