package com.atcumt.ai.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.atcumt.ai.ai.ChatMessageStore;
import com.atcumt.ai.ai.WotemoTokenWindowChatMemory;
import com.atcumt.ai.ai.WotemoTokenizer;
import com.atcumt.ai.service.AiService;
import com.atcumt.ai.utils.SseEmitterRegistry;
import com.atcumt.common.utils.UserContext;
import com.atcumt.model.ai.dto.ConversationDTO;
import com.atcumt.model.ai.dto.StopConversationDTO;
import com.atcumt.model.ai.dto.StopConversationMsgDTO;
import com.atcumt.model.ai.dto.TitleDTO;
import com.atcumt.model.ai.entity.Conversation;
import com.atcumt.model.ai.entity.Message;
import com.atcumt.model.ai.enums.AiStatus;
import com.atcumt.model.ai.vo.*;
import com.atcumt.model.common.dto.PageQueryDTO;
import com.atcumt.model.common.vo.MediaFileVO;
import com.atcumt.model.common.vo.SimplePageQueryVO;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiServiceImpl implements AiService {
    private final SseEmitterRegistry sseEmitterRegistry;
    private final ChatMessageStore chatMessageStore;
    private final MongoTemplate mongoTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    private final RedissonClient redissonClient;
    private final RocketMQTemplate rocketMQTemplate;
    private WotemoTokenizer tokenizer = new WotemoTokenizer(MAX_TOKENS);
    private StreamingChatLanguageModel deepseekReasonerModel;
    private StreamingChatLanguageModel deepseekChatModel;
    private StreamingChatLanguageModel qwenModel;
    private StreamingChatLanguageModel qwqModel;
    private StreamingChatLanguageModel deepseekR1Model;

    private static final int MAX_TOKENS = 20000;

    @Value("${langchain4j.ollama.chat-model.base-url}")
    private String ollamaChatModelBaseUrl;
    @Value("${langchain4j.openai.chat-model.deepseek.base-url}")
    private String deepseekBaseUrl;
    @Value("${langchain4j.openai.chat-model.deepseek.api-key}")
    private String deepseekApiKey;

    @PostConstruct
    void init() {
        // 初始化大模型
        deepseekChatModel = OpenAiStreamingChatModel.builder()
                .baseUrl(deepseekBaseUrl)
                .apiKey(deepseekApiKey)
                .defaultRequestParameters(OpenAiChatRequestParameters.builder()
                        .modelName("deepseek-chat")
//                        .responseFormat(ResponseFormat.JSON)
                        .build())
//                .strictJsonSchema(true)
                .logRequests(true)
                .logResponses(true)
                .build();

        deepseekReasonerModel = OpenAiStreamingChatModel.builder()
                .baseUrl(deepseekBaseUrl)
                .apiKey(deepseekApiKey)
                .defaultRequestParameters(OpenAiChatRequestParameters.builder()
                        .modelName("deepseek-reasoner")
//                        .responseFormat(ResponseFormat.JSON)
                        .build())
//                .strictJsonSchema(true)
                .logRequests(true)
                .logResponses(true)
                .build();

        qwenModel = OllamaStreamingChatModel.builder()
                .baseUrl(ollamaChatModelBaseUrl)
                .modelName("qwen2.5:7b")
//                .supportedCapabilities(Set.of(Capability.RESPONSE_FORMAT_JSON_SCHEMA))
//                .responseFormat(ResponseFormat.JSON)
                .customHeaders(Map.of("Content-Type", "application/json; charset=UTF-8"))
                .logRequests(true)
                .logResponses(true)
                .build();

        deepseekR1Model = OllamaStreamingChatModel.builder()
                .baseUrl(ollamaChatModelBaseUrl)
                .modelName("deepseek-r1:7b")
//                .supportedCapabilities(Set.of(Capability.RESPONSE_FORMAT_JSON_SCHEMA))
//                .responseFormat(ResponseFormat.JSON)
                .customHeaders(Map.of("Content-Type", "application/json; charset=UTF-8"))
                .logRequests(true)
                .logResponses(true)
                .build();

        qwqModel = OllamaStreamingChatModel.builder()
                .baseUrl(ollamaChatModelBaseUrl)
                .modelName("qwq:32b")
//                .supportedCapabilities(Set.of(Capability.RESPONSE_FORMAT_JSON_SCHEMA))
//                .responseFormat(ResponseFormat.JSON)
                .customHeaders(Map.of("Content-Type", "application/json; charset=UTF-8"))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Override
    public SseEmitter conversation(ConversationDTO conversationDTO) throws Exception {
        int estimatedTokens = tokenizer.estimateTokenCountInText(conversationDTO.getContent());
        if (estimatedTokens > MAX_TOKENS) {
            throw new RuntimeException("你输入的内容太长了，请缩短后重试");
        }

        String userId = UserContext.getUserId();
        boolean newConversation = conversationDTO.getConversationId() == null || conversationDTO.getConversationId().isEmpty();
        if (newConversation) conversationDTO.setConversationId(IdUtil.simpleUUID());
        String conversationId = conversationDTO.getConversationId();

        RLock conversationLock = redissonClient.getLock("ai:conversation_lock:" + conversationId);
        if (!newConversation && sseEmitterRegistry.contains(UserContext.getUserId(), conversationId)) {
            throw new RuntimeException("回答正在生成中，请稍后再试");
        } else if (!conversationLock.tryLock(3, TimeUnit.SECONDS)) {
            throw new RuntimeException("回答正在生成中，请稍后再试");
        }

        try {
            SseEmitter sseEmitter;

            if (newConversation) {
                // 新建对话
                chatMessageStore.createEmptyConversation(conversationId, userId);

                NewConversationVO newConversationVO = NewConversationVO
                        .builder()
                        .type("newConversation")
                        .conversationId(conversationId)
                        .messageId(1)
                        .parentId(0)
                        .build();
                sseEmitter = sseEmitterRegistry.create(UserContext.getUserId(), conversationId);
                sseEmitter.send(SseEmitter.event()
                        .id(conversationId)
                        .name("newConversation")
                        .comment("新建对话")
                        .data(newConversationVO)
                );
                generateTitle(sseEmitter, conversationDTO);
            } else {
                sseEmitter = sseEmitterRegistry.create(UserContext.getUserId(), conversationId);
            }

            WotemoTokenWindowChatMemory chatMemory = WotemoTokenWindowChatMemory
                    .builder()
                    .maxTokens(MAX_TOKENS, tokenizer)
                    .chatMemoryStore(chatMessageStore)
                    .id(conversationDTO)
                    .build();

            Conversation conversation = chatMessageStore.getConversation(conversationId, userId);
            if (conversation == null) {
                throw new RuntimeException("对话不存在");
            }
            if (conversation.getMessages().size() > 100) {
                sseEmitterRegistry.remove(userId, conversationId);
                throw new RuntimeException("对话消息太多了，请新建对话");
            }

            int currentMessageId = conversation.getCurrentMessageId();
            int parentId;

            if (conversationDTO.getParentId() == null
                    || conversationDTO.getParentId() < 0
                    || conversationDTO.getParentId() > currentMessageId
            ) {
                parentId = currentMessageId;
            } else {
                parentId = conversationDTO.getParentId();
            }

            List<ChatMessage> historyMessages = chatMessageStore.toChatMessages(parentId, conversation.getMessages());
            for (ChatMessage historyMessage : historyMessages) {
                chatMemory.add(historyMessage);
            }

            String modelName = "deepseek-chat";
            StreamingChatLanguageModel chatModel = deepseekChatModel;
            if (conversationDTO.getReasoningEnabled()) {
                modelName = "deepseek-reasoner";
                chatModel = deepseekR1Model;
            }
            final String finalModelName = modelName;

            int userMessageId = currentMessageId + 1;
            int aiMessageId = currentMessageId + 2;
            Message storeUserMessage = Message
                    .builder()
                    .messageId(userMessageId)
                    .parentId(parentId)
                    .model(finalModelName)
                    .role("user")
                    .content(conversationDTO.getContent())
                    .reasoningEnabled(conversationDTO.getReasoningEnabled())
                    .reasoningContent(null)
                    .reasoningTime(-1)
                    .reasoningStatus(AiStatus.OTHER.getValue())
                    .searchEnabled(conversationDTO.getSearchEnabled())
                    .searchResults(List.of())
                    .searchStatus(AiStatus.OTHER.getValue())
                    .files(List.of())
                    .status(AiStatus.OTHER.getValue())
                    .createTime(LocalDateTime.now())
                    .build();
            chatMessageStore.updateMessage(conversation.getConversationId(), storeUserMessage);
            chatMemory.add(chatMessageStore.toChatMessage(storeUserMessage));

            final StringBuilder[] partMsg = {new StringBuilder()};
            final StringBuilder[] fullMsg = {new StringBuilder()};
            final StringBuilder[] reasoningMsg = {new StringBuilder()};

            final long reasoningStartTime = System.currentTimeMillis();
            final int[] reasoningTime = {-1};
            final String[] reasoningStatus = {AiStatus.CANCELLED.getValue()};
            final boolean[] reasoningStarted = {false};
            final boolean[] reasoningComplete = {false};
            final boolean[] thinkFirst = {false};
            final boolean[] thinkLast = {false};

            chatModel.chat(chatMemory.messages(), new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String msg) {
                    if (!sseEmitterRegistry.contains(userId, conversationId)) {
                        Message message = Message
                                .builder()
                                .messageId(aiMessageId)
                                .parentId(userMessageId)
                                .model(finalModelName)
                                .role("assistant")
                                .content(fullMsg[0].toString().trim())
                                .reasoningEnabled(conversationDTO.getReasoningEnabled())
                                .reasoningContent(conversationDTO.getReasoningEnabled() ?
                                        reasoningMsg[0].toString().trim() : null)
                                .reasoningTime(reasoningTime[0])
                                .reasoningStatus(conversationDTO.getReasoningEnabled() ?
                                        reasoningStatus[0] : AiStatus.UNUSED.getValue())
                                .searchEnabled(conversationDTO.getSearchEnabled())
                                .searchResults(List.of())
                                .searchStatus(AiStatus.UNUSED.getValue())
                                .files(List.of())
                                .status(AiStatus.CANCELLED.getValue())
                                .createTime(LocalDateTime.now())
                                .build();
                        chatMessageStore.updateMessage(conversation.getConversationId(), message);
                        conversationLock.forceUnlock();
                        throw new RuntimeException("中止对话");
                    }

                    // 推理内容处理
                    if (conversationDTO.getReasoningEnabled()) {
                        if (msg.equals("<think>")) {
                            reasoningStarted[0] = true;
                        } else if (msg.equals("</think>")) {
                            reasoningComplete[0] = true;
                            reasoningTime[0] = (int) ((System.currentTimeMillis() - reasoningStartTime) / 1000);
                            reasoningStatus[0] = AiStatus.FINISHED.getValue();
                        } else if (reasoningStarted[0] && !reasoningComplete[0]) {
                            if (msg.isBlank()) {
                                if (!thinkFirst[0]) {
                                    thinkFirst[0] = true;
                                    return;
                                }
                            }
                            reasoningMsg[0].append(msg);
                            partMsg[0].append(msg);
                        } else if (reasoningComplete[0]) {
                            if (msg.isBlank()) {
                                if (!thinkLast[0]) {
                                    thinkLast[0] = true;
                                    return;
                                }
                            }
                            fullMsg[0].append(msg);
                            partMsg[0].append(msg);
                        }
                    } else {
                        fullMsg[0].append(msg);
                        partMsg[0].append(msg);
                    }

                    StreamingMessageVO streamingMessageVO = StreamingMessageVO
                            .builder()
                            .type("message")
                            .messageId(aiMessageId)
                            .parentId(userMessageId)
                            .model(finalModelName)
                            .role("assistant")
                            .content(partMsg[0].toString())
                            .build();

                    if (conversationDTO.getReasoningEnabled() && (!reasoningComplete[0] || msg.equals("</think>"))) {
                        streamingMessageVO.setType("reasoning");
                    }
                    if (!msg.equals("</think>") && partMsg[0].length() <= 10) return;

                    try {
                        sseEmitter.send(SseEmitter.event()
                                .id(conversationId)
                                .name(streamingMessageVO.getType())
                                .comment("AI消息")
                                .data(streamingMessageVO)
                        );
                        partMsg[0].setLength(0);
                    } catch (IOException e) {
                        log.error("发送AI消息失败", e);
                        sseEmitterRegistry.remove(userId, conversationId);
                    }
                }

                @Override
                public void onCompleteResponse(ChatResponse chatResponse) {
                    if (!partMsg[0].isEmpty()) {
                        try {
                            StreamingMessageVO streamingMessageVO = StreamingMessageVO
                                    .builder()
                                    .type("message")
                                    .messageId(aiMessageId)
                                    .parentId(userMessageId)
                                    .model(finalModelName)
                                    .role("assistant")
                                    .content(partMsg[0].toString())
                                    .build();
                            sseEmitter.send(SseEmitter.event()
                                    .id(conversationId)
                                    .name("message")
                                    .comment("AI消息")
                                    .data(streamingMessageVO)
                            );
                            partMsg[0].setLength(0);
                        } catch (IOException ignored) {}
                    }

                    Message message = Message
                            .builder()
                            .messageId(aiMessageId)
                            .parentId(userMessageId)
                            .model(finalModelName)
                            .role("assistant")
                            .content(fullMsg[0].toString().trim())
                            .reasoningEnabled(conversationDTO.getReasoningEnabled())
                            .reasoningContent(conversationDTO.getReasoningEnabled() ?
                                    reasoningMsg[0].toString().trim() : null)
                            .reasoningTime(reasoningTime[0])
                            .reasoningStatus(conversationDTO.getReasoningEnabled() ?
                                    reasoningStatus[0] : AiStatus.UNUSED.getValue())
                            .searchEnabled(conversationDTO.getSearchEnabled())
                            .searchResults(List.of())
                            .searchStatus(AiStatus.UNUSED.getValue())
                            .files(List.of())
                            .status(AiStatus.FINISHED.getValue())
                            .createTime(LocalDateTime.now())
                            .build();

                    chatMessageStore.updateMessage(conversation.getConversationId(), message);
                    sseEmitterRegistry.remove(userId, conversationId);
                    conversationLock.forceUnlockAsync();
                }

                @Override
                public void onError(Throwable error) {
                    log.error("发送AI消息失败", error);
                    sseEmitterRegistry.remove(userId, conversationId);
                    conversationLock.forceUnlockAsync();
                }
            });

            return sseEmitter;
        } catch (Exception e) {
            conversationLock.forceUnlockAsync();
            throw new RuntimeException(e);
        }
    }

    @Override
    public ConversationVO getConversation(String conversationId) {
        Conversation conversation = chatMessageStore.getConversation(conversationId, UserContext.getUserId());

        List<MessageVO> messageVOs = new ArrayList<>();
        for (Message message : conversation.getMessages()) {
            MessageVO messageVO = BeanUtil.copyProperties(message, MessageVO.class, "files");
            messageVO.setFiles(BeanUtil.copyToList(message.getFiles(), MediaFileVO.class));
            messageVOs.add(messageVO);
        }

        ConversationVO conversationVO = ConversationVO
                .builder()
                .conversationId(conversation.getConversationId())
                .userId(conversation.getUserId())
                .title(conversation.getTitle())
                .currentMessageId(conversation.getCurrentMessageId())
                .messages(messageVOs)
                .createTime(conversation.getCreateTime())
                .updateTime(conversation.getUpdateTime())
                .build();

        return conversationVO;
    }

    @Override
    public SimplePageQueryVO<ConversationPageVO> getConversations(PageQueryDTO pageQueryDTO) {
        Query query = new Query(Criteria.where("userId").is(UserContext.getUserId()));
        query.with(Sort.by(Sort.Order.desc("updateTime")));
        query.skip((pageQueryDTO.getPage() - 1) * pageQueryDTO.getSize())
                .limit(Math.toIntExact(pageQueryDTO.getSize()));
        query.fields().include("_id", "title", "createTime", "updateTime");

        List<Conversation> conversations = mongoTemplate.find(query, Conversation.class);

        return SimplePageQueryVO.<ConversationPageVO>staticBuilder()
                .page(pageQueryDTO.getPage())
                .size(pageQueryDTO.getSize())
                .data(BeanUtil.copyToList(conversations, ConversationPageVO.class))
                .build();
    }

    @Override
    public void deleteConversation(String conversationId) {
        mongoTemplate.remove(new Query(Criteria.where("_id").is(conversationId)), Conversation.class);
    }

    @Override
    public void editTitle(TitleDTO titleDTO) {
        mongoTemplate.updateFirst(
                new Query(Criteria.where("_id").is(titleDTO.getConversationId())),
                new Update().set("title", titleDTO.getTitle()),
                Conversation.class
        );
    }

    @Override
    public void stopConversation(StopConversationDTO stopConversationDTO) {
        if (sseEmitterRegistry.contains(UserContext.getUserId(), stopConversationDTO.getConversationId())) {
            sseEmitterRegistry.remove(UserContext.getUserId(), stopConversationDTO.getConversationId());
        } else {
            StopConversationMsgDTO stopConversationMsgDTO = StopConversationMsgDTO
                    .builder()
                    .userId(UserContext.getUserId())
                    .conversationId(stopConversationDTO.getConversationId())
                    .timestamp(System.currentTimeMillis())
                    .build();
            rocketMQTemplate.convertAndSend("ai:conversationStop", stopConversationMsgDTO);
        }
    }

    @Async
    public void generateTitle(SseEmitter sseEmitter, ConversationDTO conversationDTO) throws IOException {
        String title;

        String content = conversationDTO.getContent();
        if (content.length() <= 15) {
            title = content;
        } else {
            title = content.substring(0, 15);
        }

        TitleVO titleVO = TitleVO
                .builder()
                .type("title")
                .title(title)
                .build();

        sseEmitter.send(SseEmitter.event()
                .id(conversationDTO.getConversationId())
                .name("title")
                .comment("生成标题")
                .data(titleVO)
        );

        Query query = new Query(Criteria.where("_id").is(conversationDTO.getConversationId()));
        Update update = new Update().set("title", title);
        mongoTemplate.updateFirst(query, update, Conversation.class);
    }
}
