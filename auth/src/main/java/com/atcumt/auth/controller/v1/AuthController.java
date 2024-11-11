package com.atcumt.auth.controller.v1;

import cn.dev33.satoken.stp.StpUtil;
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

@RestController("authControllerV1")
@RequestMapping("/api/auth/v1")
@Tag(name = "Auth", description = "鉴权相关接口")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register/school")
    @Operation(summary = "统一身份认证注册")
    @Parameters({
            @Parameter(name = "Unified-Auth", description = "统一身份认证Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<String> registerBySchool(@RequestHeader("Unified-Auth") String token) throws Exception {
        log.info("统一身份认证注册");

        String authToken = authService.registerBySchool(token);

        if (authToken != null) {
            // 返回auth Token
            return Result.success(authToken);
        } else {
            // 系统错误
            return Result.failure(AuthMessage.SYSTEM_ERROR.getMessage());
        }
    }

    @PostMapping("/login/school")
    @Operation(summary = "统一身份认证登录")
    @Parameters({
            @Parameter(name = "Unified-Auth", description = "统一身份认证Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<String> loginBySchool(@RequestHeader("Unified-Auth") String token) throws Exception {
        log.info("统一身份认证登录");

        String authToken = authService.loginBySchool(token);

        if (authToken != null) {
            // 返回JWT Token
            return Result.success(authToken);
        } else {
            // 系统错误
            return Result.failure(AuthMessage.SYSTEM_ERROR.getMessage());
        }
    }

    // 查询登录状态
    @PostMapping("/isLogin")
    public String isLogin() {
        System.out.println(
                StpUtil.getLoginId()
        );
        return "当前会话是否登录：" + StpUtil.isLogin();
    }
}
