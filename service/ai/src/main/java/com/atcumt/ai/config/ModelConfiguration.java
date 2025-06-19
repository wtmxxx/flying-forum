package com.atcumt.ai.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingOptions;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.MetadataMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelConfiguration {
    @Value("${spring-ai.dashscope.base-url}")
    private String dashScopeBaseUrl;
    @Value("${spring-ai.dashscope.api-key}")
    private String dashScopeApiKey;
    @Value("${spring-ai.dashscope.base-model:qwen2.5-1.5b-instruct}")
    private String baseModel;
    @Value("${spring-ai.dashscope.embedding-model:text-embedding-v4}")
    private String embeddingModel;
    @Value("${spring.ai.vectorstore.elasticsearch.dimensions:1024}")
    private Integer dimensions;

    @Bean
    public ChatModel dashScopeChatModel() {
        return DashScopeChatModel
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
                                .build()
                )
                .build();
    }

    @Bean
    public DashScopeEmbeddingModel embeddingModel() {
        return new DashScopeEmbeddingModel(
                DashScopeApi
                        .builder()
                        .baseUrl(dashScopeBaseUrl)
                        .apiKey(dashScopeApiKey)
                        .build(),
                MetadataMode.ALL,
                DashScopeEmbeddingOptions
                        .builder()
                        .withModel(embeddingModel)
                        .withDimensions(dimensions)
                        .build()
        );
    }
}
