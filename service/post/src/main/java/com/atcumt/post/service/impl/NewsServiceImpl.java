package com.atcumt.post.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONObject;
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
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
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
import java.util.List;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsServiceImpl implements NewsService {
    private final MongoTemplate mongoTemplate;
    private final NacosConfigManager nacosConfigManager;
    private final RedisTemplate<Object, Object> redisTemplate;

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
        Query query = Query.query(
                Criteria.where("newsCategory").is(newsListDTO.getNewsCategory())
                        .and("status").is(PostStatus.PUBLISHED.getCode())
        );
        query.fields().exclude("content");

        if (newsListDTO.getNewsType() != null && !newsListDTO.getNewsType().isEmpty()) {
            query.addCriteria(Criteria.where("newsType").is(newsListDTO.getNewsType()));
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

        // 设置分页大小
        query.limit(newsListDTO.getSize());

        List<News> newsList = mongoTemplate.find(query, News.class);

        List<NewsSimpleVO> newsVOs = newsList.stream()
                .map(news -> BeanUtil.toBean(news, NewsSimpleVO.class))
                .toList();

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
            String configInfo = nacosConfigManager.getConfigService()
                    .getConfigAndSignListener(dataId, group, 5000, new Listener() {
                        @Override
                        public void receiveConfigInfo(String configInfo) {
                            log.info("新闻类型配置更新");
                            redisTemplate.opsForValue().set("post:news:newsType", new JSONObject(configInfo));
                        }

                        @Override
                        public Executor getExecutor() {
                            return null;
                        }
                    });
            JSONObject newsType = new JSONObject(configInfo);
            redisTemplate.opsForValue().set("post:news:newsType", newsType);
        } catch (NacosException e) {
            log.error("监听新闻类型配置失败");
        }
    }

    @Override
    public JSONObject getNewsType() {
        JSONObject cachedConfigInfo = (JSONObject) redisTemplate.opsForValue().get("post:news:newsType");
        if (cachedConfigInfo != null) {
            return cachedConfigInfo;
        }

        String dataId = "news-type.json";
        String group = "DEFAULT_GROUP";
        try {
            String configInfo = nacosConfigManager.getConfigService()
                    .getConfig(dataId, group, 5000);
            JSONObject newsType = new JSONObject(configInfo);
            redisTemplate.opsForValue().set("post:news:newsType", newsType);
            return newsType;
        } catch (NacosException e) {
            throw new InternalError("获取新闻类型失败");
        }
    }

}
