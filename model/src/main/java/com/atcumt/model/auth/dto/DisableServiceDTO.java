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
public class DisableServiceDTO {
    @Schema(description = "用户ID")
    private String userId;
    @Schema(description = "封禁服务")
    private String service;
    @Schema(description = "封禁时长(s), -1永久")
    private Long duration;
}
