package com.atcumt.search.listener;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ScriptLanguage;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.bulk.UpdateOperation;
import co.elastic.clients.json.JsonData;
import com.atcumt.common.api.forum.sensitive.SensitiveWordDubboService;
import com.atcumt.model.search.dto.SearchSuggestionDTO;
import com.atcumt.model.search.entity.SuggestionEs;
import com.atcumt.model.search.enums.SuggestionAction;
import com.atcumt.model.search.enums.SuggestionType;
import com.atcumt.search.ai.SearchSuggestionExtractor;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.elasticsearch.client.ResponseException;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@RocketMQMessageListener(
        topic = "search",
        selectorExpression = "searchSuggestion",
        consumerGroup = "search-suggestion-consumer",
        maxReconsumeTimes = 3
)
@RequiredArgsConstructor
@Slf4j
public class SearchSuggestionConsumer implements RocketMQListener<SearchSuggestionDTO> {
    private static final int BATCH_SIZE = 20;
    private final ConcurrentHashMap<String, Long> suggestionSearchCountMap = new ConcurrentHashMap<>();
    private final ElasticsearchClient elasticsearchClient;
    private final RedissonClient redissonClient;
    @DubboReference
    private final SensitiveWordDubboService sensitiveWordDubboService;
    private ChatLanguageModel suggestionModel;

    @Value("${langchain4j.ollama.chat-model.base-url}")
    private String ollamaChatModelBaseUrl;

    @PostConstruct
    void init() {
        // 初始化大模型
        suggestionModel = OllamaChatModel.builder()
                .baseUrl(ollamaChatModelBaseUrl)
                .modelName("qwen2.5:7b")
                .temperature(0.0)
                .topP(0.0)
                .topK(1)
                .supportedCapabilities(Set.of(Capability.RESPONSE_FORMAT_JSON_SCHEMA))
                .responseFormat(ResponseFormat.JSON)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Override
    public void onMessage(SearchSuggestionDTO searchSuggestionDTO) {
        SuggestionEs suggestionEs = SuggestionEs.builder()
                .suggestion(searchSuggestionDTO.getSuggestion().trim())
                .type(searchSuggestionDTO.getType())
                .score(1.0)
                .createTime(LocalDateTime.now())
                .build();

        // 根据 action 执行不同的操作
        if (SuggestionAction.SEARCH.equals(searchSuggestionDTO.getAction())) {
            searchSuggestion(suggestionEs);
        } else if (SuggestionAction.TAG.equals(searchSuggestionDTO.getAction())) {
            suggestionEs.setScore(30.0);
            tagSuggestion(suggestionEs);
        } else if (SuggestionAction.CUSTOM.equals(searchSuggestionDTO.getAction())) {
            suggestionEs.setScore(30.0);
            customSuggestion(suggestionEs);
        } else if (SuggestionAction.DELETE.equals(searchSuggestionDTO.getAction())) {
            deleteSuggestion(suggestionEs);
        } else {
            log.error("未知的搜索提示词消费行为: {}", searchSuggestionDTO.getAction());
        }
    }

    @SneakyThrows
    private void searchSuggestion(SuggestionEs suggestionEs) {
        // 先检查 suggestion 是否存在
        if (checkSuggestionExists(suggestionEs.getSuggestion())) {
            suggestionSearchCountMap.merge(getHashId(suggestionEs.getSuggestion()), 1L, Long::sum);

            if (suggestionSearchCountMap.size() >= BATCH_SIZE) {
                incScore();
            }
        } else {
            // 仅当 suggestion 不存在时，执行插入
            insertSearchSuggestion(suggestionEs);
        }
    }

    @SneakyThrows
    public void insertSearchSuggestion(SuggestionEs suggestionEs) {
        log.info("新增搜索提示: {}", suggestionEs.getSuggestion());
        RLock lock = redissonClient.getLock("search:suggestion-lock:" + getHashId(suggestionEs.getSuggestion()));
        if (lock.tryLock(0, 10, TimeUnit.SECONDS)) {
            // 判断搜索词是否违规
            if (sensitiveWordDubboService.contains(suggestionEs.getSuggestion())) return;
            // 使用大模型优化搜索词
            SearchSuggestionExtractor searchSuggestionExtractor = AiServices.create(SearchSuggestionExtractor.class, suggestionModel);
            List<String> suggestions = searchSuggestionExtractor.extract(suggestionEs.getSuggestion()).getSuggestions();

            log.info("智能处理搜索提示: {}", suggestions);

            if (suggestions.isEmpty()) return;

            for (var s : suggestions) {
                String suggestion = s.trim();
                // 先检查 suggestion 是否存在
                String suggestionId = getHashId(suggestion);
                if (!checkSuggestionExists(suggestion)) {
                    try {
                        elasticsearchClient.create(cr -> cr
                                .index("suggestion")
                                .id(suggestionId)
                                .document(SuggestionEs.builder()
                                        .suggestion(suggestion)
                                        .type(suggestionEs.getType())
                                        .score(suggestionEs.getScore())
                                        .createTime(suggestionEs.getCreateTime())
                                        .build())
                        );
                    } catch (ResponseException e) {
                        // 版本冲突
                        suggestionSearchCountMap.merge(suggestionId, 1L, Long::sum);
                        log.error("创建搜索提示词失败", e);
                    }
                }
            }
        }
    }

    @SneakyThrows
    public void tagSuggestion(SuggestionEs suggestionEs) {
        String suggestionId = getHashId(suggestionEs.getSuggestion());
        try {
            elasticsearchClient.create(cr -> cr
                    .index("suggestion")
                    .id(suggestionId)
                    .document(SuggestionEs.builder()
                            .suggestion(suggestionEs.getSuggestion())
                            .type(suggestionEs.getType())
                            .score(suggestionEs.getScore())
                            .createTime(suggestionEs.getCreateTime())
                            .build())
            );
        } catch (Exception e) {
            log.warn("标签搜索提示词已存在, suggestion: {}", suggestionEs.getSuggestion());
            SuggestionEs originalSuggestionEs = elasticsearchClient.get(gr -> gr
                            .index("suggestion").id(suggestionId),
                    SuggestionEs.class
            ).source();
            if (originalSuggestionEs != null && !SuggestionType.TAG.getValue().equals(originalSuggestionEs.getType())) {
                // score + 30 and type = TAG
                elasticsearchClient.update(ur -> ur
                        .index("suggestion")
                        .id(suggestionId)
                        .script(s -> s
                                .inline(i -> i
                                        .source("ctx._source.score += params.increment; ctx._source.type = params.type")
                                        .lang(ScriptLanguage.Painless)
                                        .params(Map.of(
                                                "increment", JsonData.of(30.0),
                                                "type", JsonData.of(SuggestionType.TAG.getValue())))
                                )
                        ), suggestionEs.getClass()
                );
            }
        }
    }

    @SneakyThrows
    public void customSuggestion(SuggestionEs suggestionEs) {
        String suggestionId = getHashId(suggestionEs.getSuggestion());
        try {
            elasticsearchClient.create(cr -> cr
                    .index("suggestion")
                    .id(suggestionId)
                    .document(SuggestionEs.builder()
                            .suggestion(suggestionEs.getSuggestion())
                            .type(suggestionEs.getType())
                            .score(suggestionEs.getScore())
                            .createTime(suggestionEs.getCreateTime())
                            .build())
            );
        } catch (Exception e) {
            log.warn("自定义搜索提示词已存在, suggestion: {}", suggestionEs.getSuggestion());
        }
    }

    @SneakyThrows
    private void incScore() {
        HashMap<String, Long> suggestionSearchCount = new HashMap<>();

        synchronized (suggestionSearchCountMap) {
            // 如果 suggestionSearchCountMap 是空的，直接返回
            if (suggestionSearchCountMap.isEmpty()) {
                return;
            }

            // 迭代 suggestionSearchCountMap 中的条目
            Iterator<Map.Entry<String, Long>> iterator = suggestionSearchCountMap.entrySet().iterator();
            int count = 0;

            // 处理最多 BATCH_SIZE + (BATCH_SIZE >> 1) 条目
            while (iterator.hasNext() && count < (BATCH_SIZE + (BATCH_SIZE >> 1))) {
                Map.Entry<String, Long> entry = iterator.next();

                // 将数据存入 suggestionSearchCount
                suggestionSearchCount.put(entry.getKey(), entry.getValue());

                // 使用 compute() 更新 suggestionSearchCountMap 中的值
                suggestionSearchCountMap.compute(entry.getKey(), (key, value) -> {
                    if (value == null) return null;  // 如果值为 null，则不处理
                    long newValue = value - entry.getValue();  // 减去对应的值
                    return newValue == 0 ? null : newValue;  // 如果新值为 0，则移除该键
                });

                count++;
            }
        }

        List<BulkOperation> bulkOperations = new ArrayList<>();
        for (var entry : suggestionSearchCount.entrySet()) {
            UpdateOperation<SuggestionEs, SuggestionEs> updateOperation = UpdateOperation.of(uo -> uo
                    .index("suggestion")
                    .id(entry.getKey())
                    .action(ua -> ua.script(s -> s
                            .inline(i -> i
                                    .source("ctx._source.score += params.increment")
                                    .lang(ScriptLanguage.Painless)
                                    .params("increment", JsonData.of(entry.getValue()))
                            )
                    ))
            );
            bulkOperations.add(BulkOperation.of(bo -> bo.update(updateOperation)));
        }
        BulkRequest bulkRequest = BulkRequest.of(br -> br.operations(bulkOperations));
        BulkResponse bulkResponse = elasticsearchClient.bulk(bulkRequest);

        if (bulkResponse.errors()) {
            for (BulkResponseItem item : bulkResponse.items()) {
                if (item.error() != null) {
                    suggestionSearchCountMap.merge(item.id(), suggestionSearchCount.get(item.id()), Long::sum);
                }
            }
        }
    }

    @Scheduled(fixedRate = 10050)
    public void scheduledCount() {
        if (!suggestionSearchCountMap.isEmpty()) {
            incScore();
        }
    }

    @SneakyThrows
    public boolean checkSuggestionExists(String suggestion) {
        try {
            return elasticsearchClient.exists(er -> er
                    .index("suggestion")
                    .id(getHashId(suggestion))
            ).value();
        } catch (IOException e) {
            log.error("检查 keyword 是否存在失败, suggestion: {}", suggestion, e);
            return false;
        }
    }

    @SneakyThrows
    public void deleteSuggestion(SuggestionEs suggestionEs) {
        try {
            log.info("删除搜索提示: {}", suggestionEs);

            if (SuggestionType.WHATEVER_TYPE.getValue().equals(suggestionEs.getType())) {
                elasticsearchClient.delete(dr -> dr
                        .index("suggestion")
                        .id(getHashId(suggestionEs.getSuggestion()))
                );
                return;
            }

            SuggestionEs originalSuggestionEs = elasticsearchClient.get(gr -> gr
                    .index("suggestion")
                    .id(getHashId(suggestionEs.getSuggestion())), SuggestionEs.class
            ).source();

            if (originalSuggestionEs != null && suggestionEs.getType().equals(originalSuggestionEs.getType())) {
                elasticsearchClient.delete(dr -> dr
                        .index("suggestion")
                        .id(getHashId(suggestionEs.getSuggestion()))
                );
            }
        } catch (IOException e) {
            log.error("删除搜索提示失败, suggestion: {}", suggestionEs);
            throw e;
        }
    }

    @SneakyThrows
    public String getHashId(String text) {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) hexString.append(String.format("%02x", b));
        return hexString.toString();
    }
}
