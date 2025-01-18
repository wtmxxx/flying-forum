package com.atcumt.model.user.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "user_privacy")
@TypeAlias("UserPrivacy")
public class UserPrivacy {
    @MongoId
    private Long privacyId;
    @Indexed
    private String userId;
    @Indexed
    private String privacyScope;
    private String privacyLevel;
    private List<String> specificUsers;
}
