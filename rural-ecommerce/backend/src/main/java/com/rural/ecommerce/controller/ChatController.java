package com.rural.ecommerce.controller;

import com.rural.ecommerce.common.Result;
import com.rural.ecommerce.entity.ChatConversation;
import com.rural.ecommerce.entity.ChatMessage;
import com.rural.ecommerce.entity.User;
import com.rural.ecommerce.mapper.ChatConversationMapper;
import com.rural.ecommerce.mapper.ChatMessageMapper;
import com.rural.ecommerce.mapper.UserMapper;
import com.rural.ecommerce.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private ChatConversationMapper chatConversationMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 获取聊天记录
     */
    @GetMapping("/history/{targetId}")
    public Result<List<Map<String, Object>>> getChatHistory(
            @PathVariable Long targetId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }

        int offset = (page - 1) * size;
        List<ChatMessage> messages = chatMessageMapper.selectChatHistory(userId, targetId, offset, size);

        // 构建响应数据
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatMessage msg : messages) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", msg.getId());
            map.put("senderId", msg.getSenderId());
            map.put("receiverId", msg.getReceiverId());
            map.put("type", msg.getType());
            map.put("content", msg.getContent());
            map.put("extra", parseExtra(msg.getExtra()));
            map.put("isRead", msg.getIsRead());
            map.put("createTime", msg.getCreateTime());

            // 添加发送者信息
            User sender = userMapper.selectById(msg.getSenderId());
            if (sender != null) {
                map.put("senderName", sender.getNickname() != null ? sender.getNickname() : sender.getUsername());
                map.put("senderAvatar", sender.getAvatar());
            }

            result.add(map);
        }

        // 标记消息为已读
        chatMessageMapper.markAsRead(targetId, userId);
        chatConversationMapper.clearUnreadCount(userId, targetId);

        return Result.success(result);
    }

    /**
     * 获取会话列表（最近联系人）
     */
    @GetMapping("/conversations")
    public Result<List<Map<String, Object>>> getConversations(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }

        List<ChatConversation> conversations = chatConversationMapper.selectByUserId(userId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatConversation conv : conversations) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", conv.getId());
            map.put("targetId", conv.getTargetId());
            map.put("targetType", conv.getTargetType());
            map.put("unreadCount", conv.getUnreadCount());
            map.put("lastMessageTime", conv.getLastMessageTime());

            // 获取对方信息
            User target = userMapper.selectById(conv.getTargetId());
            if (target != null) {
                map.put("targetName", target.getNickname() != null ? target.getNickname() : target.getUsername());
                map.put("targetAvatar", target.getAvatar());
                map.put("shopName", target.getShopName());
            }

            // 获取最后一条消息
            if (conv.getLastMessageId() != null) {
                ChatMessage lastMsg = chatMessageMapper.selectById(conv.getLastMessageId());
                if (lastMsg != null) {
                    map.put("lastMessage", lastMsg.getContent());
                    map.put("lastMessageType", lastMsg.getType());
                }
            }

            result.add(map);
        }

        return Result.success(result);
    }

    /**
     * 获取未读消息总数
     */
    @GetMapping("/unread-count")
    public Result<Integer> getUnreadCount(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.success(0);
        }

        int count = chatMessageMapper.selectUnreadCount(userId);
        return Result.success(count);
    }

    /**
     * 标记消息为已读
     */
    @PutMapping("/read/{targetId}")
    public Result<Void> markAsRead(@PathVariable Long targetId, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }

        chatMessageMapper.markAsRead(targetId, userId);
        chatConversationMapper.clearUnreadCount(userId, targetId);

        return Result.success(null);
    }

    /**
     * 发送消息（HTTP方式，用于WebSocket不可用时）
     */
    @PostMapping("/message")
    public Result<ChatMessage> sendMessage(@RequestBody Map<String, Object> messageData, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return Result.error("请先登录");
            }

            Long receiverId = Long.valueOf(messageData.get("receiverId").toString());
            Integer type = Integer.valueOf(messageData.get("type").toString());
            String content = (String) messageData.get("content");
            String extra = null;
            if (messageData.get("extra") != null) {
                extra = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(messageData.get("extra"));
            }

            // 保存消息
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setSenderId(userId);
            chatMessage.setReceiverId(receiverId);
            chatMessage.setType(type);
            chatMessage.setContent(content);
            chatMessage.setExtra(extra);
            chatMessage.setIsRead(0);
            chatMessage.setCreateTime(java.time.LocalDateTime.now());
            chatMessageMapper.insert(chatMessage);

            // 更新或创建会话
            updateConversation(userId, receiverId, chatMessage.getId());
            updateConversation(receiverId, userId, chatMessage.getId());

            return Result.success(chatMessage);
        } catch (Exception e) {
            return Result.error("发送失败：" + e.getMessage());
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
            conversation.setLastMessageTime(java.time.LocalDateTime.now());
            conversation.setUnreadCount(1);
            chatConversationMapper.insert(conversation);
        } else {
            // 更新最后消息ID和时间，并增加未读数
            conversation.setLastMessageId(messageId);
            conversation.setLastMessageTime(java.time.LocalDateTime.now());
            conversation.setUnreadCount(conversation.getUnreadCount() + 1);
            chatConversationMapper.updateById(conversation);
        }
    }

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            try {
                return jwtUtil.getUserId(token);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 解析extra字段
     */
    private Object parseExtra(String extra) {
        if (extra == null || extra.isEmpty()) {
            return null;
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(extra, Map.class);
        } catch (Exception e) {
            return extra;
        }
    }
}
