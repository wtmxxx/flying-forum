package com.atcumt.ai.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.atcumt.ai.ai.WotemoChatHistory;
import com.atcumt.ai.ai.WotemoChatMemory;
import com.atcumt.ai.ai.WotemoTokenizer;
import com.atcumt.ai.service.AiService;
import com.atcumt.ai.utils.ConversationManager;
import com.atcumt.common.utils.UserContext;
import com.atcumt.model.ai.dto.ConversationDTO;
import com.atcumt.model.ai.dto.StopConversationDTO;
import com.atcumt.model.ai.dto.StopConversationMsgDTO;
import com.atcumt.model.ai.dto.TitleDTO;
import com.atcumt.model.ai.entity.Conversation;
import com.atcumt.model.ai.entity.StoreMessage;
import com.atcumt.model.ai.enums.AiStatus;
import com.atcumt.model.ai.enums.FluxType;
import com.atcumt.model.ai.vo.*;
import com.atcumt.model.common.dto.PageQueryDTO;
import com.atcumt.model.common.vo.MediaFileVO;
import com.atcumt.model.common.vo.SimplePageQueryVO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiServiceImpl implements AiService {
    private final ConversationManager conversationManager;
    private final MongoTemplate mongoTemplate;
    private final RedissonClient redissonClient;
    private final RocketMQTemplate rocketMQTemplate;
    private final WotemoChatHistory chatHistory;
    private WotemoTokenizer tokenizer = new WotemoTokenizer(MAX_TOKENS);
    private ChatModel dashScopeChatModel;

    private static final int MAX_TOKENS = 20000;
    private static final int MAX_MESSAGE_SIZE = 100;
    private static final int MAX_MEMORY_SIZE = (MAX_MESSAGE_SIZE / 3) / 2 * 2;
    private static final int MAX_LEASE_TIME = 15;

    @Value("${spring-ai.openai.deepseek.base-url}")
    private String deepseekBaseUrl;
    @Value("${spring-ai.openai.deepseek.api-key}")
    private String deepseekApiKey;

    @PostConstruct
    void init() {
//        dashScopeChatModel = OpenAiChatModel
//                .builder()
//                .defaultOptions(OpenAiChatOptions.builder()
//                        .model("deekseek-v3")
//                        .streamUsage(true)
////                        .responseFormat(ResponseFormat.builder().type(ResponseFormat.Type.JSON_OBJECT).build())
//                        .build())
//                .openAiApi(OpenAiApi.builder()
//                        .baseUrl(deepseekBaseUrl)
//                        .apiKey(deepseekApiKey)
//                        .build())
//                .retryTemplate(RetryTemplate.builder()
//                        .maxAttempts(3)
//                        .build())
//                .build();

        dashScopeChatModel = new DashScopeChatModel(
        new DashScopeApi(
                deepseekBaseUrl,
                deepseekApiKey,
                RestClient.builder(),
                WebClient.builder(),
                RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER
        ),
                DashScopeChatOptions
                        .builder()
                        .withModel("deepseek-v3")
                        .withStream(true)
//                        .withResponseFormat(DashScopeResponseFormat.builder().type(DashScopeResponseFormat.Type.JSON_OBJECT).build())
                        .build()
                );
    }

    @Override
    public Flux<FluxVO> conversation(ConversationDTO conversationDTO) throws InterruptedException {
        Flux<NewConversationVO> newConversationFlux = Flux.empty();
        Flux<TitleVO> titleFlux = Flux.empty();
        Flux<TextMessageVO> streamingMessageFlux;

        int estimatedTokens = tokenizer.estimateTokenCountInText(conversationDTO.getTextContent());
        if (estimatedTokens > MAX_TOKENS) {
            throw new RuntimeException("你输入的内容太长了，请缩短后重试");
        }
        String userId = UserContext.getUserId();
        boolean newConversation = conversationDTO.getConversationId() == null || conversationDTO.getConversationId().isEmpty();
        if (newConversation) conversationDTO.setConversationId(IdUtil.simpleUUID());
        String conversationId = conversationDTO.getConversationId();
        RLock conversationLock = redissonClient.getLock("ai:conversation_lock:" + conversationId);
        if (!conversationLock.tryLock(0, MAX_LEASE_TIME, TimeUnit.MINUTES)) {
            throw new RuntimeException("回答正在生成中，请稍后再试");
        }

        try {
            if (newConversation) {
                // 新建对话
                chatHistory.createEmptyConversation(conversationId, userId);
                NewConversationVO newConversationVO = NewConversationVO
                        .builder()
                        .type(FluxType.NEW_CONVERSATION.getValue())
                        .conversationId(conversationId)
                        .build();
                newConversationFlux = Mono.just(newConversationVO).flux();

                titleFlux = generateTitle(conversationDTO);
            }

            Conversation conversation = chatHistory.getConversation(conversationId, userId);
            if (conversation == null) {
                throw new RuntimeException("对话不存在");
            }
            if (conversation.getMessages().size() > MAX_MESSAGE_SIZE) {
                throw new RuntimeException("对话消息太多了，新建一个对话吧");
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

            WotemoChatMemory chatMemory = new WotemoChatMemory(MAX_TOKENS);

            List<Message> historyMessages = chatHistory.toChatMessages(parentId, conversation.getMessages());
            for (Message historyMessage : historyMessages) {
                chatMemory.add(conversationId, historyMessage);
                chatMemory.add(historyMessage);
            }
            boolean reasoningEnabled = conversationDTO.getReasoningEnabled();
            String modelName = reasoningEnabled ? "deepseek-r1" : "deepseek-v3";
            int userMessageId = currentMessageId + 1;
            int assistantMessageId = currentMessageId + 2;
            StoreMessage storeUserMessage = StoreMessage
                    .builder()
                    .messageId(userMessageId)
                    .parentId(parentId)
                    .model(modelName)
                    .role(MessageType.USER.getValue())
                    .textContent(conversationDTO.getTextContent())
                    .reasoningEnabled(reasoningEnabled)
                    .reasoningContent(null)
                    .reasoningTime(-1)
                    .reasoningStatus(null)
                    .searchEnabled(conversationDTO.getSearchEnabled())
                    .searchResults(List.of())
                    .searchStatus(null)
                    .mediaFiles(List.of())
                    .status(null)
                    .createTime(LocalDateTime.now())
                    .build();

            chatHistory.updateMessage(conversation.getConversationId(), storeUserMessage);
            chatMemory.add(chatHistory.toChatMessage(storeUserMessage));

            List<Message> messages = new ArrayList<>();
            messages.add(
                    new SystemMessage("你是中国矿业大学的AI助手，你的名字叫圈圈，现在是北京时间：" + LocalDateTime.now())
            );
            messages.addAll(chatMemory.get(conversationId, MAX_MEMORY_SIZE));

            Prompt prompt = new Prompt(messages, ChatOptions.builder()
                    .model(modelName)
                    .build());

            ChatClient chatClient = ChatClient.builder(dashScopeChatModel)
                    .build();

            StringBuilder finalTextContent = new StringBuilder();
            StringBuilder finalReasoningContent = new StringBuilder();
            long reasoningStartTime = System.currentTimeMillis();
            AtomicInteger reasoningTime = new AtomicInteger(-1);

            conversationManager.register(userId, conversationId);

            Flux<ChatResponse> chatResponseFlux = chatClient
                    .prompt(prompt)
                    .stream().chatResponse()
                    .doOnNext(chatResponse -> {
                        String textContent = chatResponse.getResult().getOutput().getText();
                        System.out.println("回答：" + textContent);
                        if (reasoningEnabled) {
                            Map<String, Object> metadata = chatResponse.getResult().getOutput().getMetadata();
                            String reasoningContent = String.valueOf(metadata.getOrDefault("reasoningContent", ""));

                            if (!reasoningContent.isEmpty()) {
                                finalReasoningContent.append(reasoningContent);
                                System.out.println("深度思考：" + reasoningContent);
                            } else {
                                long reasoningEndTime = System.currentTimeMillis();
                                reasoningTime.compareAndSet(-1, (int) ((reasoningEndTime - reasoningStartTime) / 1000));
                                finalTextContent.append(textContent);
                            }
                        } else {
                            finalTextContent.append(textContent);
                        }
                    })
                    .doFinally(signalType -> {
                        AiStatus reasoningStatus = reasoningEnabled ? switch (signalType) {
                            case ON_COMPLETE -> AiStatus.FINISHED;
                            case ON_ERROR -> AiStatus.FAILED;
                            case CANCEL -> AiStatus.CANCELLED;
                            default -> AiStatus.UNUSED;
                        } : AiStatus.UNUSED;

                        AiStatus status = switch (signalType) {
                            case ON_COMPLETE -> AiStatus.FINISHED;
                            case ON_ERROR -> AiStatus.FAILED;
                            case CANCEL -> AiStatus.CANCELLED;
                            default -> AiStatus.UNUSED;
                        };

                        // 处理完成后的逻辑
                        StoreMessage storeAssistantMessage = StoreMessage
                                .builder()
                                .messageId(userMessageId)
                                .parentId(parentId)
                                .model(modelName)
                                .role(MessageType.ASSISTANT.getValue())
                                .textContent(finalTextContent.toString())
                                .reasoningEnabled(reasoningEnabled)
                                .reasoningContent(finalReasoningContent.toString())
                                .reasoningTime(reasoningTime.get())
                                .reasoningStatus(reasoningStatus.getValue())
                                .searchEnabled(conversationDTO.getSearchEnabled())
                                .searchResults(List.of())
                                .searchStatus(AiStatus.UNUSED.getValue())
                                .mediaFiles(List.of())
                                .status(status.getValue())
                                .createTime(LocalDateTime.now())
                                .build();

                        chatHistory.updateMessage(conversationId, storeAssistantMessage);
                        conversationManager.remove(userId, conversationId);
                        conversationLock.forceUnlockAsync();
                        chatMemory.clear();
                    })
                    .takeUntilOther(conversationManager.cancelFlux(userId, conversationId));

            streamingMessageFlux = chatResponseFlux.map(chatResponse -> {
                String textContent = chatResponse.getResult().getOutput().getText();

                TextMessageVO textMessageVO = TextMessageVO
                        .builder()
                        .type(FluxType.TEXT_MESSAGE.getValue())
                        .messageId(assistantMessageId)
                        .parentId(userMessageId)
                        .model(modelName)
                        .role(MessageType.ASSISTANT.getValue())
                        .textContent(textContent)
                        .build();

                if (reasoningEnabled) {
                    Map<String, Object> metadata = chatResponse.getResult().getOutput().getMetadata();
                    String reasoningContent = String.valueOf(metadata.getOrDefault("reasoningContent", ""));

                    if (!reasoningContent.isEmpty()) {
                        textMessageVO.setType(FluxType.REASONING_MESSAGE.getValue());
                        textMessageVO.setTextContent(reasoningContent);
                    }
                }

                return textMessageVO;
            });

            return Flux.merge(newConversationFlux, titleFlux, streamingMessageFlux);
        } catch (RuntimeException e) {
            conversationLock.forceUnlockAsync();
            throw new RuntimeException(e);
        }
    }

    @Override
    public ConversationVO getConversation(String conversationId) {
        Conversation conversation = chatHistory.getConversation(conversationId, UserContext.getUserId());

        List<MessageVO> messageVOs = new ArrayList<>();
        for (StoreMessage message : conversation.getMessages()) {
            MessageVO messageVO = BeanUtil.copyProperties(message, MessageVO.class, "files");
            messageVO.setMediaFiles(BeanUtil.copyToList(message.getMediaFiles(), MediaFileVO.class));
            messageVOs.add(messageVO);
        }

        return ConversationVO
                .builder()
                .conversationId(conversation.getConversationId())
                .userId(conversation.getUserId())
                .title(conversation.getTitle())
                .currentMessageId(conversation.getCurrentMessageId())
                .messages(messageVOs)
                .createTime(conversation.getCreateTime())
                .updateTime(conversation.getUpdateTime())
                .build();
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
        if (conversationManager.contains(UserContext.getUserId(), stopConversationDTO.getConversationId())) {
            conversationManager.cancel(UserContext.getUserId(), stopConversationDTO.getConversationId());
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

    public Flux<TitleVO> generateTitle(ConversationDTO conversationDTO) {
        // 异步操作
        return Mono.fromCallable(() -> {
                    String title;

                    String textContent = conversationDTO.getTextContent();
                    if (textContent.length() <= 15) {
                        title = textContent;
                    } else {
                        title = textContent.substring(0, 15);
                    }

                    TitleVO titleVO = TitleVO
                            .builder()
                            .type(FluxType.TITLE.getValue())
                            .title(title)
                            .build();

                    // 异步更新 MongoDB 数据
                    Query query = new Query(Criteria.where("_id").is(conversationDTO.getConversationId()));
                    Update update = new Update().set("title", title);
                    mongoTemplate.updateFirst(query, update, Conversation.class);

                    return titleVO;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flux();
    }

}
