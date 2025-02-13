package com.atcumt.auth.controller.common.v1;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import com.atcumt.auth.service.AuthService;
import com.atcumt.common.exception.AuthorizationException;
import com.atcumt.common.exception.BadRequestException;
import com.atcumt.common.exception.UnauthorizedException;
import com.atcumt.model.auth.dto.*;
import com.atcumt.model.auth.enums.AuthMessage;
import com.atcumt.model.auth.vo.AuthenticationVO;
import com.atcumt.model.auth.vo.LinkedAccountVO;
import com.atcumt.model.auth.vo.SensitiveRecordVO;
import com.atcumt.model.auth.vo.TokenVO;
import com.atcumt.model.common.dto.TypePageQueryDTO;
import com.atcumt.model.common.entity.Result;
import com.atcumt.model.common.vo.PageQueryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("authControllerCommonV1")
@RequestMapping("/api/auth/v1")
@Tag(name = "Auth", description = "公共鉴权相关接口")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthService authService;

    @PostMapping("/authentication/unifiedAuth")
    @Operation(summary = "统一身份认证")
    @Parameters({
            @Parameter(name = "Cookie", description = "统一身份认证的cookie", in = ParameterIn.HEADER, required = true)
    })
    public Result<AuthenticationVO> authenticationByUnifiedAuth(@RequestHeader("Cookie") String cookie) throws AuthorizationException, UnauthorizedException {
        log.info("统一身份认证");

        AuthenticationVO authenticationVO = authService.authenticationByUnifiedAuth(cookie);

        if (authenticationVO != null) {
            // 返回authenticationVO临时Token
            return Result.success(authenticationVO);
        } else {
            // 系统错误
            return Result.failure(AuthMessage.SYSTEM_ERROR.getMessage());
        }
    }

    @PostMapping("/register")
    @Operation(summary = "注册")
    @Parameters({
            @Parameter(name = "Device-Type", description = "设备类型，尽量给出（同类型设备仅能登录一个）", in = ParameterIn.HEADER, required = true)
    })
    @SaIgnore
    public Result<TokenVO> register(@RequestBody RegisterDTO registerDTO) throws Exception {
        log.info("注册, username: {}", registerDTO.getUsername());

        String sid = authService.registerPreCheck(registerDTO);
        TokenVO tokenVO = authService.register(registerDTO, sid);

        return Result.success(tokenVO);
    }

    @PostMapping("/login/username")
    @Operation(summary = "用户名密码登录")
    @Parameters({
            @Parameter(name = "Device-Type", description = "设备类型，尽量给出（同类型设备仅能登录一个）", in = ParameterIn.HEADER, required = true)
    })
    public Result<TokenVO> loginByUsernamePassword(@RequestBody UsernamePasswordDTO usernamePasswordDTO) throws Exception {
        log.info("用户名密码登录, username: {}", usernamePasswordDTO.getUsername());

        // 用户名登录
        TokenVO tokenVO = authService.loginByUsernamePassword(usernamePasswordDTO.getUsername(), usernamePasswordDTO.getPassword());

        if (tokenVO != null) {
            // 返回auth Token
            return Result.success(tokenVO);
        } else {
            // 系统错误
            return Result.failure(AuthMessage.SYSTEM_ERROR.getMessage());
        }
    }

    @PostMapping("/login/qq")
    @Operation(summary = "QQ登录")
    @Parameters({
            @Parameter(name = "Device-Type", description = "设备类型，尽量给出（同类型设备仅能登录一个）", in = ParameterIn.HEADER, required = true)
    })
    public Result<TokenVO> loginByQQ(@RequestBody QqAuthorizationCodeDTO qqAuthorizationCode) throws AuthorizationException {
        log.info("QQ登录");

        // 用户名登录
        TokenVO tokenVO = authService.loginByQQ(qqAuthorizationCode.getQqAuthorizationCode());

        if (tokenVO != null) {
            // 返回auth Token
            return Result.success(tokenVO);
        } else {
            // 系统错误
            return Result.failure(AuthMessage.SYSTEM_ERROR.getMessage());
        }
    }

    @PostMapping("/login/apple")
    @Operation(summary = "Apple登录")
    @Parameters({
            @Parameter(name = "Device-Type", description = "设备类型，尽量给出（同类型设备仅能登录一个）", in = ParameterIn.HEADER, required = true)
    })
    public Result<TokenVO> loginByApple(@RequestBody AppleAuthorizationCodeDTO appleAuthorizationCodeDTO) throws Exception {
        log.info("Apple登录");

        // 用户名登录
        TokenVO tokenVO = authService.loginByApple(appleAuthorizationCodeDTO.getAppleAuthorizationCode());

        if (tokenVO != null) {
            // 返回auth Token
            return Result.success(tokenVO);
        } else {
            // 系统错误
            return Result.failure(AuthMessage.SYSTEM_ERROR.getMessage());
        }
    }

    @PatchMapping("/me/qq")
    @Operation(summary = "绑定QQ")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "qqAuthorizationCode", description = "QQ_Authorization_Code", required = true)
    })
    public Result<Object> bindQQ(@RequestHeader("Authorization") String accessToken, String qqAuthorizationCode) throws AuthorizationException {
        log.info("绑定QQ, accessToken: {}", accessToken);

        // 绑定QQ
        authService.bindQQ(qqAuthorizationCode);

        return Result.success();
    }

    @DeleteMapping("/me/qq")
    @Operation(summary = "解绑QQ")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<Object> unBindQQ(@RequestHeader("Authorization") String accessToken) {
        log.info("解绑QQ, accessToken: {}", accessToken);

        // 解绑QQ
        authService.unBindQQ();

        return Result.success();
    }

    @PatchMapping("/me/apple")
    @Operation(summary = "绑定Apple")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "appleAuthorizationCode", description = "Apple_Authorization_Code", required = true)
    })
    public Result<Object> bindApple(@RequestHeader("Authorization") String accessToken, @RequestBody AppleAuthorizationCodeDTO appleAuthorizationCodeDTO) throws Exception {
        log.info("绑定Apple, accessToken: {}", accessToken);

        // 绑定Apple
        authService.bindApple(appleAuthorizationCodeDTO.getAppleAuthorizationCode());

        return Result.success();
    }

    @DeleteMapping("/me/apple")
    @Operation(summary = "解绑Apple")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<Object> unBindApple(@RequestHeader("Authorization") String accessToken) {
        log.info("解绑Apple, accessToken: {}", accessToken);

        // 解绑Apple
        authService.unBindApple();

        return Result.success();
    }

    @PostMapping("/login/unifiedAuth")
    @Operation(summary = "统一身份认证登录（保留）")
    @Parameters({
            @Parameter(name = "Cookie", description = "统一身份认证的cookie", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "Device-Type", description = "设备类型，尽量给出（同类型设备仅能登录一个）", in = ParameterIn.HEADER, required = true)
    })
    public Result<TokenVO> loginBySchool(@RequestHeader("Cookie") String cookie) throws Exception {
        log.info("统一身份认证登录");

        TokenVO tokenVO = authService.loginByUnifiedAuth(cookie);

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

    @PostMapping("/verification-code/captcha")
    @Operation(summary = "发送验证码（需图形）")
    @Parameters({
            @Parameter(name = "email", description = "邮箱", required = true),
            @Parameter(name = "captchaId", description = "图形验证码ID", required = true),
            @Parameter(name = "captchaCode", description = "图形验证码内容", required = true)
    })
    public Result<Object> sendVerifyCodeWithCaptcha(@RequestBody VerificationCodeDTO verificationCodeDTO) throws Exception {
        log.info("发送验证码（需图形）, email: {}", verificationCodeDTO.getEmail());

        authService.sendVerifyCodeWithCaptcha(
                verificationCodeDTO.getEmail(),
                verificationCodeDTO.getCaptchaId(),
                verificationCodeDTO.getCaptchaCode()
        );

        return Result.success("验证码已成功发送");
    }

    @PostMapping("/verification-code")
    @Operation(summary = "发送验证码")
    public Result<Object> sendVerifyCode(@RequestBody VerificationCodeDTO verificationCodeDTO) throws Exception {
        log.info("发送验证码, email: {}", verificationCodeDTO.getEmail());

        authService.sendVerifyCode(verificationCodeDTO.getEmail());

        return Result.success("验证码已成功发送");
    }

    @PostMapping("/me/email")
    @Operation(summary = "首次绑定邮箱（保留）")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<Object> bingEmail(@RequestHeader("Authorization") String accessToken, @RequestBody EmailDTO emailDTO) throws Exception {
        log.info("绑定邮箱, accessToken: {}", accessToken);

        authService.bindEmail(StpUtil.getLoginIdAsString(), emailDTO.getEmail(), emailDTO.getVerificationCode());

        return Result.success();
    }

    @PatchMapping("/me/email")
    @Operation(summary = "修改邮箱（保留）")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "verificationCode", description = "验证码", required = true),
            @Parameter(name = "email", description = "邮箱", required = true)
    })
    public Result<Object> changeEmail(@RequestHeader("Authorization") String accessToken, String verificationCode, String email) {
        log.info("修改邮箱, accessToken: {}", accessToken);

        authService.changeEmail(verificationCode, email);

        return Result.success();
    }

    @PostMapping("/login/email")
    @Operation(summary = "邮箱验证码登录（保留）")
    @Parameters({
            @Parameter(name = "Device-Type", description = "设备类型，尽量给出（同类型设备仅能登录一个）", in = ParameterIn.HEADER, required = true)
    })
    @SaIgnore
    public Result<TokenVO> loginByEmailVerificationCode(@RequestBody EmailDTO emailDTO) throws Exception {
        log.info("邮箱验证码登录");

        // 邮箱验证码登录
        TokenVO tokenVO = authService.loginByEmailVerificationCode(emailDTO.getEmail(), emailDTO.getVerificationCode());

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
            @Parameter(name = "Authorization", description = "授权Token", required = true)
    })
    public Result<TokenVO> refreshToken(@RequestHeader("Authorization") String accessToken, @RequestBody RefreshTokenDTO refreshTokenDTO) {
        log.info("使用RefreshToken刷新accessToken, 原accessToken: {}", accessToken);

        TokenVO tokenVO = authService.refreshToken(refreshTokenDTO.getRefreshToken());

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
    public Result<Object> logout(@RequestHeader("Authorization") String accessToken, String device) {
        log.info("账号登出, accessToken: {}, device: {}", accessToken, device);
        authService.logout(device);

        return Result.success();
    }

    @GetMapping("/loginDevices")
    @Operation(summary = "获取所有登录设备")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<List<String>> getDevices(@RequestHeader("Authorization") String accessToken) {
        log.info("获取所有登录设备, accessToken: {}", accessToken);

        List<String> loginDevices = authService.getLoginDevices();

        return Result.success(loginDevices);
    }

    @PatchMapping("/me/username")
    @Operation(summary = "修改用户名")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "username", description = "用户名", required = true)
    })
    public Result<Object> changeUsername(@RequestHeader("Authorization") String accessToken, String username) throws BadRequestException {
        log.info("修改用户名, accessToken: {}", accessToken);

        authService.changeUsername(username);

        return Result.success();
    }

    @PatchMapping("/me/password")
    @Operation(summary = "修改密码")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "oldPassword", description = "原密码", required = true),
            @Parameter(name = "newPassword", description = "新密码", required = true)
    })
    public Result<Object> changePassword(@RequestHeader("Authorization") String accessToken, String oldPassword, String newPassword) throws Exception {
        log.info("修改密码, accessToken: {}", accessToken);

        authService.changePassword(oldPassword, newPassword);

        return Result.success();
    }

    @PatchMapping("/me/password/reset/unifiedAuth")
    @Operation(summary = "统一身份认证重置密码", description = "通过统一身份认证，重置密码，忘记密码")
    @Parameters({
            @Parameter(name = "Cookie", description = "统一身份认证的cookie", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "password", description = "新密码", required = true)
    })
    public Result<Object> resetPassword(@RequestHeader("Cookie") String cookie, String password) throws Exception {
        log.info("重置密码");

        authService.resetPassword(cookie, password);

        return Result.success();
    }

    @DeleteMapping("/account")
    @Operation(summary = "注销账号", description = "注销账号")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "password", description = "密码", required = true)
    })
    public Result<Object> deleteAccount(@RequestHeader("Authorization") String accessToken, String password) throws Exception {
        log.info("注销账号, accessToken: {}", accessToken);

        authService.deleteAccount(password);

        return Result.success();
    }

    @PostMapping("/sensitiveRecord")
    @Operation(summary = "获取敏感记录", description = "获取敏感操作记录，如登录、修改密码（近30天）")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<PageQueryVO<SensitiveRecordVO>> getSensitiveRecord(@RequestHeader("Authorization") String accessToken, @RequestBody TypePageQueryDTO typePageQueryDTO) {
        log.info("获取敏感记录, accessToken: {}", accessToken);

        typePageQueryDTO.checkParam();

        PageQueryVO<SensitiveRecordVO> recordPage = authService.getSensitiveRecord(typePageQueryDTO);

        return Result.success(recordPage);
    }

    @GetMapping("/me/linkedAccount")
    @Operation(summary = "获取账号绑定信息", description = "获取账号绑定信息，邮箱、QQ、Apple")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<LinkedAccountVO> getLinkedAccount(@RequestHeader("Authorization") String accessToken) {
        log.info("获取账号绑定信息, accessToken: {}", accessToken);

        LinkedAccountVO linkedAccountVO = authService.getLinkedAccount();

        return Result.success(linkedAccountVO);
    }

    @GetMapping("/me/username")
    @Operation(summary = "获取用户名", description = "获取用户名")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<String> getUsername(@RequestHeader("Authorization") String accessToken) {
        log.info("获取用户名, accessToken: {}", accessToken);

        String username = authService.getUsername();

        return Result.success(username);
    }
}
