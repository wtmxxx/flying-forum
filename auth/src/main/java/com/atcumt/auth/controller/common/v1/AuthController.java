package com.atcumt.auth.controller.common.v1;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import com.atcumt.auth.service.AuthService;
import com.atcumt.model.auth.vo.TokenVO;
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

@RestController("authControllerCommonV1")
@RequestMapping("/api/auth/v1")
@Tag(name = "Auth", description = "公共鉴权相关接口")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register/school")
    @Operation(summary = "统一身份认证注册")
    @Parameters({
            @Parameter(name = "Unified-Auth", description = "统一身份认证Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<TokenVO> registerBySchool(@RequestHeader("Unified-Auth") String token) throws Exception {
        log.info("统一身份认证注册");

        TokenVO tokenVO = authService.registerBySchool(token);

        if (tokenVO != null) {
            // 返回auth Token
            return Result.success(tokenVO);
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
    public Result<TokenVO> loginBySchool(@RequestHeader("Unified-Auth") String token) throws Exception {
        log.info("统一身份认证登录");

        TokenVO tokenVO = authService.loginBySchool(token);

        if (tokenVO != null) {
            // 返回auth Token
            return Result.success(tokenVO);
        } else {
            // 系统错误
            return Result.failure(AuthMessage.SYSTEM_ERROR.getMessage());
        }
    }

    @PostMapping("/me/username")
    @Operation(summary = "首次绑定用户名和密码")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "username", description = "用户名", required = true),
            @Parameter(name = "password", description = "密码", required = true)
    })
    public Result<Object> bingUsername(@RequestHeader("Authorization") String accessToken, String username, String password) throws Exception {
        log.info("绑定用户名, accessToken: {}", accessToken);

        authService.bindUsername(StpUtil.getLoginIdAsString(), username, password);

        return Result.success();
    }

    @PostMapping("/login/username")
    @Operation(summary = "用户名密码登录")
    @Parameters({
            @Parameter(name = "username", description = "用户名", required = true),
            @Parameter(name = "password", description = "密码", required = true)
    })
    @SaIgnore
    public Result<TokenVO> loginByUsernamePassword(String username, String password) throws Exception {
        log.info("用户名密码登录");

        // 用户名登录
        TokenVO tokenVO = authService.loginByUsernamePassword(username, password);

        if (tokenVO != null) {
            // 返回auth Token
            return Result.success(tokenVO);
        } else {
            // 系统错误
            return Result.failure(AuthMessage.SYSTEM_ERROR.getMessage());
        }
    }

    @PostMapping("/verification-code")
    @Operation(summary = "发送验证码")
    @Parameters({
            @Parameter(name = "email", description = "邮箱", required = true)
    })
    public Result<Object> sendVerifyCode(String email) throws Exception {
        log.info("发送验证码, email: {}", email);

        authService.SendVerifyCode(email);

        return Result.success("验证码已成功发送");
    }

    @PostMapping("/me/email")
    @Operation(summary = "首次绑定邮箱")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "email", description = "邮箱", required = true),
            @Parameter(name = "verificationCode", description = "验证码", required = true)
    })
    public Result<Object> bingEmail(@RequestHeader("Authorization") String accessToken, String email, String verificationCode) throws Exception {
        log.info("绑定邮箱, accessToken: {}", accessToken);

        authService.bindEmail(StpUtil.getLoginIdAsString(), email, verificationCode);

        return Result.success();
    }

    @PostMapping("/login/email")
    @Operation(summary = "邮箱验证码登录")
    @Parameters({
            @Parameter(name = "email", description = "邮箱", required = true),
            @Parameter(name = "verificationCode", description = "验证码", required = true)
    })
    @SaIgnore
    public Result<TokenVO> loginByEmailVerificationCode(String email, String verificationCode) throws Exception {
        log.info("邮箱验证码登录");

        // 邮箱验证码登录
        TokenVO tokenVO = authService.loginByEmailVerificationCode(email, verificationCode);

        if (tokenVO != null) {
            // 返回auth Token
            return Result.success(tokenVO);
        } else {
            // 系统错误
            return Result.failure(AuthMessage.SYSTEM_ERROR.getMessage());
        }
    }

    @PostMapping("/refresh_token")
    @Operation(summary = "更新accessToken", description = "使用RefreshToken刷新accessToken(Authorization)，refreshToken也会被刷新")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", required = true),
            @Parameter(name = "refreshToken", description = "刷新Token", required = true)
    })
    public Result<TokenVO> refreshToken(@RequestHeader("Authorization") String accessToken, String refreshToken) throws Exception {
        log.info("使用RefreshToken刷新accessToken, 原accessToken: {}", accessToken);

        TokenVO tokenVO = authService.refreshToken(refreshToken);

        if (tokenVO != null) {
            // 返回auth Token
            return Result.success(tokenVO);
        } else {
            // 系统错误
            return Result.failure(AuthMessage.SYSTEM_ERROR.getMessage());
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "账号登出")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<Object> logout(@RequestHeader("Authorization") String accessToken) throws Exception {
        log.info("账号登出, accessToken: {}", accessToken);

        authService.logout();

        return Result.success();
    }
}
