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
public class UserReplyVO {
    private UserInfoSimpleVO userInfo;
    private Integer size;
    private String cursor;
    private Long lastReplyId;
    private List<ReplyVO> replies;
}
