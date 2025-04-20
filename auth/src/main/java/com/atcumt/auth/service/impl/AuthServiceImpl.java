package com.atcumt.auth.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.session.SaTerminalInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLogoutParameter;
import cn.dev33.satoken.stp.parameter.enums.SaLogoutRange;
import cn.hutool.captcha.AbstractCaptcha;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.convert.ConvertException;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.jwt.JWT;
import com.atcumt.auth.api.client.PortalClient;
import com.atcumt.auth.api.client.SchoolYktClient;
import com.atcumt.auth.mapper.*;
import com.atcumt.auth.service.AuthService;
import com.atcumt.auth.utils.AppleAuthUtil;
import com.atcumt.auth.utils.AuthUtil;
import com.atcumt.auth.utils.EmailUtil;
import com.atcumt.auth.utils.RefreshTokenUtil;
import com.atcumt.common.enums.RoleType;
import com.atcumt.common.exception.AuthorizationException;
import com.atcumt.common.exception.BadRequestException;
import com.atcumt.common.exception.TooManyRequestsException;
import com.atcumt.common.exception.UnauthorizedException;
import com.atcumt.common.utils.Argon2Util;
import com.atcumt.common.utils.WebUtil;
import com.atcumt.model.auth.dto.QqAccessTokenDTO;
import com.atcumt.model.auth.dto.QqOpenIdDTO;
import com.atcumt.model.auth.dto.RegisterDTO;
import com.atcumt.model.auth.entity.*;
import com.atcumt.model.auth.enums.AuthMessage;
import com.atcumt.model.auth.enums.AuthenticationType;
import com.atcumt.model.auth.enums.EncryptionType;
import com.atcumt.model.auth.vo.*;
import com.atcumt.model.common.dto.TypePageQueryDTO;
import com.atcumt.model.common.vo.PageQueryVO;
import com.atcumt.model.search.dto.SearchUserDTO;
import com.atcumt.model.user.entity.UserInfo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.seata.spring.annotation.GlobalTransactional;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl extends ServiceImpl<AuthMapper, UserAuth> implements AuthService {
    private final AuthMapper authMapper;
    private final SchoolYktClient schoolYktClient;
    private final RefreshTokenUtil refreshTokenUtil;
    private final UserRoleMapper userRoleMapper;
    private final EmailUtil emailUtil;
    private final RedisTemplate<String, Integer> redisIntegerTemplate;
    private final RedisTemplate<String, String> redisStringTemplate;
    private final PortalClient portalClient;
    private final QqAuthMapper qqAuthMapper;
    private final MongoTemplate mongoTemplate;
    private final WebClient webClient;
    private final SensitiveRecordMapper sensitiveRecordMapper;
    private final AppleAuthMapper appleAuthMapper;
    private final AppleAuthUtil appleAuthUtil;
    private final AuthUtil authUtil;
    private final ObjectMapper objectMapper;
    private final RocketMQTemplate rocketMQTemplate;

    @Value("${qq.app-id}")
    private String qqAppId;
    @Value("${qq.app-key}")
    private String qqAppKey;
    @Value("${qq.redirect-uri}")
    private String qqRedirectUri;

    @Override
    @GlobalTransactional
    @Deprecated
    public TokenVO registerBySchool(String schoolToken) throws AuthorizationException, UnauthorizedException {
        // 使用token获取学号
        String sid = getSidByToken(schoolToken);

        UserAuth userAuth = authMapper.selectOne(
                Wrappers.<UserAuth>lambdaQuery()
                        .eq(UserAuth::getSid, sid)
        );

        // 查看学号是否已注册
        if (userAuth != null)
            throw new AuthorizationException(AuthMessage.SID_ALREADY_REGISTERED.getMessage(), 409);
        else userAuth = UserAuth
                .builder()
                .sid(sid)
                .build();

        authMapper.insert(userAuth);

        userRoleMapper.insert(UserRole
                .builder()
                .userId(userAuth.getUserId())
                .roleId("user")
                .build()
        );

        // 注册成功，进行登录，返回token
        authUtil.login(userAuth.getUserId());

        return TokenVO
                .builder()
                .userId(userAuth.getUserId())
                .accessToken(StpUtil.getTokenValue())
                .expiresIn(StpUtil.getTokenTimeout())
                .refreshToken(refreshTokenUtil.generateRefreshToken())
                .build();

        // 创建JWT Token
        // return jwtTool.createToken(userAuth.getUserId());
    }

    @Override
    @GlobalTransactional
    @Deprecated
    public TokenVO loginBySchool(String schoolToken) throws AuthorizationException, UnauthorizedException {
        // 使用token获取学号
        String sid = getSidByToken(schoolToken);

        UserAuth userAuth = authMapper.selectOne(
                Wrappers.<UserAuth>lambdaQuery()
                        .eq(UserAuth::getSid, sid)
        );

        // 查看学号是否已注册
        if (userAuth == null) throw new AuthorizationException(AuthMessage.UNIFIED_AUTH_FAILURE.getMessage(), 404);

        authUtil.login(userAuth.getUserId());

        return TokenVO
                .builder()
                .userId(userAuth.getUserId())
                .accessToken(StpUtil.getTokenValue())
                .expiresIn(StpUtil.getTokenTimeout())
                .refreshToken(refreshTokenUtil.generateRefreshToken())
                .build();
    }

    @Override
    public TokenVO refreshToken(String refreshToken) throws UnauthorizedException {
        return refreshTokenUtil.getAccessToken(refreshToken);
    }

    @Override
    public void logout(String device) {
        refreshTokenUtil.deleteRefreshToken();
        if (device == null || device.isEmpty() || "TOKEN".equalsIgnoreCase(device)) {
            // 只注销当前 token 的会话
            StpUtil.logout(new SaLogoutParameter().setRange(SaLogoutRange.TOKEN));
        } else if ("ACCOUNT".equalsIgnoreCase(device)) {
            StpUtil.logout(new SaLogoutParameter().setRange(SaLogoutRange.ACCOUNT));
        } else {
            StpUtil.logout(new SaLogoutParameter().setDeviceType(device));
        }
    }

    @Override
    @Deprecated
    public void bindUsername(String userId, String username, String password) {
        // 验证密码合法性
        validatePassword(password);
        // 验证用户名合法性
        validateUsername(username);

        // 生成加密密码
        String pw_hash = EncryptionType.ARGON2.getTypeWithBraces() + Argon2Util.hash(password);
        authMapper.update(Wrappers
                .<UserAuth>lambdaUpdate()
                .eq(UserAuth::getUserId, userId)
                .set(UserAuth::getUsername, username)
                .set(UserAuth::getPassword, pw_hash)
        );
    }

    @Override
    public TokenVO loginByUsernamePassword(String username, String password) throws Exception {
        // 从数据库查询用户名和密码
        UserAuth userAuth = authMapper.selectOne(Wrappers
                .<UserAuth>lambdaQuery()
                .eq(UserAuth::getUsername, username)
        );

        // 检查用户名是否存在
        if (Objects.isNull(userAuth)) throw new AuthorizationException(AuthMessage.USERNAME_NOT_EXISTS.getMessage());

        // 获取储存的加密密码
        String storedHash = userAuth.getPassword();

        // 验证密码并登录
        if (!verifyPassword(storedHash, password)) {
            throw new AuthorizationException(AuthMessage.PASSWORD_INCORRECT.getMessage());
        }
        // 密码正确，登录
        authUtil.login(userAuth.getUserId());
        // 统一返回 Token
        return TokenVO.builder()
                .userId(userAuth.getUserId())
                .accessToken(StpUtil.getTokenValue())
                .expiresIn(StpUtil.getTokenTimeout())
                .refreshToken(refreshTokenUtil.generateRefreshToken())
                .build();
    }

    @Override
    public void sendVerifyCode(String email) throws Exception {
        // 检查邮箱合法性
        if (!Validator.isEmail(email)) {
            throw new IllegalArgumentException(AuthMessage.INVALID_EMAIL_FORMAT.getMessage());
        }

        String verificationCodeKey = "Authorization:verification-code:" + email;
        String verificationLimitKey = "Authorization:verification-limit:" + email;

        Integer verificationLimitNum = redisIntegerTemplate.opsForValue().get(verificationLimitKey);

        // 获取验证码次数限制 超过则限制
        if (Objects.nonNull(verificationLimitNum) && verificationLimitNum > 3) {
            throw new TooManyRequestsException(AuthMessage.VERIFICATION_CODE_REQUEST_TOO_FREQUENT.getMessage());
        }
        // 增加一次获取验证码次数
        redisIntegerTemplate.opsForValue().increment(verificationLimitKey);
        redisIntegerTemplate.expire(verificationLimitKey, 1, TimeUnit.MINUTES);

        // 使用Security生成安全的随机验证码
        SecureRandom RANDOM = new SecureRandom();
        int TOKEN_LENGTH = 6; // Token长度
        String CHARACTERS = "0123456789"; // 随机用序列码
        StringBuilder sb = new StringBuilder(TOKEN_LENGTH);
        for (int i = 0; i < TOKEN_LENGTH; i++) {
            int index = RANDOM.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        String verificationCodeValue = sb.toString();

        // 将验证码存放进Redis（15分钟）
        redisStringTemplate.opsForValue().set(verificationCodeKey, verificationCodeValue, 15, TimeUnit.MINUTES);

        // 发送验证码
        String emailContent =
                "尊敬的用户，您好：\n\n" +
                        "感谢您使用我们的服务！\n\n" +
                        "您的验证码为：" + verificationCodeValue + "\n\n" +
                        "为了保障您的账户安全，请注意以下事项：\n" +
                        "1. 此验证码仅用于本次操作（如修改密码、登录账号等），请勿泄露给他人。\n" +
                        "2. 如果您未发起此操作，请忽略此邮件或尽快修改您的账户密码，以确保账户安全。\n\n" +
                        "本验证码有效期为15分钟，请及时完成操作。\n\n" +
                        "祝您生活愉快！\n\n" +
                        "此致\n" +
                        "矿小圈服务团队";

        try {
            emailUtil.sendEmail(email, "矿小圈验证码", emailContent, false);
        } catch (MessagingException e) {
            throw new Exception(AuthMessage.VERIFICATION_CODE_SEND_FAILURE.getMessage());
        }
    }

    @Override
    public void sendVerifyCodeWithCaptcha(String email, String captchaId, String captchaCode) throws Exception {
        // 检查图形验证码
        String captchaKey = "Authorization:captcha:" + captchaId;
        String realCaptchaCode = redisStringTemplate.opsForValue().get(captchaKey);

        redisStringTemplate.delete(captchaKey);

        if (Objects.nonNull(realCaptchaCode) && !Objects.equals(captchaCode.toLowerCase(), realCaptchaCode.toLowerCase())) {
            throw new IllegalArgumentException(AuthMessage.CAPTCHA_CODE_INCORRECT.getMessage());
        }

        sendVerifyCode(email);
    }

    @Override
    public void bindEmail(String userId, String email, String verificationCode) {
        // 检查邮箱和验证码
        validateVerificationCode(email, verificationCode);

        authMapper.update(Wrappers
                .<UserAuth>lambdaUpdate()
                .eq(UserAuth::getUserId, userId)
                .set(UserAuth::getEmail, email)
        );
    }

    @Override
    public TokenVO loginByEmailVerificationCode(String email, String verificationCode) throws AuthorizationException {
        // 检查邮箱和验证码
        validateVerificationCode(email, verificationCode);

        UserAuth userAuth = authMapper.selectOne(Wrappers
                .<UserAuth>lambdaQuery()
                .eq(UserAuth::getEmail, email)
        );
        // 检查邮箱是否存在
        if (Objects.isNull(userAuth)) throw new AuthorizationException(AuthMessage.EMAIL_NOT_EXISTS.getMessage());

        authUtil.login(userAuth.getUserId());
        return TokenVO
                .builder()
                .userId(userAuth.getUserId())
                .accessToken(StpUtil.getTokenValue())
                .expiresIn(StpUtil.getTokenTimeout())
                .refreshToken(refreshTokenUtil.generateRefreshToken())
                .build();
    }

    @Override
    @Deprecated
    public void updateUsername(String unifiedToken, String userId, String username) throws BadRequestException, UnauthorizedException {
        // 验证频率限制
        String usernameChangeKey = "Authorization:usernameChange:" + userId;
        Integer usernameChangeValue = redisIntegerTemplate.opsForValue().get(usernameChangeKey);

        if (usernameChangeValue != null && usernameChangeValue >= 1)
            throw new BadRequestException(AuthMessage.USERNAME_CHANGE_LIMIT_EXCEEDED.getMessage());

        String sid = getSidByToken(unifiedToken);

        // 从数据库查询用户
        UserAuth userAuth = authMapper.selectOne(Wrappers
                .<UserAuth>lambdaQuery()
                .eq(UserAuth::getUserId, userId)
        );

        // MFA多因素身份验证
        // 第一道验证：统一身份认证验证身份
        if (!Objects.equals(userAuth.getSid(), sid))
            throw new UnauthorizedException(AuthMessage.UNIFIED_AUTH_FAILURE.getMessage());

        // 更新用户名
        authMapper.update(
                Wrappers.<UserAuth>lambdaUpdate()
                        .eq(UserAuth::getUserId, userId)
                        .set(UserAuth::getUsername, username)
        );

        redisIntegerTemplate.opsForValue().increment(usernameChangeKey);
        redisIntegerTemplate.expire(usernameChangeKey, 7, TimeUnit.DAYS);
    }

    @Override
    @Deprecated
    public void updatePassword(String unifiedToken, String userId, String password) throws UnauthorizedException {
        String sid = getSidByToken(unifiedToken);

        // 从数据库查询用户
        UserAuth userAuth = authMapper.selectOne(Wrappers
                .<UserAuth>lambdaQuery()
                .eq(UserAuth::getUserId, userId)
        );

        // MFA多因素身份验证
        // 第一道验证：统一身份认证验证身份
        if (!Objects.equals(userAuth.getSid(), sid))
            throw new UnauthorizedException(AuthMessage.UNIFIED_AUTH_FAILURE.getMessage());

        // 生成加密密码
        String pw_hash = EncryptionType.ARGON2.getTypeWithBraces() + Argon2Util.hash(password);

        // 更新密码
        authMapper.update(
                Wrappers.<UserAuth>lambdaUpdate()
                        .eq(UserAuth::getUserId, userId)
                        .set(UserAuth::getPassword, pw_hash)
        );
    }

    @Override
    @Deprecated
    public void updateEmail(String unifiedToken, String userId, String verificationCode, String email) throws UnauthorizedException {
        String sid = getSidByToken(unifiedToken);

        // 从数据库查询用户
        UserAuth userAuth = authMapper.selectOne(Wrappers
                .<UserAuth>lambdaQuery()
                .eq(UserAuth::getUserId, userId)
        );

        // MFA多因素身份验证
        // 第一道验证：统一身份认证验证身份
        if (!Objects.equals(userAuth.getSid(), sid))
            throw new UnauthorizedException(AuthMessage.UNIFIED_AUTH_FAILURE.getMessage());

        // 检查邮箱和验证码
        validateVerificationCode(email, verificationCode);

        // 更新邮箱
        authMapper.update(
                Wrappers.<UserAuth>lambdaUpdate()
                        .eq(UserAuth::getUserId, userId)
                        .set(UserAuth::getEmail, email)
        );
    }

    @Override
    public void sendCaptcha(HttpServletResponse response) throws Exception {
        // 设置响应头
        response.setContentType("image/png");
        // 生成唯一验证码 ID
        String captchaId = UUID.randomUUID().toString();
        response.setHeader("Captcha-Id", captchaId);

        //定义图形验证码的长、宽、验证码字符数、干扰线宽度
        AbstractCaptcha captcha;
        int choice = RandomUtil.randomInt(0, 5);
        int codeWidth = 150;
        int codeHeight = 50;
        int codeCount = 4;
        captcha = switch (choice) {
            case 0 -> CaptchaUtil.createShearCaptcha(codeWidth, codeHeight, codeCount, 5);
            case 1 -> CaptchaUtil.createCircleCaptcha(codeWidth, codeHeight, codeCount, 25);
            default -> CaptchaUtil.createLineCaptcha(codeWidth, codeHeight, codeCount, 100);
        };

//        ShearCaptcha captcha = CaptchaUtil.createShearCaptcha(200, 100, 4, 4);

        String captchaKey = "Authorization:captcha:" + captchaId;
        try {
            //图形验证码写出，可以写出到文件，也可以写出到流
            captcha.write(response.getOutputStream());

            // 存储验证码到 Redis，5 分钟过期
            redisStringTemplate.opsForValue().set(captchaKey, captcha.getCode(), 5, TimeUnit.MINUTES);
        } catch (IOException e) {
            throw new IOException("验证码生成或写出失败");
        }
    }

    @Override
    public AuthenticationVO authenticationByUnifiedAuth(String cookie) throws AuthorizationException, UnauthorizedException {
        // 获取学工号
        String sid = getSidByUnifiedAuth(cookie);

        String token = IdUtil.simpleUUID();

        redisStringTemplate.opsForValue().set("Authorization:authentication:unifiedAuth:" + token, sid, 15, TimeUnit.MINUTES);

        return AuthenticationVO
                .builder()
                .type(AuthenticationType.UNIFIED_AUTH.getTypeName())
                .token(token)
                .expiresIn(15 * 60L)
                .build();
    }

    @Override
    public String registerPreCheck(RegisterDTO registerDTO) {
        validateUsername(registerDTO.getUsername());
        validatePassword(registerDTO.getPassword());

        // 验证统一身份认证
        String sid = redisStringTemplate.opsForValue().get("Authorization:authentication:unifiedAuth:" + registerDTO.getUnifiedAuthToken());

        if (sid == null || sid.isEmpty()) {
            throw new AuthorizationException(AuthMessage.UNIFIED_AUTH_FAILURE.getMessage());
        }

        // 检查学号是否已注册
        String username = checkSidExists(sid);
        if (username != null) {
            HttpServletResponse response = WebUtil.getResponse();
            if (response != null) {
                response.setHeader("username", username);
            }
            throw new AuthorizationException(AuthMessage.SID_ALREADY_EXISTS.getMessage());
        }

        // 检查用户名是否已注册
        if (checkUsernameExists(registerDTO.getUsername())) {
            throw new AuthorizationException(AuthMessage.USERNAME_ALREADY_EXISTS.getMessage());
        }
        return sid;
    }

    @Override
    @GlobalTransactional(
            rollbackFor = Exception.class,
            name = "register-transaction",
            lockRetryInterval = 500,
            lockRetryTimes = 5
    )
    @Transactional(rollbackFor = Exception.class)
    public TokenVO register(RegisterDTO registerDTO, String sid) throws Exception {
        String userId = IdUtil.simpleUUID();

        // 绑定QQ
        String qqAuthorizationCode = registerDTO.getQqAuthorizationCode();
        QqAuth qqAuth = null;

        if (qqAuthorizationCode != null && !qqAuthorizationCode.isEmpty()) {
            qqAuth = bindQQ(qqAuthorizationCode, userId);
        }

        // 绑定Apple
        String appleAuthorizationCode = registerDTO.getAppleAuthorizationCode();
        AppleAuth appleAuth = null;
        if (appleAuthorizationCode != null && !appleAuthorizationCode.isEmpty()) {
            appleAuth = bindApple(appleAuthorizationCode, userId);
        }

        // 生成加密密码
        String pw_hash = EncryptionType.ARGON2.getTypeWithBraces() + Argon2Util.hash(registerDTO.getPassword());

        UserAuth userAuth = UserAuth
                .builder()
                .userId(userId)
                .sid(sid)
                .username(registerDTO.getUsername())
                .password(pw_hash)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        authMapper.insert(userAuth);

        userRoleMapper.insert(UserRole
                .builder()
                .userId(userAuth.getUserId())
                .roleId(RoleType.DEFAULT.getCode())
                .build()
        );

        UserInfo userInfo = UserInfo
                .builder()
                .userId(userAuth.getUserId())
                .gender(-1)
                .followersCount(0)
                .followingsCount(0)
                .likeReceivedCount(0)
                .level(0)
                .experience(0)
                .build();

        if (qqAuth != null && qqAuth.getQqNickname() != null && !qqAuth.getQqNickname().isEmpty()) {
            userInfo.setNickname(qqAuth.getQqNickname());
        } else if (appleAuth != null && appleAuth.getAppleName() != null && !appleAuth.getAppleName().isEmpty()) {
            userInfo.setNickname(appleAuth.getAppleName());
        } else {
            userInfo.setNickname("圈圈" + RandomUtil.randomString(6));
        }

        // 注册成功，进行登录，返回token
        authUtil.login(userAuth.getUserId());

        // MongoDB操作放最后，实现顺序性事务
        mongoTemplate.insert(userInfo);

        // 发送用户名修改消息
        sendUsernameChangeMessage(userId);

        return TokenVO
                .builder()
                .userId(userAuth.getUserId())
                .accessToken(StpUtil.getTokenValue())
                .expiresIn(StpUtil.getTokenTimeout())
                .refreshToken(refreshTokenUtil.generateRefreshToken())
                .build();
    }

    @Override
    public TokenVO loginByUnifiedAuth(String cookie) throws AuthorizationException, UnauthorizedException {
        // 使用统一身份认证Cookie获取学号
        String sid = getSidByUnifiedAuth(cookie);

        UserAuth userAuth = authMapper.selectOne(
                Wrappers.<UserAuth>lambdaQuery()
                        .eq(UserAuth::getSid, sid)
        );

        // 查看学号是否已注册
        if (userAuth == null) throw new AuthorizationException(AuthMessage.UNIFIED_AUTH_FAILURE.getMessage(), 404);

        authUtil.login(userAuth.getUserId());

        return TokenVO
                .builder()
                .userId(userAuth.getUserId())
                .accessToken(StpUtil.getTokenValue())
                .expiresIn(StpUtil.getTokenTimeout())
                .refreshToken(refreshTokenUtil.generateRefreshToken())
                .build();
    }

    @Override
    public TokenVO loginByQQ(String qqAuthorizationCode) throws AuthorizationException {
        // QQ登录
        String qqAccessToken;
        String qqOpenId = null;

        QqAccessTokenDTO qqAccessTokenDTO = getQqAccessToken(qqAuthorizationCode);
        qqAccessToken = qqAccessTokenDTO.getAccess_token();
        qqOpenId = qqAccessTokenDTO.getOpenid();
        if (qqOpenId == null || qqOpenId.isEmpty()) {
            qqOpenId = getQqOpenId(qqAccessToken);
        }
        // 检查QQ是否已绑定
        QqAuth qqAuth = qqAuthMapper.selectOne(Wrappers
                .<QqAuth>lambdaQuery()
                .eq(QqAuth::getQqOpenid, qqOpenId)
        );
        if (qqAuth == null) throw new AuthorizationException(AuthMessage.QQ_NOT_BOUND.getMessage());

        authUtil.login(qqAuth.getUserId());

        return TokenVO
                .builder()
                .userId(qqAuth.getUserId())
                .accessToken(StpUtil.getTokenValue())
                .expiresIn(StpUtil.getTokenTimeout())
                .refreshToken(refreshTokenUtil.generateRefreshToken())
                .build();
    }

    @Override
    public void changeEmail(String verificationCode, String email) {
        // 检查邮箱和验证码
        validateVerificationCode(email, verificationCode);

        // 更新邮箱
        authMapper.update(
                Wrappers.<UserAuth>lambdaUpdate()
                        .eq(UserAuth::getUserId, StpUtil.getLoginIdAsString())
                        .set(UserAuth::getEmail, email)
        );
    }

    @Override
    public void changeUsername(String username) throws BadRequestException {
        String userId = StpUtil.getLoginIdAsString();
        // 验证频率限制
        String usernameChangeKey = "Authorization:usernameChange:" + userId;
        Integer usernameChangeValue = redisIntegerTemplate.opsForValue().get(usernameChangeKey);

        if (usernameChangeValue != null && usernameChangeValue >= 1)
            throw new BadRequestException(AuthMessage.USERNAME_CHANGE_LIMIT_EXCEEDED.getMessage());

        // 更新用户名
        authMapper.update(
                Wrappers.<UserAuth>lambdaUpdate()
                        .eq(UserAuth::getUserId, userId)
                        .set(UserAuth::getUsername, username)
        );

        redisIntegerTemplate.opsForValue().increment(usernameChangeKey);
        redisIntegerTemplate.expire(usernameChangeKey, Duration.ofDays(7));

        sendUsernameChangeMessage(userId);
    }

    @Async
    public void sendUsernameChangeMessage(String userId) {
        SearchUserDTO searchUserDTO = SearchUserDTO
                .builder()
                .userId(userId)
                .isUserAuth(true)
                .build();
        rocketMQTemplate.convertAndSend("search:searchUser", searchUserDTO);
    }

    @Override
    public void changePassword(String oldPassword, String newPassword) throws Exception {
        String userId = StpUtil.getLoginIdAsString();

        checkPassword(userId, oldPassword);

        // 生成加密密码
        String pw_hash = EncryptionType.ARGON2.getTypeWithBraces() + Argon2Util.hash(newPassword);

        // 更新密码
        authMapper.update(
                Wrappers.<UserAuth>lambdaUpdate()
                        .eq(UserAuth::getUserId, userId)
                        .set(UserAuth::getPassword, pw_hash)
        );
    }

    @Override
    public void resetPassword(String cookie, String password) throws AuthorizationException, UnauthorizedException {
        // 使用统一身份认证Cookie获取学号
        String sid = getSidByUnifiedAuth(cookie);

        UserAuth userAuth = authMapper.selectOne(
                Wrappers.<UserAuth>lambdaQuery()
                        .eq(UserAuth::getSid, sid)
        );

        // 查看学号是否已注册
        if (userAuth == null) throw new AuthorizationException(AuthMessage.UNIFIED_AUTH_FAILURE.getMessage(), 404);

        // 生成加密密码
        String pw_hash = EncryptionType.ARGON2.getTypeWithBraces() + Argon2Util.hash(password);

        authMapper.update(
                Wrappers.<UserAuth>lambdaUpdate()
                        .eq(UserAuth::getSid, sid)
                        .set(UserAuth::getPassword, pw_hash)
        );
    }

    @Override
    @GlobalTransactional(
            rollbackFor = Exception.class,
            name = "delete-account-transaction",
            lockRetryInterval = 500,
            lockRetryTimes = 5
    )
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccount(String password) throws Exception {
        String userId = StpUtil.getLoginIdAsString();

        checkPassword(userId, password);

        authMapper.update(Wrappers
                        .<UserAuth>lambdaUpdate()
                        .eq(UserAuth::getUserId, userId)
//                .set(UserAuth::getSid, null)
                        .set(UserAuth::getUsername, "kxq_" + IdUtil.getSnowflakeNextIdStr())
                        .set(UserAuth::getEmail, null)
                        .set(UserAuth::getPassword, password)
                        .set(UserAuth::getEnabled, false)
        );
        userRoleMapper.delete(Wrappers
                .<UserRole>lambdaUpdate()
                .eq(UserRole::getUserId, userId)
        );
        sensitiveRecordMapper.delete(Wrappers
                .<SensitiveRecord>lambdaUpdate()
                .eq(SensitiveRecord::getUserId, userId)
        );
        qqAuthMapper.delete(Wrappers
                .<QqAuth>lambdaUpdate()
                .eq(QqAuth::getUserId, userId)
        );
        appleAuthMapper.delete(Wrappers
                .<AppleAuth>lambdaUpdate()
                .eq(AppleAuth::getUserId, userId)
        );
        // 更新用户信息的昵称头像等等
        Query query = new Query();
        query.addCriteria(Criteria.where("userId").is(userId));
        // 创建更新操作，设置字段为指定值
        Update update = new Update();
        update.set("nickname", "已注销");
        update.set("avatar", null);
        update.set("banner", null);
        update.set("bio", null);
        update.set("status", null);
        // 执行更新操作
        mongoTemplate.updateFirst(query, update, UserInfo.class);
    }

    @Override
    public PageQueryVO<SensitiveRecordVO> getSensitiveRecord(TypePageQueryDTO typePageQueryDTO) {
        Page<SensitiveRecord> recordPage = Page.of(typePageQueryDTO.getPage(), typePageQueryDTO.getSize());
        recordPage.addOrder(OrderItem.asc("record_time"));

        String type = typePageQueryDTO.getType();

        LambdaQueryWrapper<SensitiveRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SensitiveRecord::getUserId, StpUtil.getLoginIdAsString());
        if (type != null && !type.isEmpty()) {
            queryWrapper.eq(SensitiveRecord::getType, type);
        }
        recordPage = sensitiveRecordMapper.selectPage(recordPage, queryWrapper);

        return PageQueryVO
                .<SensitiveRecordVO>staticBuilder()
                .totalRecords(recordPage.getTotal())
                .totalPages(recordPage.getPages())
                .page(recordPage.getCurrent())
                .size(recordPage.getSize())
                .data(BeanUtil.copyToList(recordPage.getRecords(), SensitiveRecordVO.class))
                .build();

    }

    @Override
    public QqAuth bindQQ(String qqAuthorizationCode) throws AuthorizationException {
        if (qqAuthorizationCode == null || qqAuthorizationCode.isEmpty()) {
            return null;
        }

        QqAccessTokenDTO qqAccessTokenDTO = getQqAccessToken(qqAuthorizationCode);
        String qqAccessToken = qqAccessTokenDTO.getAccess_token();
        String qqOpenId = qqAccessTokenDTO.getOpenid();
        if (qqOpenId == null || qqOpenId.isEmpty()) {
            qqOpenId = getQqOpenId(qqAccessToken);
        }
        String qqNickname = getQqNickname(qqOpenId, qqAccessToken);

        // 检查QQ是否已绑定
        QqAuth qqAuth = qqAuthMapper.selectOne(Wrappers
                .<QqAuth>lambdaQuery()
                .eq(QqAuth::getQqOpenid, qqOpenId)
                .select(QqAuth::getUserId)
        );
        if (qqAuth != null) throw new AuthorizationException(AuthMessage.QQ_ALREADY_BOUND.getMessage());
        qqAuth = QqAuth
                .builder()
                .qqOpenid(qqOpenId)
                .qqNickname(qqNickname)
                .userId(StpUtil.getLoginIdAsString())
                .build();
        qqAuthMapper.insert(qqAuth);

        return qqAuth;
    }

    @Override
    public QqAuth bindQQ(String qqAuthorizationCode, String userId) throws AuthorizationException {
        if (qqAuthorizationCode == null || qqAuthorizationCode.isEmpty()) {
            return null;
        }

        QqAccessTokenDTO qqAccessTokenDTO = getQqAccessToken(qqAuthorizationCode);
        String qqAccessToken = qqAccessTokenDTO.getAccess_token();
        String qqOpenId = qqAccessTokenDTO.getOpenid();
        if (qqOpenId == null || qqOpenId.isEmpty()) {
            qqOpenId = getQqOpenId(qqAccessToken);
        }
        String qqNickname = getQqNickname(qqOpenId, qqAccessToken);

        // 检查QQ是否已绑定
        QqAuth qqAuth = qqAuthMapper.selectOne(Wrappers
                .<QqAuth>lambdaQuery()
                .eq(QqAuth::getQqOpenid, qqOpenId)
                .select(QqAuth::getUserId)
        );
        if (qqAuth != null) throw new AuthorizationException(AuthMessage.QQ_ALREADY_BOUND.getMessage());

        qqAuth = QqAuth
                .builder()
                .qqOpenid(qqOpenId)
                .qqNickname(qqNickname)
                .userId(userId)
                .build();
        qqAuthMapper.insert(qqAuth);

        return qqAuth;
    }

    @Override
    public void unBindQQ() {
        String userId = StpUtil.getLoginIdAsString();
        qqAuthMapper.delete(Wrappers
                .<QqAuth>lambdaUpdate()
                .eq(QqAuth::getUserId, userId)
        );
    }

    @Override
    public AppleAuth bindApple(String appleAuthorizationCode) throws Exception {
        String appleIdToken = appleAuthUtil.getAppleIdToken(appleAuthorizationCode);

        if (appleIdToken == null || appleIdToken.isEmpty()) {
            throw new AuthorizationException(AuthMessage.APPLE_ALREADY_BOUND.getMessage());
        }

        AppleAuth appleAuth = appleAuthUtil.getAppleInfo(appleIdToken);

        appleAuth.setUserId(StpUtil.getLoginIdAsString());
        appleAuth.setCreateTime(LocalDateTime.now());
        appleAuth.setUpdateTime(LocalDateTime.now());
        try {
            appleAuthMapper.insert(appleAuth);
        } catch (Exception e) {
            throw new AuthorizationException(AuthMessage.APPLE_ALREADY_BOUND.getMessage());
        }
        return appleAuth;
    }

    @Override
    public AppleAuth bindApple(String appleAuthorizationCode, String userId) throws Exception {
        String appleIdToken = appleAuthUtil.getAppleIdToken(appleAuthorizationCode);

        if (appleIdToken == null || appleIdToken.isEmpty()) {
            throw new AuthorizationException(AuthMessage.APPLE_ALREADY_BOUND.getMessage());
        }

        AppleAuth appleAuth = appleAuthUtil.getAppleInfo(appleIdToken);

        appleAuth.setUserId(userId);
        appleAuth.setCreateTime(LocalDateTime.now());
        appleAuth.setUpdateTime(LocalDateTime.now());
        try {
            appleAuthMapper.insert(appleAuth);
        } catch (Exception e) {
            throw new AuthorizationException(AuthMessage.APPLE_ALREADY_BOUND.getMessage());
        }
        return appleAuth;
    }

    @Override
    public void unBindApple() {
        appleAuthMapper.delete(Wrappers
                .<AppleAuth>lambdaUpdate()
                .eq(AppleAuth::getUserId, StpUtil.getLoginIdAsString())
        );
    }

    @Override
    public TokenVO loginByApple(String appleAuthorizationCode) throws Exception {
        String appleIdToken = appleAuthUtil.getAppleIdToken(appleAuthorizationCode);

        if (appleIdToken == null || appleIdToken.isEmpty()) {
            throw new AuthorizationException(AuthMessage.APPLE_ALREADY_BOUND.getMessage());
        }

        AppleAuth appleAuth = appleAuthUtil.getAppleInfo(appleIdToken);

        System.out.println(appleAuth);

        // 检查Apple是否已绑定
        appleAuth = appleAuthMapper.selectOne(Wrappers
                .<AppleAuth>lambdaQuery()
                .eq(AppleAuth::getAppleId, appleAuth.getAppleId())
        );
        if (appleAuth == null) throw new AuthorizationException(AuthMessage.APPLE_NOT_BOUND.getMessage());

        authUtil.login(appleAuth.getUserId());

        return TokenVO
                .builder()
                .userId(appleAuth.getUserId())
                .accessToken(StpUtil.getTokenValue())
                .expiresIn(StpUtil.getTokenTimeout())
                .refreshToken(refreshTokenUtil.generateRefreshToken())
                .build();
    }

    @Override
    public LinkedAccountVO getLinkedAccount() {
        String userId = StpUtil.getLoginIdAsString();
        UserAuth userAuth = authMapper.selectOne(Wrappers
                .<UserAuth>lambdaQuery()
                .eq(UserAuth::getUserId, userId)
                .select(UserAuth::getUserId, UserAuth::getEmail)
        );
        QqAuth qqAuth = qqAuthMapper.selectOne(Wrappers
                .<QqAuth>lambdaQuery()
                .eq(QqAuth::getUserId, userId)
                .select(QqAuth::getUserId, QqAuth::getQqNickname)
        );
        AppleAuth appleAuth = appleAuthMapper.selectOne(Wrappers
                .<AppleAuth>lambdaQuery()
                .eq(AppleAuth::getUserId, userId)
                .select(AppleAuth::getUserId, AppleAuth::getAppleName)
        );

        return LinkedAccountVO
                .builder()
                .userId(userId)
                .email(userAuth.getEmail())
                .qq(qqAuth != null)
                .apple(appleAuth != null)
                .build();
    }

    @Override
    public String getUsername() {
        UserAuth userAuth = authMapper.selectOne(Wrappers
                .<UserAuth>lambdaQuery()
                .eq(UserAuth::getUserId, StpUtil.getLoginIdAsString())
                .select(UserAuth::getUsername)
        );

        if (userAuth == null) {
            return "未知用户";
        }

        return userAuth.getUsername();
    }

    @Override
    public List<DeviceVO> getLoginDevices() {
        List<SaTerminalInfo> saTerminalInfos = StpUtil.getTerminalListByLoginId(cn.dev33.satoken.stp.StpUtil.getLoginId());

        List<DeviceVO> loginDevices = new ArrayList<>();
        for (SaTerminalInfo saTerminalInfo : saTerminalInfos) {
            loginDevices.add(DeviceVO.builder()
                    .deviceType(saTerminalInfo.getDeviceType())
                    .deviceName(String.valueOf(saTerminalInfo.getExtraData().get("deviceName")))
                    .region(String.valueOf(saTerminalInfo.getExtraData().get("region")))
                    .lastLoginTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(saTerminalInfo.getCreateTime()), ZoneId.systemDefault()))
                    .build());
        }

        return loginDevices;
    }

    // Cookie获取学工号
    String getSidByUnifiedAuth(String cookie) throws AuthorizationException, UnauthorizedException {
        // 使用统一身份认证Cookie获取学号
        JsonNode profile = portalClient.getProfile(cookie);
        String sid;
        try {
            sid = profile.at("$.results.entities[0].account").asText();
        } catch (Exception e) {
            throw new AuthorizationException(AuthMessage.UNIFIED_AUTH_FAILURE.getMessage());
        }

        if (sid == null || sid.isEmpty()) {
            throw new AuthorizationException(AuthMessage.UNIFIED_AUTH_FAILURE.getMessage());
        }

        return sid;
    }

    // 获Token取学工号
    @Deprecated
    String getSidByToken(String schoolToken) throws UnauthorizedException {
        // 请求学校服务器获取校园卡信息
        String schoolCard = schoolYktClient.getSchoolCard(schoolToken);
        // 解析JSON字符串
        JSONObject jsonObj = JSONUtil.parseObj(schoolCard);
        if (!jsonObj.get("success", Boolean.class)) {
            throw new UnauthorizedException(AuthMessage.UNIFIED_AUTH_FAILURE.getMessage());
        }

        // 获取 results 下的 entities 数组
        // 获取data -> card数组中的第一个对象 -> sno字段
        String sid = jsonObj.getByPath("data.card[0].sno", String.class);

        // 去掉"Bearer "前缀（如果存在）
        if (schoolToken.startsWith("Bearer ") || schoolToken.startsWith("bearer ")) {
            schoolToken = schoolToken.substring(7);
        }
        // JWT和学校双重验证
        JWT schoolJwt = JWT.of(schoolToken);
        if (sid.equals(schoolJwt.getPayload("sno").toString())) {
            return sid;
        } else {
            throw new UnauthorizedException(AuthMessage.UNIFIED_AUTH_FAILURE.getMessage());
        }
    }

    // 获取QQ Access Token
    QqAccessTokenDTO getQqAccessToken(String qqAuthorizationCode) throws AuthorizationException {
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("graph.qq.com")
                            .path("/oauth2.0/token")
                            .queryParam("grant_type", "authorization_code")
                            .queryParam("client_id", qqAppId)
                            .queryParam("client_secret", qqAppKey)
                            .queryParam("code", qqAuthorizationCode)
                            .queryParam("redirect_uri", qqRedirectUri)
                            .queryParam("fmt", "json")
                            .queryParam("need_openid", 1)
                            .build())
                    .retrieve()
                    .bodyToMono(QqAccessTokenDTO.class)
                    .block();
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new AuthorizationException(AuthMessage.QQ_AUTH_FAILURE.getMessage());
        }
    }

    // 获取QQ OpenId
    String getQqOpenId(String qqAccessToken) throws AuthorizationException {
        QqOpenIdDTO qqOpenIdDTO;
        try {
            qqOpenIdDTO = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("graph.qq.com")
                            .path("/oauth2.0/me")
                            .queryParam("access_token", qqAccessToken)
                            .queryParam("fmt", "json")
                            .build())
                    .retrieve()
                    .bodyToMono(QqOpenIdDTO.class)
                    .block();
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new AuthorizationException(AuthMessage.QQ_AUTH_FAILURE.getMessage());
        }
        if (qqOpenIdDTO != null) {
            return qqOpenIdDTO.getOpenid();
        }
        return null;
    }

    // 获取QQ昵称
    String getQqNickname(String qqOpenId, String qqAccessToken) {
        JsonNode jsonNode = webClient.get()
                .uri("https://graph.qq.com/user/get_user_info")
                .attribute("access_token", qqAccessToken)
                .attribute("oauth_consumer_key", qqAppId)
                .attribute("openid", qqOpenId)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        if (jsonNode != null) {
            try {
                return jsonNode.get("nickname").asText();
            } catch (ConvertException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 验证密码是否合法并且强度足够
     *
     * @param password 密码
     * @throws IllegalArgumentException 如果密码非法或不符合规则
     */
    public void validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }

        if (password.length() < 8 || password.length() > 20) {
            throw new IllegalArgumentException("密码长度必须在 8 到 20 个字符之间");
        }

        // 检查是否包含非法字符
        if (password.contains(" ")) {
            throw new IllegalArgumentException("密码不能包含空格");
        }
    }

    /**
     * 验证用户名是否合法
     *
     * @param username 用户名
     * @throws IllegalArgumentException 如果用户名非法或不符合规则
     */
    public void validateUsername(String username) {
        // 正则表达式：6-16位，英文、数字和下划线组成
        String regex = "^[a-zA-Z0-9_]{6,16}$";
        // 使用Pattern匹配
        if (!Pattern.matches(regex, username)) {
            throw new IllegalArgumentException("用户名不合法");
        }
    }

    public String checkSidExists(String sid) {
        UserAuth userAuth = authMapper.selectOne(Wrappers
                .<UserAuth>lambdaQuery()
                .eq(UserAuth::getSid, sid)
                .select(UserAuth::getUsername)
        );

        // 检查学号是否已注册
        if (userAuth != null) {
            return userAuth.getUsername();
        }

        return null;
    }

    public boolean checkUsernameExists(String username) {
        UserAuth userAuth = authMapper.selectOne(Wrappers
                .<UserAuth>lambdaQuery()
                .eq(UserAuth::getUsername, username)
                .select(UserAuth::getUsername)
        );

        // 检查用户名是否已注册
        return !(userAuth == null);
    }

    public void validateVerificationCode(String email, String verificationCode) {
        // 检查邮箱合法性
        if (!Validator.isEmail(email)) {
            throw new IllegalArgumentException(AuthMessage.INVALID_EMAIL_FORMAT.getMessage());
        }

        // 校验验证码
        String verificationCodeKey = "Authorization:verification-code:" + email;
        String verificationCodeValue = redisStringTemplate.opsForValue().get(verificationCodeKey);

        if (!Objects.equals(verificationCodeValue, verificationCode))
            throw new IllegalArgumentException(AuthMessage.VERIFICATION_CODE_INCORRECT.getMessage());
        redisStringTemplate.delete(verificationCodeKey);
    }

    // 检查密码是否正确（修改时注意用户名密码登录）
    public void checkPassword(String userId, String password) throws Exception {
        // 从数据库查询用ID和密码
        UserAuth userAuth = authMapper.selectOne(Wrappers
                .<UserAuth>lambdaQuery()
                .eq(UserAuth::getUserId, userId)
        );

        // 检查用户ID是否存在
        if (Objects.isNull(userAuth)) throw new RuntimeException("未知错误");

        // 获取储存的加密密码
        String storedHash = userAuth.getPassword();

        // 检查密码是否正确
        if (!verifyPassword(storedHash, password)) {
            throw new UnauthorizedException(AuthMessage.PASSWORD_INCORRECT.getMessage());
        }
    }

    public boolean verifyPassword(String hash, String password) throws Exception {
        // 选择加密算法（新老算法迭代使用）
        boolean isValidPassword;
        if (hash.startsWith(EncryptionType.ARGON2.getTypeWithBraces())) {
            String pw_hash = hash.substring(EncryptionType.ARGON2.getTypeWithBraces().length());
            isValidPassword = Argon2Util.verify(pw_hash, password);
        } else if (hash.startsWith(EncryptionType.BCRYPT.getTypeWithBraces())) {
            String pw_hash = hash.substring(EncryptionType.BCRYPT.getTypeWithBraces().length());
            isValidPassword = BCrypt.checkpw(password, pw_hash);
        } else {
            throw new Exception("密码解析失败");
        }
        return isValidPassword;
    }
}
