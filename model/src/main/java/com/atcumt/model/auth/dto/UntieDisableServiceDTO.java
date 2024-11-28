package com.atcumt.model.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UntieDisableServiceDTO {
    @Schema(description = "用户ID")
    private String userId;
    @Schema(description = "解封服务")
    private String service;
}
