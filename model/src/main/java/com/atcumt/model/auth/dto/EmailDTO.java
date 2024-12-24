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
public class EmailDTO {
    @Schema(description = "邮箱")
    private String email;
    @Schema(description = "验证码")
    private String verificationCode;
}
