package com.atcumt.model.user.vo;

import com.atcumt.model.user.entity.UserStatus;
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
    private String username;
    private String nickname;
    private String avatar;
    private String bio;
    private Integer gender;
    private String hometown;
    private String major;
    private Integer grade;
    private List<UserStatus> statuses;
    private Integer level;
    private Integer followersCount;
    private Integer followingCount;
    private Integer likeReceivedCount;
}
