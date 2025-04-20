package com.atcumt.model.post.vo;

import com.atcumt.model.common.vo.MediaFileVO;
import com.atcumt.model.user.vo.UserInfoSimpleVO;
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
public class QuestionVO {
    private Long questionId; // 帖子ID
    private String userId;  // 作者ID
    private UserInfoSimpleVO userInfo; // 作者信息
    private String title;  // 帖子标题
    private String content; // 帖子内容
    private List<MediaFileVO> mediaFiles; // 动态存储附件数据：图片、视频、文档等，每个媒体文件包含文件类型、URL等信息
    private List<TagSimpleVO> tags; // 标签ID
    private Integer likeCount; // 点赞数
    private Integer dislikeCount; // 点踩数
    private Integer commentCount; // 评论数
    private Long viewCount;  // 观看量
    private String status;  // 帖子状态
    private String rejectReason;  // 帖子审核不通过原因
    private Double score;  // 帖子评分
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
}
