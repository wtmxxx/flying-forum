package com.atcumt.model.post.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NewsVO {
    private Long newsId;
    private String newsCategory;  // 新闻分类
    private String newsType;  // 新闻类型
    private String sourceName;  // 来源名称
    private String sourceUrl;  // 来源链接
    private String showType;  // 展示类型
    private String title;  // 帖子标题
    private String content; // 帖子内容
    private Integer commentCount; // 评论数
    private Long viewCount;  // 观看量
    private String status;  // 帖子状态
    private Double score;  // 帖子评分
    private LocalDateTime publishTime; // 发布时间
}
