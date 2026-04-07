package com.rural.ecommerce.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rural.ecommerce.common.Result;
import com.rural.ecommerce.entity.ChatMessage;
import com.rural.ecommerce.entity.User;
import com.rural.ecommerce.mapper.ChatMessageMapper;
import com.rural.ecommerce.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/admin/chat")
public class AdminChatController {

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 获取所有聊天会话列表
     */
    @GetMapping("/sessions")
    public Result<Map<String, Object>> getAllSessions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {

        // 查询所有聊天消息，按时间倒序
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        
        // 时间范围筛选
        if (startTime != null && !startTime.isEmpty()) {
            wrapper.ge(ChatMessage::getCreateTime, startTime);
        }
        if (endTime != null && !endTime.isEmpty()) {
            wrapper.le(ChatMessage::getCreateTime, endTime);
        }
        
        wrapper.orderByDesc(ChatMessage::getCreateTime);
        
        List<ChatMessage> allMessages = chatMessageMapper.selectList(wrapper);
        
        // 按会话分组
        Map<String, Map<String, Object>> sessionMap = new LinkedHashMap<>();
        
        for (ChatMessage msg : allMessages) {
            // 生成会话唯一标识（较小的ID在前）
            String sessionKey = msg.getSenderId() < msg.getReceiverId() 
                ? msg.getSenderId() + "_" + msg.getReceiverId()
                : msg.getReceiverId() + "_" + msg.getSenderId();
            
            if (!sessionMap.containsKey(sessionKey)) {
                Map<String, Object> session = new HashMap<>();
                session.put("sessionKey", sessionKey);
                session.put("userId", msg.getSenderId() < msg.getReceiverId() ? msg.getSenderId() : msg.getReceiverId());
                session.put("targetId", msg.getSenderId() < msg.getReceiverId() ? msg.getReceiverId() : msg.getSenderId());
                session.put("lastMessageTime", msg.getCreateTime());
                session.put("lastMessage", msg.getContent());
                session.put("lastMessageType", msg.getType());
                session.put("messageCount", 1);
                
                // 获取用户信息
                User user = userMapper.selectById((Long) session.get("userId"));
                User target = userMapper.selectById((Long) session.get("targetId"));
                
                if (user != null) {
                    session.put("userName", user.getNickname() != null ? user.getNickname() : user.getUsername());
                    session.put("userAvatar", user.getAvatar());
                }
                if (target != null) {
                    session.put("targetName", target.getNickname() != null ? target.getNickname() : target.getUsername());
                    session.put("targetAvatar", target.getAvatar());
                    session.put("targetShopName", target.getShopName());
                    session.put("targetType", target.getUserType());
                }
                
                sessionMap.put(sessionKey, session);
            } else {
                Map<String, Object> session = sessionMap.get(sessionKey);
                session.put("messageCount", (Integer) session.get("messageCount") + 1);
            }
        }
        
        // 关键词筛选
        List<Map<String, Object>> sessions = new ArrayList<>(sessionMap.values());
        if (keyword != null && !keyword.isEmpty()) {
            sessions.removeIf(s -> {
                String userName = (String) s.get("userName");
                String targetName = (String) s.get("targetName");
                String targetShopName = (String) s.get("targetShopName");
                String lastMessage = (String) s.get("lastMessage");
                return (userName == null || !userName.contains(keyword))
                    && (targetName == null || !targetName.contains(keyword))
                    && (targetShopName == null || !targetShopName.contains(keyword))
                    && (lastMessage == null || !lastMessage.contains(keyword));
            });
        }
        
        // 分页
        int total = sessions.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);
        List<Map<String, Object>> pageList = start < total ? sessions.subList(start, end) : new ArrayList<>();
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", pageList);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        
        return Result.success(result);
    }

    /**
     * 查看指定会话的聊天记录
     */
    @GetMapping("/history")
    public Result<List<Map<String, Object>>> getSessionHistory(
            @RequestParam Long userId,
            @RequestParam Long targetId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {

        int offset = (page - 1) * size;
        
        // 查询两个用户之间的所有消息
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(ChatMessage::getSenderId, userId).eq(ChatMessage::getReceiverId, targetId)
                .or()
                .eq(ChatMessage::getSenderId, targetId).eq(ChatMessage::getReceiverId, userId))
            .orderByAsc(ChatMessage::getCreateTime);
        
        List<ChatMessage> messages = chatMessageMapper.selectList(wrapper);
        
        // 手动分页
        int total = messages.size();
        int start = offset;
        int end = Math.min(start + size, total);
        List<ChatMessage> pageList = start < total ? messages.subList(start, end) : new ArrayList<>();
        
        // 构建响应数据
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatMessage msg : pageList) {
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
                map.put("senderType", sender.getUserType());
            }
            
            result.add(map);
        }
        
        return Result.success(result);
    }

    /**
     * 搜索聊天记录
     */
    @GetMapping("/search")
    public Result<List<Map<String, Object>>> searchMessages(
            @RequestParam String keyword,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long targetId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        
        // 关键词搜索
        wrapper.like(ChatMessage::getContent, keyword);
        
        // 指定用户
        if (userId != null) {
            wrapper.and(w -> w.eq(ChatMessage::getSenderId, userId).or().eq(ChatMessage::getReceiverId, userId));
        }
        
        // 指定会话
        if (targetId != null && userId != null) {
            wrapper.and(w -> w.and(w1 -> w1.eq(ChatMessage::getSenderId, userId).eq(ChatMessage::getReceiverId, targetId))
                    .or(w2 -> w2.eq(ChatMessage::getSenderId, targetId).eq(ChatMessage::getReceiverId, userId)));
        }
        
        // 时间范围
        if (startTime != null && !startTime.isEmpty()) {
            wrapper.ge(ChatMessage::getCreateTime, startTime);
        }
        if (endTime != null && !endTime.isEmpty()) {
            wrapper.le(ChatMessage::getCreateTime, endTime);
        }
        
        wrapper.orderByDesc(ChatMessage::getCreateTime);
        
        List<ChatMessage> messages = chatMessageMapper.selectList(wrapper);
        
        // 手动分页
        int total = messages.size();
        int offset = (page - 1) * size;
        int start = offset;
        int end = Math.min(start + size, total);
        List<ChatMessage> pageList = start < total ? messages.subList(start, end) : new ArrayList<>();
        
        // 构建响应数据
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatMessage msg : pageList) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", msg.getId());
            map.put("senderId", msg.getSenderId());
            map.put("receiverId", msg.getReceiverId());
            map.put("type", msg.getType());
            map.put("content", msg.getContent());
            map.put("createTime", msg.getCreateTime());
            
            // 添加发送者信息
            User sender = userMapper.selectById(msg.getSenderId());
            if (sender != null) {
                map.put("senderName", sender.getNickname() != null ? sender.getNickname() : sender.getUsername());
            }
            
            // 添加接收者信息
            User receiver = userMapper.selectById(msg.getReceiverId());
            if (receiver != null) {
                map.put("receiverName", receiver.getNickname() != null ? receiver.getNickname() : receiver.getUsername());
            }
            
            result.add(map);
        }
        
        return Result.success(result);
    }

    /**
     * 获取聊天统计
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {

        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        
        if (startTime != null && !startTime.isEmpty()) {
            wrapper.ge(ChatMessage::getCreateTime, startTime);
        }
        if (endTime != null && !endTime.isEmpty()) {
            wrapper.le(ChatMessage::getCreateTime, endTime);
        }
        
        long totalMessages = chatMessageMapper.selectCount(wrapper);
        
        // 统计各类型消息数量
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMessages", totalMessages);
        
        // 文本消息
        LambdaQueryWrapper<ChatMessage> textWrapper = new LambdaQueryWrapper<>();
        if (startTime != null && !startTime.isEmpty()) textWrapper.ge(ChatMessage::getCreateTime, startTime);
        if (endTime != null && !endTime.isEmpty()) textWrapper.le(ChatMessage::getCreateTime, endTime);
        textWrapper.eq(ChatMessage::getType, 1);
        stats.put("textMessages", chatMessageMapper.selectCount(textWrapper));
        
        // 图片消息
        LambdaQueryWrapper<ChatMessage> imageWrapper = new LambdaQueryWrapper<>();
        if (startTime != null && !startTime.isEmpty()) imageWrapper.ge(ChatMessage::getCreateTime, startTime);
        if (endTime != null && !endTime.isEmpty()) imageWrapper.le(ChatMessage::getCreateTime, endTime);
        imageWrapper.eq(ChatMessage::getType, 2);
        stats.put("imageMessages", chatMessageMapper.selectCount(imageWrapper));
        
        // 订单卡片
        LambdaQueryWrapper<ChatMessage> orderWrapper = new LambdaQueryWrapper<>();
        if (startTime != null && !startTime.isEmpty()) orderWrapper.ge(ChatMessage::getCreateTime, startTime);
        if (endTime != null && !endTime.isEmpty()) orderWrapper.le(ChatMessage::getCreateTime, endTime);
        orderWrapper.eq(ChatMessage::getType, 3);
        stats.put("orderMessages", chatMessageMapper.selectCount(orderWrapper));
        
        // 商品卡片
        LambdaQueryWrapper<ChatMessage> productWrapper = new LambdaQueryWrapper<>();
        if (startTime != null && !startTime.isEmpty()) productWrapper.ge(ChatMessage::getCreateTime, startTime);
        if (endTime != null && !endTime.isEmpty()) productWrapper.le(ChatMessage::getCreateTime, endTime);
        productWrapper.eq(ChatMessage::getType, 4);
        stats.put("productMessages", chatMessageMapper.selectCount(productWrapper));
        
        return Result.success(stats);
    }

    /**
     * 删除单条消息
     */
    @DeleteMapping("/message/{id}")
    public Result<Void> deleteMessage(@PathVariable Long id) {
        chatMessageMapper.deleteById(id);
        return Result.success(null);
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
