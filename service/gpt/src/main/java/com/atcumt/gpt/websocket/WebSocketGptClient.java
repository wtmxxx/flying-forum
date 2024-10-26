package com.atcumt.gpt.websocket;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class WebSocketGptClient extends TextWebSocketHandler {
    private final WebSocketClient client;
    @Value("${gpt.uri}:${gpt.port}")
    private String uri;
    //    private String content;
    private WebSocketSession gptSession;
    private WebSocketSession userSession;

    public void connect(WebSocketSession userSession) {
        WebSocketConnectionManager manager = new WebSocketConnectionManager(client, this, uri);
        manager.start();
        this.userSession = userSession;
    }

    public void disconnect() throws Exception {
        if (gptSession != null && gptSession.isOpen()) {
            gptSession.close();
        }
    }

    public void sendMessage(String message) throws Exception {
        if (gptSession != null && gptSession.isOpen()) {
            gptSession.sendMessage(new TextMessage(message));
        } else {
            throw new IllegalStateException("WebSocket is not connected.");
        }
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        this.gptSession = session; // 处理连接后保存session
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws IOException {
//        System.out.println("Received: " + message.getPayload());
        this.userSession.sendMessage(message);

        // TODO 处理消息到数据库
    }
}
