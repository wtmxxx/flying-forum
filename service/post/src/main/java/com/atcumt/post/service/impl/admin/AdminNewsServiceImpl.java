package com.atcumt.post.service.impl.admin;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.ConfigType;
import com.alibaba.nacos.api.exception.NacosException;
import com.atcumt.common.enums.PermAction;
import com.atcumt.common.enums.PermModule;
import com.atcumt.common.utils.HeatScoreUtil;
import com.atcumt.common.utils.PermissionUtil;
import com.atcumt.model.post.dto.NewsDTO;
import com.atcumt.model.post.entity.News;
import com.atcumt.model.post.enums.PostStatus;
import com.atcumt.post.service.admin.AdminNewsService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminNewsServiceImpl implements AdminNewsService {
    private final MongoTemplate mongoTemplate;
    private final NacosConfigManager nacosConfigManager;
    private String imageRegex = "!\\[.*?]\\(\\s*(https?://[^\\s)]+?\\.(?:jpe?g|png|gif|jpeg)(?:[?#][^\\s)]*)?)\\s*\\)";

    @Override
    public void uploadNews(List<NewsDTO> newsDTOs) {
        // 权限鉴定
        StpUtil.checkPermission(PermissionUtil.generate(PermModule.NEWS, PermAction.CREATE));

        List<News> newsList = new ArrayList<>();

        for (var newsDTO : newsDTOs) {
            News news = BeanUtil.copyProperties(newsDTO, News.class);
            news.setNewsId(IdUtil.getSnowflakeNextId());
            // 从内容中提取图片
            Pattern pattern = Pattern.compile(imageRegex, Pattern.CASE_INSENSITIVE);
            if (news.getContent() != null) {
                Matcher matcher = pattern.matcher(news.getContent());

                if (matcher.find()) {
                    news.setImages(List.of(matcher.group(1)));
                }
            }
            news.setCommentCount(0);
            news.setViewCount(0L);
            news.setStatus(PostStatus.PUBLISHED.getCode());
            news.setScore(HeatScoreUtil.getPostHeat(0, 0, 0));
            newsList.add(news);
        }
        mongoTemplate.insert(newsList, News.class);
    }

    @Override
    public void uploadNewsType(JsonNode newsType) throws NacosException {
        // 权限鉴定
        StpUtil.checkPermission(PermissionUtil.generate(PermModule.NEWS, PermAction.CREATE));

        nacosConfigManager.getConfigService().publishConfig("news-type.json", "DEFAULT_GROUP", newsType.toString(), ConfigType.JSON.getType());
    }
}
