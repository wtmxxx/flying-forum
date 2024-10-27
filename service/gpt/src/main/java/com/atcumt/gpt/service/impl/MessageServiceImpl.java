package com.atcumt.gpt.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONObject;
import com.atcumt.gpt.mapper.ConversationMapper;
import com.atcumt.gpt.mapper.MessageMapper;
import com.atcumt.gpt.service.MessageService;
import com.atcumt.model.gpt.constants.MessageRole;
import com.atcumt.model.gpt.dto.ConversationGptDTO;
import com.atcumt.model.gpt.dto.MessageDTO;
import com.atcumt.model.gpt.entity.Conversation;
import com.atcumt.model.gpt.entity.Message;
import com.atcumt.model.gpt.vo.MessageVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {
    // 消息相关的数据库操作Mapper
    private final MessageMapper messageMapper;
    // 会话相关的数据库操作Mapper
    private final ConversationMapper conversationMapper;
    // 用于同步HTTP请求的RestTemplate
    private final RestTemplate restTemplate;
    // 用于响应式HTTP请求（支持流式数据）的WebClient
    private final WebClient webClient;

    // GPT服务的URI（从应用程序配置中加载）
    @Value("${cumt-forum.gpt.uri}")
    private String gptUri;
    // GPT服务的端口（从应用程序配置中加载）
    @Value("${cumt-forum.gpt.port}")
    private String gptPort;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MessageVO processChatStuff(MessageDTO messageDTO) {
        // 记录插入时间，比当前时间早1秒
        LocalDateTime insertTime = LocalDateTime.now().minusSeconds(1);
        // 将MessageDTO对象转换为Message实体
        Message userMessage = BeanUtil.toBean(messageDTO, Message.class);
        // 设置消息角色为"HUMAN"，表示用户消息
        userMessage.setRole(MessageRole.HUMAN);
        // 设置消息的创建和更新时间
        userMessage.setCreateTime(insertTime);
        userMessage.setUpdateTime(insertTime);

        // 更新会话更新时间，表示该会话有新消息
        LambdaUpdateWrapper<Conversation> conversationUpdateWrapper = new LambdaUpdateWrapper<>();
        conversationUpdateWrapper
                .eq(Conversation::getId, messageDTO.getConversationId())
                .set(Conversation::getUpdateTime, LocalDateTime.now());
        // 更新数据库中的会话记录
        conversationMapper.update(conversationUpdateWrapper);

        // 根据会话ID查询历史消息，按照更新时间升序排列
        LambdaQueryWrapper<Message> messageQueryWrapper = new LambdaQueryWrapper<>();
        messageQueryWrapper
                .eq(Message::getConversationId, messageDTO.getConversationId())
                .orderByAsc(Message::getUpdateTime);
        List<Message> messages = messageMapper.selectList(messageQueryWrapper);
        // 将当前用户消息添加到历史消息列表中
        messages.add(userMessage);

        // 获取会话信息
        Conversation conversation = conversationMapper.selectById(messageDTO.getConversationId());

        // 将会话信息和消息列表封装到DTO对象中，用于向GPT服务发送请求
        ConversationGptDTO conversationGptDTO = BeanUtil.toBean(conversation, ConversationGptDTO.class);
        conversationGptDTO.setMessages(messages);

        String title = conversationGptDTO.getTitle(); // 会话标题，可用于识别会话上下文

        // 使用RestTemplate发送请求到GPT服务，获取GPT的回复
        ResponseEntity<JSONObject> responseMessage = restTemplate
                .postForEntity(gptUri + ":" + gptPort + "/gpt/chat", conversationGptDTO, JSONObject.class);

        // 检查GPT响应的状态码是否成功
        if (responseMessage.getStatusCode().is2xxSuccessful()) {
            // 将响应内容提取为字符串类型
            String responseBody = Objects.requireNonNull(responseMessage.getBody()).get("content", String.class);

            // 创建 GPT 的回复消息对象
            Message gptReply = Message
                    .builder()
                    .role(MessageRole.AI) // 设置角色为"AI"，表示GPT的回复
                    .conversationId(messageDTO.getConversationId())
                    .content(responseBody) // 设置回复内容
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();

            // 将用户消息和GPT回复消息插入到数据库中
            messageMapper.insert(userMessage);
            messageMapper.insert(gptReply);

            // 添加上一个消息的ID(用户提问ID)
            MessageVO messageVO = BeanUtil.copyProperties(gptReply, MessageVO.class);
            messageVO.setLastMessageId(userMessage.getId());

            // 将GPT的回复封装为VO对象并返回
            return BeanUtil.copyProperties(messageVO, MessageVO.class);
        } else {
            // 如果GPT服务调用失败，抛出异常
            throw new RuntimeException("GPT服务调用失败(Message)，状态码：" + responseMessage.getStatusCode());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Flux<MessageVO> processChatStreamFlux(MessageDTO messageDTO) {
        // 记录插入时间，比当前时间早1秒
        LocalDateTime insertTime = LocalDateTime.now().minusSeconds(1);
        // 将MessageDTO转换为Message实体
        Message userMessage = BeanUtil.toBean(messageDTO, Message.class);
        userMessage.setRole(MessageRole.HUMAN);
        userMessage.setCreateTime(insertTime);
        userMessage.setUpdateTime(insertTime);

        // 更新会话的最后更新时间
        LambdaUpdateWrapper<Conversation> conversationUpdateWrapper = new LambdaUpdateWrapper<>();
        conversationUpdateWrapper
                .eq(Conversation::getId, messageDTO.getConversationId())
                .set(Conversation::getUpdateTime, LocalDateTime.now());
        conversationMapper.update(conversationUpdateWrapper);

        // 获取所有历史消息并构造 GPT 请求
        LambdaQueryWrapper<Message> messageQueryWrapper = new LambdaQueryWrapper<>();
        messageQueryWrapper
                .eq(Message::getConversationId, messageDTO.getConversationId())
                .orderByAsc(Message::getUpdateTime);
        List<Message> messages = messageMapper.selectList(messageQueryWrapper);
        messages.add(userMessage);

        // 获取会话信息并封装成DTO对象
        Conversation conversation = conversationMapper.selectById(messageDTO.getConversationId());
        ConversationGptDTO conversationGptDTO = BeanUtil.toBean(conversation, ConversationGptDTO.class);
        conversationGptDTO.setMessages(messages);

        // 接收Python流式输出并拼接到字符串中
        StringBuilder gptResponseContent = new StringBuilder();
        String uuid = UUID.randomUUID().toString(); // 使用UUID算法生成唯一ID
        return this.streamChatData(conversationGptDTO)
                .doOnNext(chunk -> gptResponseContent.append(chunk.getContent()))  // 拼接所有块的内容
                .doOnComplete(() -> {
                    // 流结束时，将完整的GPT回复保存到数据库
                    messageMapper.insert(userMessage);  // 插入用户消息

                    Message gptReply = Message
                            .builder()
                            .id(uuid)
                            .role(MessageRole.AI)
                            .conversationId(messageDTO.getConversationId())
                            .content(gptResponseContent.toString())  // 完整拼接后的内容
                            .createTime(LocalDateTime.now())
                            .updateTime(LocalDateTime.now())
                            .build();
                    messageMapper.insert(gptReply);  // 插入数据库
                })
                .map(chunk -> {
                    // 返回每个流式块给前端
                    MessageVO messageVO = new MessageVO();
                    messageVO.setId(uuid);
                    messageVO.setLastMessageId(userMessage.getId());
                    messageVO.setContent(chunk.getContent());
                    messageVO.setConversationId(messageDTO.getConversationId());
                    messageVO.setRole(MessageRole.AI);
                    messageVO.setCreateTime(LocalDateTime.now());
                    messageVO.setUpdateTime(LocalDateTime.now());
                    return messageVO;
                });
    }

    // 使用WebClient与GPT服务交互，接收流式的消息块
    public Flux<Message> streamChatData(ConversationGptDTO conversationGptDTO) {
        return this.webClient.post()
                .uri("/gpt/chat")
                .bodyValue(conversationGptDTO) // 设置请求体为会话DTO对象
                .retrieve()
                .bodyToFlux(Message.class);  // 使用Flux接收流式输出，将其转换为Message对象
    }

}
