package com.atcumt.model.like.vo;

import com.atcumt.model.user.vo.UserInfoSimpleVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserLikeVO {
    private Long likeId;
    private String userId;
    private UserInfoSimpleVO userInfo;
    private LocalDateTime createTime;
}
