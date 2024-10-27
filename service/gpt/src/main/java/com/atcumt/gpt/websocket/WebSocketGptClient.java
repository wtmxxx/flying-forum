package com.atcumt.gpt.websocket;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.atcumt.gpt.mapper.ConversationMapper;
import com.atcumt.gpt.mapper.MessageMapper;
import com.atcumt.model.common.Result;
import com.atcumt.model.gpt.constants.MessageRole;
import com.atcumt.model.gpt.constants.WebSocketType;
import com.atcumt.model.gpt.dto.ConversationGptDTO;
import com.atcumt.model.gpt.entity.Conversation;
import com.atcumt.model.gpt.entity.Message;
import com.atcumt.model.gpt.vo.MessageVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class WebSocketGptClient extends TextWebSocketHandler {
    private final WebSocketClient client;
    //    private final RedisTemplate<String, Map<String, WebSocketSession>> redisTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    @Value("${gpt.uri}:${gpt.port}")
    private String uri;
    // 缓存每个会话的Session
    private Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    // 缓存每个会话的累加消息
    private Map<String, StringBuilder> messageCache = new ConcurrentHashMap<>();

    public void connect(WebSocketSession userSession, String conversationId) {
        WebSocketConnectionManager manager = new WebSocketConnectionManager(client, this, uri, conversationId);

        // 创建 HttpHeaders 对象
        HttpHeaders httpHeaders = new HttpHeaders();

        // 添加自定义头部信息，例如Authorization、Custom-Header等
        httpHeaders.set("X-Conversation-ID", conversationId);


        manager.setHeaders(new WebSocketHttpHeaders(httpHeaders));
        manager.start();
//        redisTemplate.opsForHash().put("gptWebSocket:" + conversationId, WebSocketType.USER, userSession);
//        redisTemplate.expire("gptWebSocket:" + conversationId, 1, TimeUnit.HOURS);

        sessions.put(conversationId + ":" + WebSocketType.USER, userSession);
    }

    public void disconnect(String conversationId) throws Exception {
        // 获取ConversationId
//        String conversationId = getConversationId(userSession);
//        WebSocketSession gptSession = (WebSocketSession) redisTemplate.opsForHash().get("gptWebSocket:" + conversationId, WebSocketType.GPT);

        WebSocketSession gptSession = sessions.get(conversationId + ":" + WebSocketType.GPT);
        if (gptSession != null && gptSession.isOpen()) {
            gptSession.close();
        }

        sessions.remove(conversationId + ":" + WebSocketType.GPT);
        sessions.remove(conversationId + ":" + WebSocketType.USER);

//        redisTemplate.delete("gptWebSocket:" + conversationId);
    }

    public void sendMessage(WebSocketSession gptSession, String conversationId) throws Exception {
//        WebSocketSession gptSession = (WebSocketSession) redisTemplate.opsForHash().get("gptWebSocket:" + conversationId, WebSocketType.GPT);

//        WebSocketSession gptSession = sessions.get(conversationId + ":" + WebSocketType.GPT);

        String message = redisTemplate.opsForValue().get("gptWebSocket:" + conversationId);

        if (message == null) {
            throw new IllegalStateException("Redis does not contain message.");
        }

        if (gptSession != null && gptSession.isOpen()) {
            // 获取所有历史消息并构造 GPT 请求
            LambdaQueryWrapper<Message> messageQueryWrapper = new LambdaQueryWrapper<>();
            messageQueryWrapper
                    .eq(Message::getConversationId, conversationId)
                    .orderByAsc(Message::getUpdateTime);
            List<Message> messages = messageMapper.selectList(messageQueryWrapper);

            // 获取会话信息并封装成DTO对象
            Conversation conversation = conversationMapper.selectById(conversationId);
            ConversationGptDTO conversationGptDTO = BeanUtil.toBean(conversation, ConversationGptDTO.class);
            conversationGptDTO.setMessages(messages);

            gptSession.sendMessage(new TextMessage(JSONUtil.toJsonStr(conversationGptDTO)));
            redisTemplate.delete("gptWebSocket:" + conversationId);
        } else {
            throw new IllegalStateException("WebSocket is not connected.");
        }
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession gptSession) throws Exception {
        // 获取ConversationId
        String conversationId = getConversationId(gptSession);

        // 初始化缓存
        messageCache.put(conversationId, new StringBuilder());

        // Redis插入Session
//        redisTemplate.opsForHash().put("gptWebSocket:" + conversationId, WebSocketType.GPT, gptSession);
        // Map插入Session
        sessions.put(conversationId + ":" + WebSocketType.GPT, gptSession);

        sendMessage(gptSession, conversationId);
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession gptSession, @NonNull TextMessage message) throws IOException {
//        System.out.println("Received: " + message.getPayload());
        // 获取ConversationId
        String conversationId = getConversationId(gptSession);
//        WebSocketSession userSession = (WebSocketSession) redisTemplate.opsForHash().get("gptWebSocket:" + conversationId, WebSocketType.USER);
        WebSocketSession userSession = sessions.get(conversationId + ":" + WebSocketType.USER);

        String messageType = JSONUtil.parseObj(message.getPayload()).getStr(WebSocketType.MESSAGE_TYPE);

        // 累加消息到缓存
        StringBuilder conversationCache = messageCache.get(conversationId);

        MessageVO messageVO = new MessageVO();
        if (userSession != null) {
            if (messageType.equals(WebSocketType.CONTENT)) {
                if (conversationCache != null) {
                    conversationCache.append(JSONUtil.parseObj(message.getPayload()).getStr(WebSocketType.CONTENT));
                }

                // 返回每个流式块给前端
//                messageVO.setId(uuid);
                messageVO.setLastMessageId(userSession.getAttributes().get("userMessageId").toString());
                messageVO.setContent(JSONUtil.parseObj(message.getPayload()).get(WebSocketType.CONTENT).toString());
                messageVO.setConversationId(conversationId);
                messageVO.setRole(MessageRole.AI);
                messageVO.setCreateTime(LocalDateTime.now());
                messageVO.setUpdateTime(LocalDateTime.now());
                userSession.sendMessage(new TextMessage(JSONUtil.toJsonStr(
                        Result.success(
                                Map.of(
                                        WebSocketType.MESSAGE_TYPE, WebSocketType.CONTENT,
                                        WebSocketType.DATA, messageVO
                                )
                        )
                )));
            } else if (messageType.equals(WebSocketType.CITATIONS)) {
                Object citations = JSONUtil.parseObj(message.getPayload()).get(WebSocketType.CITATIONS);

                String uuid = UUID.randomUUID().toString(); // 使用UUID算法生成唯一ID

                Message gptReply = Message
                        .builder()
                        .id(uuid)
                        .role(MessageRole.AI)
                        .conversationId(conversationId)
                        .content(conversationCache.toString())  // 完整拼接后的内容
                        .citations(citations.toString())
                        .createTime(LocalDateTime.now())
                        .updateTime(LocalDateTime.now())
                        .build();

                messageMapper.insert(gptReply);  // 插入GPT消息

                userSession.sendMessage(new TextMessage(JSONUtil.toJsonStr(
                        Result.success(
                                Map.of(
                                        WebSocketType.MESSAGE_TYPE, WebSocketType.CITATIONS,
                                        WebSocketType.MESSAGE_ID, uuid,
                                        WebSocketType.DATA, citations
                                )
                        )
                )));

            } else {
                throw new IllegalStateException("Unknown message type: " + messageType);
            }
        } else {
            throw new IllegalStateException("Map got WebSocket null for conversationId.");
        }

    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        // 连接关闭时的逻辑
        System.out.println("Connection closed: " + session.getId());

        disconnect(getConversationId(session));
    }


    private String getConversationId(WebSocketSession gptSession) {
        return Objects.requireNonNull(gptSession.getHandshakeHeaders().get("X-Conversation-ID")).getFirst();
        // 获取查询路径
//        String path = Objects.requireNonNull(gptSession.getUri()).getPath();  // "/{conversationId}"

        // 路径格式为 /{conversationId}
//        String[] segments = path.split("/");

        // 获取 conversationId （位于路径的最后一段）
//        return segments[segments.length - 1];
    }
}
