package com.atcumt.auth.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.captcha.AbstractCaptcha;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.jwt.JWT;
import com.atcumt.auth.api.client.SchoolClient;
import com.atcumt.auth.mapper.AuthMapper;
import com.atcumt.auth.mapper.UserRoleMapper;
import com.atcumt.auth.service.AuthService;
import com.atcumt.auth.utils.EmailUtil;
import com.atcumt.auth.utils.RefreshTokenUtil;
import com.atcumt.common.exception.AuthorizationException;
import com.atcumt.common.exception.BadRequestException;
import com.atcumt.common.exception.TooManyRequestsException;
import com.atcumt.common.exception.UnauthorizedException;
import com.atcumt.model.auth.entity.UserAuth;
import com.atcumt.model.auth.entity.UserRole;
import com.atcumt.model.auth.enums.EncryptionType;
import com.atcumt.model.auth.vo.TokenVO;
import com.atcumt.model.common.AuthMessage;
import com.atcumt.model.common.DeviceType;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.seata.spring.annotation.GlobalTransactional;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl extends ServiceImpl<AuthMapper, UserAuth> implements AuthService {
    private final AuthMapper authMapper;
    private final SchoolClient schoolClient;
    private final RefreshTokenUtil refreshTokenUtil;
    private final UserRoleMapper userRoleMapper;
    private final EmailUtil emailUtil;
    private final RedisTemplate<String, Integer> redisIntegerTemplate;
    private final RedisTemplate<String, String> redisStringTemplate;

    @Override
    @GlobalTransactional
    public TokenVO registerBySchool(String schoolToken) {
        // 使用token获取学号
        String studentId = getStudentIdByToken(schoolToken);

        UserAuth userAuth = authMapper.selectOne(
                Wrappers.<UserAuth>lambdaQuery()
                        .eq(UserAuth::getStudentId, studentId)
        );

        // 查看学号是否已注册
        if (userAuth != null)
            throw new AuthorizationException(AuthMessage.STUDENT_ID_ALREADY_REGISTERED.getMessage(), 409);
        else userAuth = UserAuth
                .builder()
                .studentId(studentId)
                .lastLoginTime(LocalDateTime.now())
                .build();

        authMapper.insert(userAuth);

        userRoleMapper.insert(UserRole
                .builder()
                .userId(userAuth.getUserId())
                .roleId("user")
                .build()
        );

        // 注册成功，进行登录，返回token
        StpUtil.login(userAuth.getUserId(), DeviceType.getDeviceType());

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
    public TokenVO loginBySchool(String schoolToken) throws Exception {
        // 使用token获取学号
        String studentId = getStudentIdByToken(schoolToken);

        UserAuth userAuth = authMapper.selectOne(
                Wrappers.<UserAuth>lambdaQuery()
                        .eq(UserAuth::getStudentId, studentId)
        );

        authMapper.update(
                Wrappers.<UserAuth>lambdaUpdate()
                        .eq(UserAuth::getStudentId, studentId)
                        .set(UserAuth::getLastLoginTime, LocalDateTime.now())
        );

        // 查看学号是否已注册
        if (userAuth == null) throw new AuthorizationException(AuthMessage.UNIFIED_AUTH_FAILURE.getMessage(), 404);

        StpUtil.login(userAuth.getUserId(), DeviceType.getDeviceType());

        return TokenVO
                .builder()
                .userId(userAuth.getUserId())
                .accessToken(StpUtil.getTokenValue())
                .expiresIn(StpUtil.getTokenTimeout())
                .refreshToken(refreshTokenUtil.generateRefreshToken())
                .build();
    }

    @Override
    public TokenVO refreshToken(String refreshToken) {
        return refreshTokenUtil.getAccessToken(refreshToken);
    }

    @Override
    public void logout() {
        refreshTokenUtil.deleteRefreshToken();
        StpUtil.logout();
    }

    @Override
    public void bindUsername(String userId, String username, String password) {
        // 验证密码合法性
        validatePassword(password);

        // 生成加密密码
        String pw_hash = "{bcrypt}" + BCrypt.hashpw(password, BCrypt.gensalt());
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

        // 获取储存的加密密码
        String storedHash = userAuth.getPassword();

        // 检查加密算法（新老算法迭代使用）
        if (storedHash.startsWith(EncryptionType.BCRYPT.getTypeWithBraces())) {
            String pw_hash = storedHash.substring(EncryptionType.BCRYPT.getTypeWithBraces().length());
            if (BCrypt.checkpw(password, pw_hash)) {
                // 密码正确，登录
                StpUtil.login(userAuth.getUserId(), DeviceType.getDeviceType());
                // 更新登录时间
                authMapper.update(
                        Wrappers.<UserAuth>lambdaUpdate()
                                .eq(UserAuth::getUsername, username)
                                .set(UserAuth::getLastLoginTime, LocalDateTime.now())
                );
                // 返回Token
                return TokenVO
                        .builder()
                        .userId(userAuth.getUserId())
                        .accessToken(StpUtil.getTokenValue())
                        .expiresIn(StpUtil.getTokenTimeout())
                        .refreshToken(refreshTokenUtil.generateRefreshToken())
                        .build();
            } else {
                throw new UnauthorizedException(AuthMessage.PASSWORD_INCORRECT.getMessage());
            }
        } else {
            throw new Exception("密码解析失败");
        }
    }

    @Override
    public void SendVerifyCode(String email, String captchaId, String captchaCode) throws Exception {
        // 检查图形验证码
        String captchaKey = "Authorization:captcha:" + captchaId;
        String realCaptchaCode = redisStringTemplate.opsForValue().get(captchaKey);

        redisStringTemplate.delete(captchaKey);

        if (Objects.nonNull(realCaptchaCode) && !Objects.equals(captchaCode.toLowerCase(), realCaptchaCode.toLowerCase())) {
            throw new IllegalArgumentException(AuthMessage.CAPTCHA_CODE_INCORRECT.getMessage());
        }

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
                        "矿大论坛服务团队";

        try {
            emailUtil.sendEmail(email, "矿大论坛验证码", emailContent, false);
        } catch (MessagingException e) {
            throw new Exception(AuthMessage.VERIFICATION_CODE_SEND_FAILURE.getMessage());
        }
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
    public TokenVO loginByEmailVerificationCode(String email, String verificationCode) {
        // 检查邮箱和验证码
        validateVerificationCode(email, verificationCode);

        UserAuth userAuth = authMapper.selectOne(Wrappers
                .<UserAuth>lambdaQuery()
                .eq(UserAuth::getEmail, email)
        );

        if (Objects.isNull(userAuth)) throw new IllegalArgumentException(AuthMessage.EMAIL_NOT_EXISTS.getMessage());

        StpUtil.login(userAuth.getUserId(), DeviceType.getDeviceType());
        return TokenVO
                .builder()
                .userId(userAuth.getUserId())
                .accessToken(StpUtil.getTokenValue())
                .expiresIn(StpUtil.getTokenTimeout())
                .refreshToken(refreshTokenUtil.generateRefreshToken())
                .build();
    }

    @Override
    public void updateUsername(String unifiedToken, String userId, String username) {
        // 验证频率限制
        String usernameChangeKey = "Authorization:username-change:" + userId;
        Integer usernameChangeValue = redisIntegerTemplate.opsForValue().get(usernameChangeKey);

        if (usernameChangeValue != null && usernameChangeValue >= 1)
            throw new BadRequestException(AuthMessage.USERNAME_CHANGE_LIMIT_EXCEEDED.getMessage());

        String studentId = getStudentIdByToken(unifiedToken);

        // 从数据库查询用户
        UserAuth userAuth = authMapper.selectOne(Wrappers
                .<UserAuth>lambdaQuery()
                .eq(UserAuth::getUserId, userId)
        );

        // MFA多因素身份验证
        // 第一道验证：统一身份认证验证身份
        if (!Objects.equals(userAuth.getStudentId(), studentId))
            throw new UnauthorizedException(AuthMessage.UNIFIED_AUTH_FAILURE.getMessage());

        // 更新用户名
        authMapper.update(
                Wrappers.<UserAuth>lambdaUpdate()
                        .eq(UserAuth::getUserId, userId)
                        .set(UserAuth::getUsername, username)
        );

        redisIntegerTemplate.opsForValue().increment(usernameChangeKey);
    }

    @Override
    public void updatePassword(String unifiedToken, String userId, String password) {
        String studentId = getStudentIdByToken(unifiedToken);

        // 从数据库查询用户
        UserAuth userAuth = authMapper.selectOne(Wrappers
                .<UserAuth>lambdaQuery()
                .eq(UserAuth::getUserId, userId)
        );

        // MFA多因素身份验证
        // 第一道验证：统一身份认证验证身份
        if (!Objects.equals(userAuth.getStudentId(), studentId))
            throw new UnauthorizedException(AuthMessage.UNIFIED_AUTH_FAILURE.getMessage());

        // 更新密码
        authMapper.update(
                Wrappers.<UserAuth>lambdaUpdate()
                        .eq(UserAuth::getUserId, userId)
                        .set(UserAuth::getPassword, password)
        );
    }

    @Override
    public void updateEmail(String unifiedToken, String userId, String verificationCode, String email) {
        String studentId = getStudentIdByToken(unifiedToken);

        // 从数据库查询用户
        UserAuth userAuth = authMapper.selectOne(Wrappers
                .<UserAuth>lambdaQuery()
                .eq(UserAuth::getUserId, userId)
        );

        // MFA多因素身份验证
        // 第一道验证：统一身份认证验证身份
        if (!Objects.equals(userAuth.getStudentId(), studentId))
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
        response.setHeader("X-Captcha-Id", captchaId);

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


    String getStudentIdByToken(String schoolToken) {
        // 请求学校服务器获取校园卡信息
        String schoolCard = schoolClient.getSchoolCard(schoolToken);
        // 解析JSON字符串
        JSONObject jsonObj = JSONUtil.parseObj(schoolCard);
        if (!jsonObj.get("success", Boolean.class)) {
            throw new UnauthorizedException(AuthMessage.UNIFIED_AUTH_FAILURE.getMessage());
        }

        // 获取 results 下的 entities 数组
        // 获取data -> card数组中的第一个对象 -> sno字段
        String studentId = jsonObj.getByPath("data.card[0].sno", String.class);

        // 去掉"Bearer "前缀（如果存在）
        if (schoolToken.startsWith("Bearer ") || schoolToken.startsWith("bearer ")) {
            schoolToken = schoolToken.substring(7);
        }
        // JWT和学校双重验证
        JWT schoolJwt = JWT.of(schoolToken);
        if (studentId.equals(schoolJwt.getPayload("sno").toString())) {
            return studentId;
        } else {
            throw new UnauthorizedException(AuthMessage.UNIFIED_AUTH_FAILURE.getMessage());
        }
    }

    /**
     * 验证密码是否合法并且强度足够
     *
     * @param password 用户输入的密码
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

        // 密码合法
        System.out.println("密码验证通过！");
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
}
