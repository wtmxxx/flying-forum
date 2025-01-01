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
public class ReplyPlusVO {
    private Long replyId;
    private Long replyToId;
    private String replyToUserId;
    private UserInfoSimpleVO replyToUserInfo;
    private Long rootCommentId;
    private UserInfoSimpleVO userInfo;
    private String content;
    private List<MediaFileVO> mediaFiles;
    private Integer likeCount;
    private Integer dislikeCount;
    private Boolean liked;
    private Boolean disliked;
    private Double score;
    private LocalDateTime createTime;
}
