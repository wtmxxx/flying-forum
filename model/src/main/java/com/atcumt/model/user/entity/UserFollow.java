package com.atcumt.model.user.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "user_follow")
@TypeAlias("UserFollow")
public class UserFollow {
    @MongoId
    private Long followId;
    @Indexed
    private String followerId; // 关注者的用户ID
    @Indexed
    private String followedId; // 被关注者的用户ID
    @Indexed
    private LocalDateTime createTime;
}
