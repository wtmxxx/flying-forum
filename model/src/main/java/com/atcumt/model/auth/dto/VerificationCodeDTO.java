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
public class VerificationCodeDTO {
    @Schema(description = "邮箱")
    private String email;
    @Schema(description = "图形验证码ID")
    private String captchaId;
    @Schema(description = "图形验证码内容")
    private String captchaCode;
}
