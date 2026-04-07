package com.rural.ecommerce.controller;

import com.rural.ecommerce.entity.ChatConversation;
import com.rural.ecommerce.entity.ChatMessage;
import com.rural.ecommerce.entity.User;
import com.rural.ecommerce.entity.UserMessage;
import com.rural.ecommerce.mapper.ChatConversationMapper;
import com.rural.ecommerce.mapper.ChatMessageMapper;
import com.rural.ecommerce.repository.UserMessageRepository;
import com.rural.ecommerce.service.UserService;
import com.rural.ecommerce.util.JwtUtil;
import com.rural.ecommerce.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/message")
public class MessageController {

    @Autowired
    private UserMessageRepository userMessageRepository;

    @Autowired
    private ChatConversationMapper chatConversationMapper;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    private Long getCurrentUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            return jwtUtil.getUserId(token);
        }
        return null;
    }

    /**
     * 获取消息列表（包含系统通知、商品通知、订单通知、聊天记录）
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getMessageList(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }

        List<Map<String, Object>> result = new ArrayList<>();

        // 1. 获取用户消息（系统、商品、订单通知）
        List<UserMessage> userMessages = userMessageRepository.findByUserId(userId);
        for (UserMessage msg : userMessages) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", msg.getId());
            map.put("userId", msg.getUserId());
            map.put("title", msg.getTitle());
            map.put("content", msg.getContent());
            map.put("type", msg.getType());
            map.put("isRead", msg.getIsRead());
            map.put("createTime", msg.getCreateTime());
            result.add(map);
        }

        // 2. 获取聊天记录（按会话分组，取最后一条消息）
        List<ChatConversation> conversations = chatConversationMapper.selectByUserId(userId);
        for (ChatConversation conv : conversations) {
            // 获取最后一条消息
            ChatMessage lastMessage = chatMessageMapper.selectLastMessage(userId, conv.getTargetId());
            if (lastMessage != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", "chat_" + conv.getTargetId()); // 使用chat_前缀区分
                map.put("userId", userId);
                
                // 获取对方信息
                User targetUser = userService.getById(conv.getTargetId());
                String targetName = targetUser != null ? 
                    (targetUser.getShopName() != null ? targetUser.getShopName() : targetUser.getUsername()) : "未知用户";
                
                map.put("title", targetName);
                map.put("content", lastMessage.getContent());
                map.put("type", 4); // 消息通知类型
                map.put("isRead", conv.getUnreadCount() > 0 ? 0 : 1);
                map.put("createTime", lastMessage.getCreateTime());
                map.put("targetId", conv.getTargetId()); // 用于跳转聊天
                result.add(map);
            }
        }

        // 按时间倒序排列
        result.sort((a, b) -> {
            Object timeA = a.get("createTime");
            Object timeB = b.get("createTime");
            if (timeA == null || timeB == null) return 0;
            return timeB.toString().compareTo(timeA.toString());
        });

        return Result.success(result);
    }

    /**
     * 获取未读消息数量
     */
    @GetMapping("/unread-count")
    public Result<Map<String, Integer>> getUnreadCount(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            Map<String, Integer> empty = new HashMap<>();
            empty.put("total", 0);
            empty.put("system", 0);
            empty.put("product", 0);
            empty.put("order", 0);
            empty.put("chat", 0);
            return Result.success(empty);
        }

        Map<String, Integer> counts = new HashMap<>();
        
        // 用户消息未读数
        int userMsgUnread = userMessageRepository.countUnreadByUserId(userId);
        
        // 各类型未读数
        int systemUnread = userMessageRepository.countUnreadByType(userId, 1);
        int productUnread = userMessageRepository.countUnreadByType(userId, 2);
        int orderUnread = userMessageRepository.countUnreadByType(userId, 3);
        
        // 聊天未读数
        int chatUnread = chatConversationMapper.selectTotalUnreadCount(userId);
        
        counts.put("total", userMsgUnread + chatUnread);
        counts.put("system", systemUnread);
        counts.put("product", productUnread);
        counts.put("order", orderUnread);
        counts.put("chat", chatUnread);
        
        return Result.success(counts);
    }

    /**
     * 标记消息已读
     */
    @PostMapping("/read/{id}")
    public Result<Void> markAsRead(@PathVariable String id, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }

        // 判断是聊天消息还是用户消息
        if (id.startsWith("chat_")) {
            // 标记聊天记录为已读
            Long targetId = Long.parseLong(id.substring(5));
            chatMessageMapper.markAsRead(targetId, userId);
            // 更新会话未读数
            chatConversationMapper.resetUnreadCount(userId, targetId);
        } else {
            // 标记用户消息为已读
            Long msgId = Long.parseLong(id);
            UserMessage message = userMessageRepository.selectById(msgId);
            if (message == null || !message.getUserId().equals(userId)) {
                return Result.error("消息不存在");
            }
            message.setIsRead(1);
            userMessageRepository.updateById(message);
        }
        
        return Result.success();
    }

    /**
     * 标记所有消息已读
     */
    @PostMapping("/read-all")
    public Result<Void> markAllAsRead(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }
        
        // 标记所有用户消息为已读
        userMessageRepository.markAllAsRead(userId);
        
        // 标记所有聊天消息为已读
        chatConversationMapper.markAllAsRead(userId);
        
        return Result.success();
    }

    /**
     * 删除消息
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteMessage(@PathVariable String id, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }

        if (id.startsWith("chat_")) {
            // 删除聊天记录（逻辑删除会话）
            Long targetId = Long.parseLong(id.substring(5));
            chatConversationMapper.deleteByUserIdAndTargetId(userId, targetId);
        } else {
            // 删除用户消息
            Long msgId = Long.parseLong(id);
            UserMessage message = userMessageRepository.selectById(msgId);
            if (message == null || !message.getUserId().equals(userId)) {
                return Result.error("消息不存在");
            }
            userMessageRepository.deleteById(msgId);
        }
        
        return Result.success();
    }

    /**
     * 发送系统通知（内部调用）
     */
    public void sendSystemMessage(Long userId, String title, String content) {
        UserMessage message = new UserMessage();
        message.setUserId(userId);
        message.setTitle(title);
        message.setContent(content);
        message.setType(1);
        message.setIsRead(0);
        userMessageRepository.insert(message);
    }

    /**
     * 发送商品通知（关注店铺上新）
     */
    public void sendProductMessage(Long userId, String shopName, String productName) {
        UserMessage message = new UserMessage();
        message.setUserId(userId);
        message.setTitle(shopName + " 上新了");
        message.setContent("您关注的店铺 " + shopName + " 发布了新商品：" + productName + "，快来看看吧！");
        message.setType(2);
        message.setIsRead(0);
        userMessageRepository.insert(message);
    }

    /**
     * 发送订单通知
     */
    public void sendOrderMessage(Long userId, String orderNo, String status) {
        UserMessage message = new UserMessage();
        message.setUserId(userId);
        message.setTitle("订单状态更新");
        message.setContent("您的订单 " + orderNo + " " + status);
        message.setType(3);
        message.setIsRead(0);
        userMessageRepository.insert(message);
    }
}
