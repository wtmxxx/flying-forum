package com.atcumt.ai.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingOptions;
import org.elasticsearch.client.RestClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStoreOptions;
import org.springframework.ai.vectorstore.elasticsearch.SimilarityFunction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VectorConfiguration {
    @Value("${spring-ai.dashscope.base-url}")
    private String dashScopeBaseUrl;
    @Value("${spring-ai.dashscope.api-key}")
    private String dashScopeApiKey;
    @Value("${spring-ai.dashscope.embedding-model:text-embedding-v4}")
    private String embeddingModel;
    @Value("${spring.ai.vectorstore.elasticsearch.index-name:knowledge-base}")
    private String indexName;
    @Value("${spring.ai.vectorstore.elasticsearch.dimensions:1024}")
    private Integer dimensions;
    @Value("${spring.ai.vectorstore.elasticsearch.embedding-field-name:embedding}")
    private String embeddingFieldName;
    @Value("${spring.ai.vectorstore.elasticsearch.similarity:cosine}")
    private SimilarityFunction similarity;

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

    @Bean
    public ElasticsearchVectorStore vectorStore(RestClient elasticsearchRestClient, EmbeddingModel embeddingModel, BatchingStrategy batchingStrategy) {
        ElasticsearchVectorStoreOptions vectorStoreOptions = new ElasticsearchVectorStoreOptions();
        vectorStoreOptions.setIndexName(indexName);
        vectorStoreOptions.setDimensions(dimensions);
        vectorStoreOptions.setEmbeddingFieldName(embeddingFieldName);
        vectorStoreOptions.setSimilarity(similarity);

        return ElasticsearchVectorStore
                .builder(elasticsearchRestClient, embeddingModel)
                .initializeSchema(true)
                .options(vectorStoreOptions)
                .batchingStrategy(batchingStrategy)
                .build();
    }

    @Bean
    public TokenCountEstimator tokenCountEstimator() {
        return new JTokkitTokenCountEstimator();
    }

    @Bean
    public BatchingStrategy batchingStrategy(TokenCountEstimator tokenCountEstimator) {
        return new TokenCountBatchingStrategy(
                tokenCountEstimator,       // Use the CustomTokenizer for token counting
                5000,                      // Set the maximum input token count
                0.1,                       // Set the reserve percentage
                Document.DEFAULT_CONTENT_FORMATTER,
                MetadataMode.ALL
        );
    }
}
