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
public class UserInfoVO {
    private String userId;
    private String nickname;
    private String avatar;
    private String banner;
    private String bio;
    private Integer gender;
    private Integer level;
    private Integer followersCount;
    private Integer followingCount;
    private Integer likeReceivedCount;
    private List<String> status;
}
