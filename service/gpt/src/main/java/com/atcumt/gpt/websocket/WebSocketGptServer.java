package com.atcumt.gpt.websocket;

import cn.hutool.json.JSONUtil;
import com.atcumt.gpt.mapper.ConversationMapper;
import com.atcumt.gpt.mapper.MessageMapper;
import com.atcumt.model.gpt.constants.MessageRole;
import com.atcumt.model.gpt.dto.MessageDTO;
import com.atcumt.model.gpt.entity.Conversation;
import com.atcumt.model.gpt.entity.Message;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class WebSocketGptServer extends TextWebSocketHandler {
    private final WebSocketGptClient webSocketGptClient;
    private final RedisTemplate<String, String> redisTemplate;
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;

    @Autowired
    @Lazy
    public WebSocketGptServer(WebSocketGptClient webSocketGptClient,
                              RedisTemplate<String, String> redisTemplate,
                              ConversationMapper conversationMapper,
                              MessageMapper messageMapper
    ) {
        this.webSocketGptClient = webSocketGptClient;
        this.redisTemplate = redisTemplate;
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
    }

//    private WebSocketSession session;

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        // 连接建立时的逻辑
        log.info("和{}客户端建立连接", session.getId());
//        this.session = session;
//        webSocketGptClient.connect(session);
    }

    @Override
    @Transactional
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        // 处理文本消息
        log.info("收到来自{}客户端的信息:{}", session.getId(), message);

        MessageDTO messageDTO = JSONUtil.toBean(message.getPayload(), MessageDTO.class);

        String conversationId = messageDTO.getConversationId();
        String content = messageDTO.getContent();

        session.getAttributes().put("conversationId", conversationId);

        // 处理消息到数据库
        // 记录插入时间，比当前时间早1秒
        LocalDateTime insertTime = LocalDateTime.now().minusSeconds(1);
        // 将MessageDTO转换为Message实体
        Message userMessage = Message
                .builder()
                .conversationId(conversationId)
                .content(content)
                .role(MessageRole.HUMAN)
                .createTime(insertTime)
                .updateTime(insertTime)
                .build();

        // 更新会话的最后更新时间
        LambdaUpdateWrapper<Conversation> conversationUpdateWrapper = new LambdaUpdateWrapper<>();
        conversationUpdateWrapper
                .eq(Conversation::getId, conversationId)
                .set(Conversation::getUpdateTime, LocalDateTime.now());
        conversationMapper.update(conversationUpdateWrapper);
        if (content != null && !content.isEmpty()) messageMapper.insert(userMessage);  // 插入用户消息

        session.getAttributes().put("userMessageId", userMessage.getId());

        redisTemplate.opsForValue().set("gptWebSocket:" + conversationId, content == null ? "" : content);

        redisTemplate.expire("gptWebSocket:" + conversationId, 2, TimeUnit.HOURS);

        webSocketGptClient.connect(session, conversationId);

//        webSocketGptClient.sendMessage(content, conversationId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        // 连接关闭时的逻辑
        System.out.println("Connection closed: " + session.getId());
        webSocketGptClient.disconnect((String) session.getAttributes().get("conversationId"));
    }
}
