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
@Schema(description = "注册DTO，我会绑定所有传递的字段，不需要置null或者不传递")
public class RegisterDTO {
    @Schema(description = "统一身份认证临时Token")
    private String unifiedAuthToken;
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "密码")
    private String password;
    @Schema(description = "QQ_Authorization_Code")
    private String qqAuthorizationCode;
    @Schema(description = "Apple_Authorization_Code")
    private String appleAuthorizationCode;
}
