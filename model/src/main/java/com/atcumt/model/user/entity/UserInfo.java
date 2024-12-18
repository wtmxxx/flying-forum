package com.atcumt.model.user.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "user_info")
@TypeAlias("UserInfo")
public class UserInfo {
    @MongoId
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
