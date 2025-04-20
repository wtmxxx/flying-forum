package com.atcumt.search.task;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.JsonData;
import com.atcumt.common.utils.RedisLockUtil;
import com.atcumt.model.search.enums.SuggestionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Search Suggestion，定时清理
 */
@Component
@Slf4j
public class SearchSuggestionCleanTask {
    private final ElasticsearchClient elasticsearchClient;
    private final RedisLockUtil redisLockUtil;
    private static final String LOCK_KEY = "search:suggestion-clean-lock";

    @Autowired
    SearchSuggestionCleanTask(
            ElasticsearchClient elasticsearchClient,
            RedisTemplate<String, String> redisTemplate
    ) {
        this.elasticsearchClient = elasticsearchClient;
        this.redisLockUtil = RedisLockUtil.create(redisTemplate, 5, TimeUnit.MINUTES);
    }

    // 每天凌晨3点执行
    @Scheduled(cron = "0 0 3 * * ?")
//    @Scheduled(fixedRate = 5000)
    public void cleanSuggestion() {
        if (redisLockUtil.tryLock(LOCK_KEY)) {
            try {
                elasticsearchClient.deleteByQuery(d -> d
                        .index("suggestion")
                        .query(q -> q.bool(b -> b
                                .must(m -> m.term(t -> t.field("type").value(SuggestionType.SEARCH.getValue())))
                                .must(m -> m.range(r -> r.field("score").lte(JsonData.of(3.0))))
                                .must(m -> m.range(r -> r.field("createTime").lt(JsonData.of(LocalDateTime.now().minusDays(7)))))
                        ))
                );
            } catch (Exception e) {
                log.error("清除搜索提示词异常: ", e);
            }
        }
    }
}