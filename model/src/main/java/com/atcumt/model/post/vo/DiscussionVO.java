package com.atcumt.model.post.vo;

import com.atcumt.model.common.entity.MediaFile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DiscussionVO {
    private Long discussionId; // 帖子ID
    private String authorId;  // 作者ID
    private String title;  // 帖子标题
    private String content; // 帖子内容
    private List<MediaFile> mediaFiles; // 动态存储附件数据：图片、视频、文档等，每个媒体文件包含文件类型、URL等信息
    private List<Long> tagIds; // 标签ID
    private Integer likeCount; // 点赞数
    private Integer commentCount; // 评论数
    private Long viewCount;  // 观看量
    private Integer status;  // 帖子状态
    private String rejectReason;  // 帖子审核不通过原因
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
}
