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
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;

//@RestController("authControllerCommonV1")
//@RequestMapping("/api/auth/v1")
//@Tag(name = "Auth", description = "公共鉴权相关接口")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register/school")
    @Operation(summary = "统一身份认证注册")
    @Parameters({
            @Parameter(name = "Unified-Auth", description = "统一身份认证Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "Device-Type", description = "设备类型，尽量给出（同类型设备仅能登录一个）", in = ParameterIn.HEADER, required = true)
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
            @Parameter(name = "Unified-Auth", description = "统一身份认证Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "Device-Type", description = "设备类型，尽量给出（同类型设备仅能登录一个）", in = ParameterIn.HEADER, required = true)
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
    public Result<Object> bingUsernamePassword(@RequestHeader("Authorization") String accessToken, String username, String password) throws Exception {
        log.info("绑定用户名, accessToken: {}", accessToken);

        authService.bindUsername(StpUtil.getLoginIdAsString(), username, password);

        return Result.success();
    }

    @PostMapping("/login/username")
    @Operation(summary = "用户名密码登录")
    @Parameters({
            @Parameter(name = "username", description = "用户名", required = true),
            @Parameter(name = "password", description = "密码", required = true),
            @Parameter(name = "Device-Type", description = "设备类型，尽量给出（同类型设备仅能登录一个）", in = ParameterIn.HEADER, required = true)
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

    @GetMapping("/captcha")
    @Operation(summary = "获取图形验证码")
    public void sendCaptcha(HttpServletResponse response) throws Exception {
        log.info("获取图形验证码");

        authService.sendCaptcha(response);
    }

    @PostMapping("/verification-code")
    @Operation(summary = "发送验证码")
    @Parameters({
            @Parameter(name = "email", description = "邮箱", required = true),
            @Parameter(name = "captchaId", description = "图形验证码ID", required = true),
            @Parameter(name = "captchaCode", description = "图形验证码内容", required = true)
    })
    public Result<Object> sendVerifyCode(String email, String captchaId, String captchaCode) throws Exception {
        log.info("发送验证码, email: {}", email);

        authService.SendVerifyCode(email, captchaId, captchaCode);

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
            @Parameter(name = "verificationCode", description = "验证码", required = true),
            @Parameter(name = "Device-Type", description = "设备类型，尽量给出（同类型设备仅能登录一个）", in = ParameterIn.HEADER, required = true)
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
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "device", description = "登出设备，默认全部登出")
    })
    public Result<Object> logout(@RequestHeader("Authorization") String accessToken, String device) throws Exception {
        log.info("账号登出, accessToken: {}, device: {}", accessToken, device);

        authService.logout(device);

        return Result.success();
    }

    @PutMapping("/me/username")
    @Operation(summary = "修改用户名")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "Unified-Auth", description = "统一身份认证Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "username", description = "用户名", required = true)
    })
    public Result<Object> updateUsername(@RequestHeader("Authorization") String accessToken, @RequestHeader("Unified-Auth") String unifiedToken, String username) throws Exception {
        log.info("修改用户名, accessToken: {}", accessToken);

        authService.updateUsername(unifiedToken, StpUtil.getLoginIdAsString(), username);

        return Result.success();
    }

    @PutMapping("/me/password")
    @Operation(summary = "修改密码")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "Unified-Auth", description = "统一身份认证Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "password", description = "密码", required = true)
    })
    public Result<Object> updatePassword(@RequestHeader("Authorization") String accessToken, @RequestHeader("Unified-Auth") String unifiedToken, String password) {
        log.info("修改密码, accessToken: {}", accessToken);

        authService.updatePassword(unifiedToken, StpUtil.getLoginIdAsString(), password);

        return Result.success();
    }

    @PutMapping("/me/email")
    @Operation(summary = "修改邮箱")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "Unified-Auth", description = "统一身份认证Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "verificationCode", description = "验证码", required = true),
            @Parameter(name = "email", description = "邮箱", required = true)
    })
    public Result<Object> updateEmail(@RequestHeader("Authorization") String accessToken, @RequestHeader("Unified-Auth") String unifiedToken, String verificationCode, String email) {
        log.info("修改邮箱, accessToken: {}", accessToken);

        authService.updateEmail(unifiedToken, StpUtil.getLoginIdAsString(), verificationCode, email);

        return Result.success();
    }
}
