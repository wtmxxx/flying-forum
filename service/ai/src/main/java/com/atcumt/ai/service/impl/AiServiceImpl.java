package com.atcumt.ai.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.atcumt.ai.ai.FlyingChatHistory;
import com.atcumt.ai.ai.FlyingChatMemory;
import com.atcumt.ai.service.AiService;
import com.atcumt.ai.tools.WebSearchTool;
import com.atcumt.ai.utils.ConversationManager;
import com.atcumt.common.utils.UserContext;
import com.atcumt.model.ai.dto.ConversationDTO;
import com.atcumt.model.ai.dto.StopConversationDTO;
import com.atcumt.model.ai.dto.StopConversationMsgDTO;
import com.atcumt.model.ai.dto.TitleDTO;
import com.atcumt.model.ai.entity.*;
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
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
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
@RefreshScope
@RequiredArgsConstructor
@Slf4j
public class AiServiceImpl implements AiService {
    private final ConversationManager conversationManager;
    private final MongoTemplate mongoTemplate;
    private final RedissonClient redissonClient;
    private final RocketMQTemplate rocketMQTemplate;
    private final FlyingChatHistory chatHistory;
    private final WebSearchTool webSearchTool;
    private final ElasticsearchVectorStore vectorStore;
    private final TokenCountEstimator tokenizer;
    private ChatModel dashScopeChatModel;

    private static final int MAX_TOKENS = 20000;
    private static final int MAX_MESSAGE_SIZE = 100;
    private static final int MAX_MEMORY_SIZE = (MAX_MESSAGE_SIZE / 3) / 2 * 2;
    private static final int MAX_LEASE_TIME = 15;

    @Value("${spring-ai.dashscope.base-url}")
    private String dashScopeBaseUrl;
    @Value("${spring-ai.dashscope.api-key}")
    private String dashScopeApiKey;
    @Value("${spring-ai.dashscope.base-model:qwen2.5-1.5b-instruct}")
    private String baseModel;
    @Value("${spring-ai.dashscope.title-model:qwen2.5-1.5b-instruct}")
    private String titleModel;
    @Value("${spring-ai.dashscope.chat-model:qwen2.5-1.5b-instruct}")
    private String chatModel;
    @Value("${spring-ai.dashscope.reasoning-model:deepseek-r1-distill-llama-70b}")
    private String reasoningModel;


    @PostConstruct
    void init() {
        dashScopeChatModel = DashScopeChatModel
                .builder()
                .dashScopeApi(
                        DashScopeApi
                                .builder()
                                .baseUrl(dashScopeBaseUrl)
                                .apiKey(dashScopeApiKey)
                                .build()
                )
                .defaultOptions(
                        DashScopeChatOptions
                                .builder()
                                .withModel(baseModel)
                                .withStream(true)
//                        .withResponseFormat(DashScopeResponseFormat.builder().type(DashScopeResponseFormat.Type.JSON_OBJECT).build())
                                .build()
                )
                .build();
    }

    private String getSystemMessage() {
        return "你是中国矿业大学的AI助手，你的名字叫圈圈，现在是北京时间：" + LocalDateTime.now();
    }

    @Override
    public Flux<FluxVO> conversation(ConversationDTO conversationDTO) throws InterruptedException {
        Flux<NewConversationVO> newConversationFlux = Flux.empty();
        Flux<TitleVO> titleFlux = Flux.empty();
        Flux<WebSearchResultsVO> webSearchFlux = Flux.empty();
        Flux<KnowledgeBaseVO> knowledgeBaseFlux = Flux.empty();
        Flux<TextMessageVO> streamingMessageFlux;

        int estimatedTokens = tokenizer.estimate(conversationDTO.getTextContent());
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
                if (newConversation) {
                    Thread.sleep(100);
                    conversation = chatHistory.getConversation(conversationId, userId);
                    if (conversation == null) {
                        Thread.yield();
                        conversation = chatHistory.getConversation(conversationId, userId);
                    }
                }

                if (conversation == null) throw new RuntimeException("对话不存在");
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

            FlyingChatMemory chatMemory = new FlyingChatMemory(MAX_TOKENS, MAX_MEMORY_SIZE, tokenizer);

            List<Message> historyMessages = chatHistory.toChatMessages(parentId, conversation.getMessages());
            for (Message historyMessage : historyMessages) {
                chatMemory.add(historyMessage);
            }
            boolean reasoningEnabled = conversationDTO.getReasoningEnabled();
            String modelName = reasoningEnabled ? reasoningModel : chatModel;
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

            org.springframework.ai.rag.Query transformedQuery = new org.springframework.ai.rag.Query(conversationDTO.getTextContent());
            if (conversationDTO.getSearchEnabled() || conversationDTO.getKnowledgeBaseEnabled()) {
                List<Message> searchHistoryMessages = new ArrayList<>();
                searchHistoryMessages.add(new UserMessage("系统提示：" + getSystemMessage()));
                searchHistoryMessages.addAll(historyMessages.subList(Math.max(0, historyMessages.size() - 5), historyMessages.size()));

                org.springframework.ai.rag.Query query = org.springframework.ai.rag.Query.builder()
                        .text(conversationDTO.getTextContent())
                        .history(searchHistoryMessages)
                        .build();

                System.out.println(searchHistoryMessages);

                String promptTemplate = """
                                        你是一名智能助手，任务是根据对话历史和用户提出的跟进问题，综合上下文信息，生成一个清晰、独立、具体的查询语句。
                                        
                                        - 查询必须保留用户原意；
                                        - 必须补全如“我们学校”“这里”“这门课”此类省略或指代不明的信息；
                                        - 不要添加无关内容；
                                        - 只输出最终的改写结果。
                                        
                                        以下是一个示例：
                                        
                                        对话历史：
                                        用户：我们学校是中国矿业大学
                                        助手：明白了
                                        
                                        跟进问题：
                                        我们学校最近有哪些活动？
                                        
                                        独立查询：
                                        中国矿业大学最近有哪些活动？
                                        
                                        ---
                                        
                                        现在请处理以下输入：
                                        
                                        对话历史：
                                        {history}
                                        
                                        跟进问题：
                                        {query}
                                        
                                        独立查询：
                                        """;

                QueryTransformer queryTransformer = CompressionQueryTransformer.builder()
                        .promptTemplate(new PromptTemplate(promptTemplate))
                        .chatClientBuilder(ChatClient.builder(dashScopeChatModel))
                        .build();

                transformedQuery = queryTransformer.transform(query);

                System.out.println(transformedQuery.text());
            }

            List<WebSearch> webSearches;
            if (conversationDTO.getSearchEnabled()) {
                String transformedText = transformedQuery.text();
                WebSearchParameter webSearchParameter = WebSearchParameter
                        .builder()
                        .q(transformedText)
                        .build();

                webSearches = webSearchTool.search(webSearchParameter, 10);

                webSearchFlux = Mono.just(WebSearchResultsVO
                        .builder()
                        .searchResults(webSearches)
                        .build()).flux();

                System.out.println(webSearches);

                StringBuilder searchText = new StringBuilder("以下是搜索到的内容，可能对回答有帮助：\n\n");

                for (int i = 0; i < Math.min(5, webSearches.size()); i++) {
                    try {
                        var webSearch = webSearches.get(i);
                        int index = i + 1; // 序号从1开始

                        searchText.append("[").append(index).append("] ")
                                .append("Title: ").append(webSearch.getTitle()).append("\n")
                                .append("Content: ").append(webSearch.getContent()).append("\n\n");

                    } catch (Exception ignored) {}
                }

                searchText.append("如需引用以上内容，请在回答中注明对应序号，例如：中国矿业大学位于徐州<ref>[1]</ref>，拥有两个校区<ref>[1]</ref><ref>[2]</ref>。\n\n");

                chatMemory.add(new UserMessage(searchText.toString()));
            } else {
                webSearches = List.of();
            }

            // 知识库
            List<KnowledgeBase> knowledgeBases = new ArrayList<>();
            if (conversationDTO.getKnowledgeBaseEnabled()) {
                DocumentRetriever retriever = VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)
                        .similarityThreshold(0.5)    // 设置相似度阈值
                        .topK(5)                     // 返回前3个最相关的文档
//                    .filterExpression()
                        .build();

                List<Document> documents = retriever.retrieve(transformedQuery);

                if (!documents.isEmpty()) {
                    StringBuilder knowledgeBaseText = new StringBuilder("以下是知识库中相关的内容，可能对回答有帮助：\n\n");

                    for (int i = 0; i < documents.size(); i++) {
                        Document document = documents.get(i);
                        int index = i + 1 + webSearches.size(); // 序号从1开始，接着搜索结果

                        knowledgeBaseText.append("[").append(index).append("] ")
                                .append("Title: ").append(document.getMetadata().getOrDefault("title", "无标题")).append("\n")
                                .append("Content: ").append(document.getText()).append("\n")
                                .append("Url: ").append(document.getMetadata().getOrDefault("url", "无链接")).append("\n\n");

                        KnowledgeBase knowledgeBase = KnowledgeBase
                                .builder()
                                .id(document.getId())
                                .title(String.valueOf(document.getMetadata().getOrDefault("title", "无标题")))
                                .content(document.getText())
                                .url((String) document.getMetadata().getOrDefault("url", null))
                                .build();
                        knowledgeBases.add(knowledgeBase);
                    }

                    knowledgeBaseText.append("如需引用以上内容，请在回答中注明对应序号，例如：中国矿业大学位于徐州<ref>[1]</ref>，拥有两个校区<ref>[1]</ref><ref>[2]</ref>。\n\n");

                    chatMemory.add(new UserMessage(knowledgeBaseText.toString()));

                    System.out.println(knowledgeBaseText);
                }

                knowledgeBaseFlux = Mono.just(KnowledgeBaseVO
                        .builder()
                        .documents(knowledgeBases)
                        .build()).flux();
            }

            chatMemory.add(chatHistory.toChatMessage(storeUserMessage));

            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(getSystemMessage()));
            messages.addAll(chatMemory.get(conversationId));

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
//                        System.out.println("回答：" + textContent);
                        if (reasoningEnabled) {
                            Map<String, Object> metadata = chatResponse.getResult().getOutput().getMetadata();
                            String reasoningContent = String.valueOf(metadata.getOrDefault("reasoningContent", ""));

                            if (!reasoningContent.isEmpty()) {
                                finalReasoningContent.append(reasoningContent);
//                                System.out.println("深度思考：" + reasoningContent);
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
                                .messageId(assistantMessageId)
                                .parentId(userMessageId)
                                .model(modelName)
                                .role(MessageType.ASSISTANT.getValue())
                                .textContent(finalTextContent.toString())
                                .reasoningEnabled(reasoningEnabled)
                                .reasoningContent(finalReasoningContent.toString())
                                .reasoningTime(reasoningTime.get())
                                .reasoningStatus(reasoningStatus.getValue())
                                .searchEnabled(conversationDTO.getSearchEnabled())
                                .searchResults(webSearches)
                                .searchStatus(conversationDTO.getSearchEnabled() ?
                                        AiStatus.FINISHED.getValue() : AiStatus.UNUSED.getValue())
                                .knowledgeBaseEnabled(conversationDTO.getKnowledgeBaseEnabled())
                                .documents(knowledgeBases)
                                .knowledgeBaseStatus(conversationDTO.getKnowledgeBaseEnabled() ?
                                        AiStatus.FINISHED.getValue() : AiStatus.UNUSED.getValue())
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

            return Flux.merge(newConversationFlux, titleFlux, webSearchFlux, knowledgeBaseFlux, streamingMessageFlux);
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
        if (UserContext.getUserId() == null) {
            return SimplePageQueryVO.<ConversationPageVO>staticBuilder()
                    .page(pageQueryDTO.getPage())
                    .size(pageQueryDTO.getSize())
                    .data(new ArrayList<>())
                    .build();
        }

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
                    if (textContent.length() <= 10) {
                        title = textContent;
                    } else {
                        try {
                            Prompt prompt = new Prompt(
                                    List.of(
                                            new UserMessage("""
                                                请根据以下对话内容，为新建的聊天生成一个简洁且相关的标题。
                                                
                                                要求：
                                                1. 不超过15个字符；
                                                2. 准确概括主要话题或目的；
                                                3. 简明清晰且具有吸引力；
                                                4. 避免使用模糊或泛泛的词语。
                                                
                                                对话内容：
                                                """ + textContent + """
                                                
                                                标题：
                                                """)
                                    ),
                                    DashScopeChatOptions.builder()
                                            .withModel(titleModel)
                                            .withTemperature(0.3)
                                            .withTopK(50)
                                            .withTopP(0.9)
                                            .build()
                            );
                            title = dashScopeChatModel.call(prompt).getResult().getOutput().getText();

                            if (title == null || title.isEmpty()) {
                                title = "新对话";
                            } else if (title.length() > 15) {
                                title = title.substring(0, 15);
                            }
                        } catch (Exception e) {
                            title = "新对话";
                        }
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
