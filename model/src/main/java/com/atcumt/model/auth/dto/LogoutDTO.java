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
public class LogoutDTO {
    @Schema(description = "登出设备，默认登出当前会话，为ALL则登出全部设备", examples = {"ALL", "MOBILE_CLIENT"})
    private String device;
}
