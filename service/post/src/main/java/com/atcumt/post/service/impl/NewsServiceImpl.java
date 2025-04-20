package com.atcumt.post.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.atcumt.model.comment.enums.CommentMessage;
import com.atcumt.model.like.enums.LikeMessage;
import com.atcumt.model.post.dto.NewsListDTO;
import com.atcumt.model.post.entity.News;
import com.atcumt.model.post.enums.PostStatus;
import com.atcumt.model.post.vo.NewsListVO;
import com.atcumt.model.post.vo.NewsSimpleVO;
import com.atcumt.model.post.vo.NewsVO;
import com.atcumt.post.service.NewsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsServiceImpl implements NewsService {
    private final MongoTemplate mongoTemplate;
    private final NacosConfigManager nacosConfigManager;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public NewsVO getNews(Long newsId) {
        News news = mongoTemplate.findOne(
                new Query(Criteria.where("newsId").is(newsId)), News.class
        );

        // 如果新闻发布时间在 10 天内，检查新闻是否被删除
        if (news != null && news.getPublishTime().isAfter(LocalDateTime.now().minusDays(10))) {
            checkNewsDeleted(news);
        }

        return BeanUtil.toBean(news, NewsVO.class);
    }

    @Async
    public void checkNewsDeleted(News news) {
        String url = news.getSourceUrl();
        Long newsId = news.getNewsId();

        // 检查新闻是否被删除
        WebClient.builder().baseUrl(url).build().get().retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError() || status.is3xxRedirection(),
                        response -> {
                            log.info("新闻 {} 已被删除", newsId);
                            // 如果新闻被删除，更新新闻状态为已删除
                            mongoTemplate.updateFirst(
                                    new Query(Criteria.where("_id").is(newsId)),
                                    new Update().set("status", PostStatus.DELETED.getCode()),
                                    News.class
                            );
                            return Mono.empty();
                        })
                .toEntity(String.class).block();
    }

    @Override
    public NewsListVO getNewsList(NewsListDTO newsListDTO) {
        Query query = new Query();
        // 如果有 newsCategory，添加额外的条件：筛选 newsCategory
        if ((newsListDTO.getNewsCategory() != null && !newsListDTO.getNewsCategory().isEmpty())) {
            // 如果 newsCategory 为 latest，不添加筛选条件
            if (!newsListDTO.getNewsCategory().equals("最新") && !newsListDTO.getNewsCategory().equals("latest")) {
                query.addCriteria(Criteria.where("newsCategory").is(newsListDTO.getNewsCategory()));
            }
        }

        // 如果有 newsType，添加额外的条件：筛选 newsType
        if ((newsListDTO.getNewsType() != null && !newsListDTO.getNewsType().isEmpty())) {
            // 如果 newsType 为 latest，不添加筛选条件
            if (!newsListDTO.getNewsType().equals("最新") && !newsListDTO.getNewsType().equals("latest")) {
                query.addCriteria(Criteria.where("newsType").is(newsListDTO.getNewsType()));
            }
        }

        // 如果有 sourceName，添加额外的条件：筛选 sourceName
        if ((newsListDTO.getSourceName() != null && !newsListDTO.getSourceName().isEmpty())) {
            // 如果 sourceName 为 latest，不添加筛选条件
            if (!newsListDTO.getSourceName().equals("最新") && !newsListDTO.getSourceName().equals("latest")) {
                query.addCriteria(Criteria.where("sourceName").is(newsListDTO.getSourceName()));
            }
        }

        switch (newsListDTO.getSort()) {
            case "time_desc" -> {
                if (newsListDTO.getCursor() != null) {
                    LocalDateTime cursor;
                    try {
                        cursor = LocalDateTime.parse(newsListDTO.getCursor());
                    } catch (Exception e) {
                        throw new IllegalArgumentException(LikeMessage.CURSOR_FORMAT_INCORRECT.getMessage());
                    }
                    // 先添加筛选条件，再进行排序
                    query.addCriteria(Criteria.where("publishTime").lte(cursor));
                }

                // 如果有 lastNewsId，添加额外的条件：筛选 newsId 小于 lastNewsId
                if (newsListDTO.getLastNewsId() != null) {
                    query.addCriteria(Criteria.where("newsId").lt(newsListDTO.getLastNewsId()));
                }

                // 排序：先按 score 排序，再按 newsId 排序
                query.with(Sort.by(
                        Sort.Order.desc("publishTime"),
                        Sort.Order.desc("newsId")
                ));
            }
            case "score" -> {
                if (newsListDTO.getCursor() != null) {
                    double cursor;
                    try {
                        cursor = Double.parseDouble(newsListDTO.getCursor());
                    } catch (Exception e) {
                        throw new IllegalArgumentException(LikeMessage.CURSOR_FORMAT_INCORRECT.getMessage());
                    }
                    // 先添加筛选条件，再进行排序 默认只返回一周内的数据
                    query.addCriteria(Criteria
                            .where("score").lte(cursor)
                            .and("publishTime").gte(LocalDateTime.now().minusWeeks(1))
                    );
                }

                // 如果有 lastNewsId，添加额外的条件：筛选 newsId 小于 lastNewsId
                if (newsListDTO.getLastNewsId() != null) {
                    query.addCriteria(Criteria.where("newsId").lt(newsListDTO.getLastNewsId()));
                }

                // 排序：先按 score 排序，再按 newsId 排序
                query.with(Sort.by(
                        Sort.Order.desc("score"),
                        Sort.Order.desc("publishTime"),
                        Sort.Order.desc("newsId")
                ));
            }
            default -> throw new IllegalArgumentException(CommentMessage.SORT_NOT_SUPPORT.getMessage());
        }

        query.addCriteria(Criteria.where("status").is(PostStatus.PUBLISHED.getCode()));

        query.fields().exclude("content");
        // 设置分页大小
        query.limit(newsListDTO.getSize());

        List<News> newsList = mongoTemplate.find(query, News.class);

        Set<Object> newsTypes = new HashSet<>();

        List<NewsSimpleVO> newsVOs = newsList.stream()
                .map(news -> {
                    NewsSimpleVO newsSimpleVO = BeanUtil.toBean(news, NewsSimpleVO.class);
                    newsTypes.add(news.getNewsType());
                    return newsSimpleVO;
                })
                .toList();

        List<Object> distinctNewsTypes = newsTypes.stream().toList();
        List<Object> shortNames = redisTemplate.opsForHash().multiGet("post:news:newsTypeShortName", distinctNewsTypes);
        Map<String, String> shortNameMap = new HashMap<>();
        for (int i = 0; i < distinctNewsTypes.size(); i++) {
            if (shortNames.get(i) != null) {
                shortNameMap.put(String.valueOf(distinctNewsTypes.get(i)), String.valueOf(shortNames.get(i)));
            }
        }

        for (NewsSimpleVO newsVO : newsVOs) {
            String shortName = shortNameMap.get(newsVO.getNewsType());

            if (shortName != null) {
                newsVO.setShortName(shortName);
            } else {
                newsVO.setShortName(newsVO.getNewsType());
            }
        }

        Long lastNewsId = null;
        String cursor = null;
        if (!newsVOs.isEmpty()) {
            lastNewsId = newsList.getLast().getNewsId();

            cursor = newsList.getLast().getPublishTime().toString();
        }

        NewsListVO newsListVO = NewsListVO.builder()
                .newsCategory(newsListDTO.getNewsCategory())
                .newsType(newsListDTO.getNewsType())
                .sourceName(newsListDTO.getSourceName())
                .size(newsList.size())
                .cursor(cursor)
                .lastNewsId(lastNewsId)
                .newsList(newsVOs)
                .build();
        return newsListVO;
    }

    @PostConstruct
    public void initNewsType() {
        String dataId = "news-type.json";
        String group = "DEFAULT_GROUP";
        try {
            String newsType = nacosConfigManager.getConfigService()
                    .getConfigAndSignListener(dataId, group, 5000, new Listener() {
                        @Override
                        public void receiveConfigInfo(String newsType) {
                            log.info("新闻类型配置更新");
                            redisTemplate.opsForValue().set("post:news:newsType", newsType);
                            redisTemplate.opsForHash().putAll("post:news:newsTypeShortName", getNewsTypeShortNames(newsType));
                        }

                        @Override
                        public Executor getExecutor() {
                            return null;
                        }
                    });
            redisTemplate.opsForValue().set("post:news:newsType", newsType);
            redisTemplate.opsForHash().putAll("post:news:newsTypeShortName", getNewsTypeShortNames(newsType));
        } catch (Exception e) {
            log.error("监听新闻类型配置失败");
        }
    }

    @SneakyThrows
    public Map<String, String> getNewsTypeShortNames(String jsonString) {
        Map<String, String> result = new HashMap<>();

        // 获取"newsCategory"节点中的数据
        JsonNode newsCategories = objectMapper.readTree(jsonString).get("newsCategory");

        // 遍历所有的categoryName (校园、学院等)
        for (var newsCategory : newsCategories) {
            // 获取每个categoryName下的categoryList
            JsonNode categoryList = newsCategory.get("categoryList");

            // 遍历每个categoryList中的条目
            for (var categoryItem : categoryList) {
                String newsTypeName = categoryItem.get("newsType").asText();
                String shortName = categoryItem.get("shortName").asText();

                // 将所有条目直接放入result Map中，不区分类别
                result.put(newsTypeName, shortName);
            }
        }

        return result;
    }

    @Override
    @SneakyThrows
    public JsonNode getNewsType() {
        JsonNode cachedConfigInfo = objectMapper.readTree(redisTemplate.opsForValue().get("post:news:newsType"));
        if (cachedConfigInfo != null) {
            return cachedConfigInfo;
        }

        String dataId = "news-type.json";
        String group = "DEFAULT_GROUP";
        try {
            String newsType = nacosConfigManager.getConfigService()
                    .getConfig(dataId, group, 5000);
            redisTemplate.opsForValue().set("post:news:newsType", newsType);
            return objectMapper.readTree(newsType);
        } catch (NacosException e) {
            log.error("获取新闻类型失败", e);
            throw new RuntimeException("获取新闻类型失败");
        }
    }

}
