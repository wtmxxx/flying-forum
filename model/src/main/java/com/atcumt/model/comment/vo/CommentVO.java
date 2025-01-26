package com.atcumt.model.comment.vo;

import com.atcumt.model.common.vo.MediaFileVO;
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
public class CommentVO {
    private Long commentId;
    private String postType;
    private Long postId;
    private String commentToUserId;
    private String userId;
    private String content;
    private List<MediaFileVO> mediaFiles;
    private Integer likeCount;
    private Integer dislikeCount;
    private Integer replyCount;
    private Double score;
    private LocalDateTime createTime;
}
