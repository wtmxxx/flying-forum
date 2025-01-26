package com.atcumt.post.service.impl.admin;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.atcumt.common.utils.HeatScoreUtil;
import com.atcumt.model.post.dto.NewsDTO;
import com.atcumt.model.post.entity.News;
import com.atcumt.model.post.enums.PostStatus;
import com.atcumt.post.service.admin.AdminNewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminNewsServiceImpl implements AdminNewsService {
    private final MongoTemplate mongoTemplate;

    @Override
    public void uploadNews(List<NewsDTO> newsDTOs) {
        List<News> newsList = new ArrayList<>();

        for (var newsDTO : newsDTOs) {
            News news = BeanUtil.copyProperties(newsDTO, News.class);
            news.setNewsId(IdUtil.getSnowflakeNextId());
            news.setCommentCount(0);
            news.setViewCount(0L);
            news.setStatus(PostStatus.PUBLISHED.getCode());
            news.setScore(HeatScoreUtil.getPostHeat(0, 0, 0));
            newsList.add(news);
        }
        mongoTemplate.insert(newsList, News.class);
    }
}
