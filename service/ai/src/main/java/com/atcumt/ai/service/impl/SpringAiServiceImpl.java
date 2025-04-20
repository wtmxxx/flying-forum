package com.atcumt.ai.service.impl;

import com.atcumt.ai.ai.ChatMessageStore;
import com.atcumt.ai.service.SpringAiService;
import com.atcumt.ai.utils.SseEmitterRegistry;
import com.atcumt.model.ai.dto.ConversationDTO;
import com.atcumt.model.ai.dto.StopConversationDTO;
import com.atcumt.model.ai.dto.TitleDTO;
import com.atcumt.model.ai.vo.ConversationPageVO;
import com.atcumt.model.ai.vo.ConversationVO;
import com.atcumt.model.common.dto.PageQueryDTO;
import com.atcumt.model.common.vo.SimplePageQueryVO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RedissonClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpringAiServiceImpl implements SpringAiService {
    private final SseEmitterRegistry sseEmitterRegistry;
    private final ChatMessageStore chatMessageStore;
    private final MongoTemplate mongoTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    private final RedissonClient redissonClient;
    private final RocketMQTemplate rocketMQTemplate;
    private ChatModel deepseekChatModel;
    private ChatClient chatClient;

    @Value("${spring-ai.openai.deepseek.base-url}")
    private String deepseekBaseUrl;
    @Value("${spring-ai.openai.deepseek.api-key}")
    private String deepseekApiKey;

    @PostConstruct
    void init() {
        deepseekChatModel = OpenAiChatModel
                .builder()
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("deekseek-chat")
                        .streamUsage(true)
                        .responseFormat(ResponseFormat.builder().type(ResponseFormat.Type.JSON_OBJECT).build())
                        .build())
                .openAiApi(OpenAiApi.builder()
                        .baseUrl(deepseekBaseUrl)
                        .apiKey(deepseekApiKey)
                        .build())
                .retryTemplate(RetryTemplate.builder()
                        .maxAttempts(3)
                        .build())
                .build();
    }

    @Override
    public Flux<ChatResponse> conversation(ConversationDTO conversationDTO) {
        chatClient = ChatClient.builder(deepseekChatModel).build();

        List<Message> messages = List.of(
                new SystemMessage("你是中国矿业大学的AI助手，你的名字叫圈圈，现在是北京时间：" + LocalDateTime.now())
        );
        Prompt prompt = new Prompt(messages, ChatOptions.builder()
                .model("deepseek-chat")
                .build());
        Flux<ChatResponse> chatResponseFlux = chatClient.prompt(prompt).stream().chatResponse();

//        chatResponseFlux.doOnNext(chatResponse -> {
//            System.out.println("Response: " + chatResponse.toString());
//        }).doOnComplete( () -> {
//            System.out.println("Completed");
//        }).doOnError(throwable -> {
//            System.out.println("Error: " + throwable.getMessage());
//        }).subscribe();

        return chatResponseFlux;
    }

    @Override
    public ConversationVO getConversation(String conversationId) {
        return null;
    }

    @Override
    public SimplePageQueryVO<ConversationPageVO> getConversations(PageQueryDTO pageQueryDTO) {
        return null;
    }

    @Override
    public void deleteConversation(String conversationId) {

    }

    @Override
    public void stopConversation(StopConversationDTO stopConversationDTO) {

    }

    @Override
    public void editTitle(TitleDTO titleDTO) {

    }
}
