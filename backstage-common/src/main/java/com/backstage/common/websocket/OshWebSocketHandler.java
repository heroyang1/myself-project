package com.backstage.common.websocket;

import com.alibaba.fastjson2.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 原生 WebSocket 处理器
 * 连接地址：ws://host/ws/connect?token=xxx
 * 推送：sendToUser(userId, payload)
 */
@Component
public class OshWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(OshWebSocketHandler.class);

    /** userId -> WebSocketSession */
    private final ConcurrentHashMap<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /**
     * 持久化回调，由 system 模块实现并注入
     * required = false：common 模块单独启动时不报错
     */
    @Autowired(required = false)
    private WsMessagePersistHandler persistHandler;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = getUserId(session);
        if (userId == null) {
            close(session);
            return;
        }
        sessions.put(userId, session);
        log.info("WS 连接建立，userId={}, sessionId={}", userId, session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = getUserId(session);
        if (userId != null) {
            sessions.remove(userId);
            log.info("WS 连接关闭，userId={}", userId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        if ("ping".equals(message.getPayload())) {
            try {
                session.sendMessage(new TextMessage("pong"));
            } catch (Exception ignored) {
                // 心跳回复失败通常是连接已断开，忽略即可，后续读操作会触发 onClose
            }
        }
    }

    /**
     * 推送消息给指定用户
     * 无论用户是否在线、推送是否成功，都写入 MySQL
     */
    public void sendToUser(Long userId, Object payload) {
        // 1. 先持久化（无论在线与否）
        persist(userId, payload);

        // 2. 用户不在线则跳过推送
        WebSocketSession session = sessions.get(userId);
        if (session == null || !session.isOpen()) {
            log.debug("WS 推送跳过：用户不在线，userId={}", userId);
            return;
        }

        // 3. 推送
        try {
            session.sendMessage(new TextMessage(JSON.toJSONString(payload)));
            log.info("WS 推送成功，userId={}", userId);
        } catch (Exception e) {
            log.error("WS 推送失败，userId={}，原因={}", userId, e.getMessage());
        }
    }

    public boolean isOnline(Long userId) {
        WebSocketSession s = sessions.get(userId);
        return s != null && s.isOpen();
    }

    /**
     * 广播消息给所有在线用户
     * 广播消息不持久化（公告性质，不需要写库）
     */
    public void broadcast(Object payload) {
        if (sessions.isEmpty()) return;
        String json = JSON.toJSONString(payload);
        TextMessage message = new TextMessage(json);
        int success = 0, fail = 0;
        for (WebSocketSession session : sessions.values()) {
            if (session == null || !session.isOpen()) continue;
            try {
                session.sendMessage(message);
                success++;
            } catch (Exception e) {
                fail++;
                log.warn("WS 广播失败，sessionId={}，原因={}", session.getId(), e.getMessage());
            }
        }
        log.info("WS 广播完成，成功={}，失败={}", success, fail);
    }

    /** 当前在线人数 */
    public int getOnlineCount() {
        return (int) sessions.values().stream().filter(s -> s != null && s.isOpen()).count();
    }

    private void persist(Long userId, Object payload) {
        if (persistHandler == null) return;
        try {
            persistHandler.persist(userId, payload);
        } catch (Exception e) {
            log.error("WS 消息持久化失败，userId={}，原因={}", userId, e.getMessage());
        }
    }

    private Long getUserId(WebSocketSession session) {
        Object val = session.getAttributes().get("userId");
        return val instanceof Long ? (Long) val : null;
    }

    private void close(WebSocketSession session) {
        // 会话可能已关闭，重复关闭抛异常属预期，忽略
        try { session.close(CloseStatus.NOT_ACCEPTABLE); } catch (Exception ignored) { }
    }
}
