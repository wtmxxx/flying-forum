package com.atcumt.model.user.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoSimpleVO {
    private String userId;
    private String nickname;
    private String avatar;
    private String bio;
    private Integer level;
}
