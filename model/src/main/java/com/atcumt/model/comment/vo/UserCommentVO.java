package com.atcumt.model.comment.vo;

import com.atcumt.model.user.vo.UserInfoSimpleVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserCommentVO {
    private UserInfoSimpleVO userInfo;
    private Integer size;
    private String cursor;
    private Long lastCommentId;
    private List<CommentVO> comments;
}
