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
public class UserPrivacyVO {
    private String userId;
    private String privacyScope;
    private String privacyLevel;
    private List<String> specificUsers;
}
