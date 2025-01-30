package com.atcumt.model.user.entity;

import com.atcumt.model.common.entity.MediaFile;
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
    private MediaFile avatar;
    private String bio;
    private Integer gender;
    private String hometown;
    private String major;
    private Integer grade;
    private List<UserStatus> statuses;
    private Integer level;
    private Integer experience;
    private Integer followersCount;
    private Integer followingsCount;
    private Integer likeReceivedCount;
}
