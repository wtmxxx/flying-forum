package com.atcumt.model.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "用户隐私DTO")
public class UserPrivacyDTO {
    private String userId;
    @Schema(description = "隐私范围(FOLLOWING/FOLLOWER/LIKE/COLLECTION)")
    private String privacyScope;
    @Schema(description = "隐私级别(PUBLIC/FOLLOWERS_ONLY/MUTUAL_FOLLOWERS_ONLY/SPECIFIC_USERS_ONLY)")
    private String privacyLevel;
    @Schema(description = "指定用户列表")
    private List<String> specificUsers;
}
