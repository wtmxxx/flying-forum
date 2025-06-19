package com.atcumt.ai.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
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

import java.lang.reflect.Field;

@Configuration
public class VectorConfiguration {
    @Value("${spring.ai.vectorstore.elasticsearch.index-name:knowledge-base}")
    private String indexName;
    @Value("${spring.ai.vectorstore.elasticsearch.dimensions:1024}")
    private Integer dimensions;
    @Value("${spring.ai.vectorstore.elasticsearch.embedding-field-name:embedding}")
    private String embeddingFieldName;
    @Value("${spring.ai.vectorstore.elasticsearch.similarity:cosine}")
    private SimilarityFunction similarity;

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

    @Bean
    public ElasticsearchVectorStore vectorStore(RestClient elasticsearchRestClient, EmbeddingModel embeddingModel, BatchingStrategy batchingStrategy, ElasticsearchClient elasticsearchClient) {
        ElasticsearchVectorStoreOptions vectorStoreOptions = new ElasticsearchVectorStoreOptions();
        vectorStoreOptions.setIndexName(indexName);
        vectorStoreOptions.setDimensions(dimensions);
        vectorStoreOptions.setEmbeddingFieldName(embeddingFieldName);
        vectorStoreOptions.setSimilarity(similarity);

        System.out.println("密切关注 ElasticsearchVectorStore 是否会在后续 Spring AI 版本中修改ElasticsearchClient 的创建方式！！！");
        // 使用反射注入替换 final 字段

        return new ElasticsearchVectorStore(ElasticsearchVectorStore
                .builder(elasticsearchRestClient, embeddingModel)
                .initializeSchema(true)
                .options(vectorStoreOptions)
                .batchingStrategy(batchingStrategy)) {
            {
                try {
                    // 使用反射注入替换 final 字段
                    Field field = ElasticsearchVectorStore.class.getDeclaredField("elasticsearchClient");
                    field.setAccessible(true);
                    field.set(this, elasticsearchClient);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to override elasticsearchClient", e);
                }
            }
        };
    }
}
