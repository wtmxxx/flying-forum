package com.atcumt.gpt.websocket;

import cn.hutool.json.JSONUtil;
import com.atcumt.model.gpt.dto.MessageDTO;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Service
@Slf4j
public class WebSocketGptServer extends TextWebSocketHandler {
    private final WebSocketGptClient webSocketGptClient;

    @Autowired
    @Lazy
    public WebSocketGptServer(WebSocketGptClient webSocketGptClient) {
        this.webSocketGptClient = webSocketGptClient;
    }

//    private WebSocketSession session;

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        // 连接建立时的逻辑
        log.info("和{}客户端建立连接", session.getId());
//        this.session = session;
        webSocketGptClient.connect(session);
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        // 处理文本消息
        log.info("收到来自{}客户端的信息:{}", session.getId(), message);

        MessageDTO messageDTO = JSONUtil.toBean(message.getPayload(), MessageDTO.class);

        String conversationId = messageDTO.getConversationId();
        String content = messageDTO.getContent();

        // TODO 处理消息到数据库

        webSocketGptClient.sendMessage(content);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        // 连接关闭时的逻辑
        System.out.println("Connection closed: " + session.getId());
        webSocketGptClient.disconnect();
    }
}
