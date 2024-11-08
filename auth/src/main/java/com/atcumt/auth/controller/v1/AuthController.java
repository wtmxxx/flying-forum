package com.atcumt.auth.controller.v1;

import com.atcumt.auth.service.AuthService;
import com.atcumt.model.common.AuthMessage;
import com.atcumt.model.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("gptControllerV1")
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "鉴权相关接口")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register/school")
    @Operation(summary = "用户注册")
    @Parameters({
            @Parameter(name = "Authorization", description = "统一身份认证Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<String> registerBySchool(@RequestHeader("Authorization") String token) throws Exception {
        log.info("用户注册");

        String jwt = authService.registerBySchool(token);

        if (jwt != null) {
            // 返回JWT Token
            return Result.success(jwt);
        } else {
            // 系统错误
            return Result.failure(AuthMessage.SYSTEM_ERROR.getMessage());
        }
    }
}
