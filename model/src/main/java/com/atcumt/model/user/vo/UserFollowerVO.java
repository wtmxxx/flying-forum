package com.atcumt.model.user.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserFollowerVO {
    private Integer size;
    private String cursor;
    private Long lastFollowId;
    private List<UserInfoSimpleVO> followers;
}
