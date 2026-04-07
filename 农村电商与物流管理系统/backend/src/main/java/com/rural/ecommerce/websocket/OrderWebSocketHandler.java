package com.rural.ecommerce.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class OrderWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 存储农户ID与WebSocket会话的映射
    private static final Map<Long, WebSocketSession> farmerSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long farmerId = getFarmerIdFromSession(session);
        if (farmerId != null) {
            farmerSessions.put(farmerId, session);
            log.info("农户 {} 已连接WebSocket，当前在线农户数: {}", farmerId, farmerSessions.size());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long farmerId = getFarmerIdFromSession(session);
        if (farmerId != null) {
            farmerSessions.remove(farmerId);
            log.info("农户 {} 已断开WebSocket，当前在线农户数: {}", farmerId, farmerSessions.size());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 处理心跳消息
        String payload = message.getPayload();
        if ("ping".equals(payload)) {
            session.sendMessage(new TextMessage("pong"));
        }
    }

    /**
     * 向指定农户发送新订单通知
     */
    public void sendNewOrderNotification(Long farmerId, Object orderData) {
        log.info("尝试向农户 {} 发送新订单通知，当前在线农户: {}", farmerId, farmerSessions.keySet());
        WebSocketSession session = farmerSessions.get(farmerId);
        if (session != null && session.isOpen()) {
            try {
                Map<String, Object> message = new HashMap<>();
                message.put("type", "NEW_ORDER");
                message.put("data", orderData);
                String jsonMessage = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(jsonMessage));
                log.info("已向农户 {} 发送新订单通知", farmerId);
            } catch (IOException e) {
                log.error("向农户 {} 发送WebSocket消息失败", farmerId, e);
            }
        } else {
            log.warn("农户 {} 不在线或连接已关闭，无法发送通知", farmerId);
        }
    }

    /**
     * 从会话中获取农户ID
     */
    private Long getFarmerIdFromSession(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.contains("farmerId=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("farmerId=")) {
                    try {
                        return Long.parseLong(param.substring(9));
                    } catch (NumberFormatException e) {
                        log.error("解析农户ID失败: {}", param);
                    }
                }
            }
        }
        return null;
    }
}
