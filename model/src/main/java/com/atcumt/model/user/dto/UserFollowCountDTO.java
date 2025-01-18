package com.atcumt.model.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserFollowCountDTO {
    private String followerId; // 关注者的用户ID
    private String followedId; // 被关注者的用户ID
}
