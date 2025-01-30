package com.atcumt.model.post.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "news")
@TypeAlias("News")
@CompoundIndexes({
        @CompoundIndex(name = "publishTime_id_idx", def = "{'publishTime': -1, '_id': -1}"),
        @CompoundIndex(name = "score_publishTime_id_idx", def = "{'score': -1, 'publishTime': -1, '_id': -1}")
})
public class News {
    @MongoId
    private Long newsId;
    @Indexed
    private String newsCategory;  // 新闻分类
    @Indexed
    private String newsType;  // 新闻类型
    @Indexed
    private String sourceName;  // 来源名称
    private String sourceUrl;  // 来源链接
    private String showType;  // 展示类型
    private String title;  // 帖子标题
    private String content; // 帖子内容
    private Integer commentCount; // 评论数
    private Long viewCount;  // 观看量
    @Indexed
    private String status;  // 帖子状态
    private Double score;  // 帖子评分
    private LocalDateTime publishTime; // 发布时间
}
