package com.atcumt.model.comment.vo;

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
public class CommentPlusVO {
    private Long commentId;
    private String commentToUserId;
    private UserInfoSimpleVO userInfo;
    private String content;
    private List<MediaFileVO> mediaFiles;
    private Integer likeCount;
    private Integer dislikeCount;
    private Boolean liked;
    private Boolean disliked;
    private Integer replyCount;
    private Double score;
    private LocalDateTime createTime;
}
