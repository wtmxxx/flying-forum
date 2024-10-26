package com.atcumt.gpt.websocket;

import com.atcumt.model.gpt.constants.WebSocketType;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class WebSocketGptClient extends TextWebSocketHandler {
    private final WebSocketClient client;
    private final RedisTemplate<String, Map<String, WebSocketSession>> redisTemplate;
    @Value("${gpt.uri}:${gpt.port}/{conversationId}")
    private String uri;

    public void connect(WebSocketSession userSession, String conversationId) {
        WebSocketConnectionManager manager = new WebSocketConnectionManager(client, this, uri, conversationId);
        manager.start();
        redisTemplate.opsForHash().put("gptWebSocket:" + conversationId, WebSocketType.USER, userSession);
        redisTemplate.expire("gptWebSocket:" + conversationId, 1, TimeUnit.HOURS);
    }

    public void disconnect(WebSocketSession userSession) throws Exception {
        // 获取ConversationId
        String conversationId = getConversationId(userSession);
        WebSocketSession gptSession = (WebSocketSession) redisTemplate.opsForHash().get("gptWebSocket:" + conversationId, WebSocketType.GPT);

        if (gptSession != null && gptSession.isOpen()) {
            gptSession.close();
        }

//        redisTemplate.delete("gptWebSocket:" + conversationId);
    }

    public void sendMessage(String message, String conversationId) throws Exception {
        WebSocketSession gptSession = (WebSocketSession) redisTemplate.opsForHash().get("gptWebSocket:" + conversationId, WebSocketType.GPT);

        if (gptSession != null && gptSession.isOpen()) {
            gptSession.sendMessage(new TextMessage(message));
        } else {
            throw new IllegalStateException("WebSocket is not connected.");
        }
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession gptSession) {
        // 获取ConversationId
        String conversationId = getConversationId(gptSession);

        // Redis插入Session
        redisTemplate.opsForHash().put("gptWebSocket:" + conversationId, WebSocketType.GPT, gptSession);
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession gptSession, @NonNull TextMessage message) throws IOException {
//        System.out.println("Received: " + message.getPayload());
        // 获取ConversationId
        String conversationId = getConversationId(gptSession);
        WebSocketSession userSession = (WebSocketSession) redisTemplate.opsForHash().get("gptWebSocket:" + conversationId, WebSocketType.USER);
        if (userSession != null) {
            userSession.sendMessage(message);
        } else {
            throw new IllegalStateException("Redis got WebSocket null for conversationId.");
        }

        // TODO 处理消息到数据库
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        // 连接关闭时的逻辑
        System.out.println("Connection closed: " + session.getId());
    }


    private String getConversationId(WebSocketSession gptSession) {
        // 获取查询路径
        String path = Objects.requireNonNull(gptSession.getUri()).getPath();  // "/{conversationId}"

        // 路径格式为 /{conversationId}
        String[] segments = path.split("/");

        // 获取 conversationId （位于路径的最后一段）
        return segments[segments.length - 1];
    }
}
