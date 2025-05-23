package com.atcumt.model.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AppleAuthorizationCodeDTO {
    private String appleAuthorizationCode;
}
