package com.atcumt.auth.utils;

import cn.dev33.satoken.stp.StpUtil;
import com.atcumt.common.exception.UnauthorizedException;
import com.atcumt.model.auth.enums.AuthMessage;
import com.atcumt.model.auth.vo.TokenVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Component
public class RefreshTokenUtil {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_LENGTH = 256; // Token长度
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"; // 随机用序列码

    private final RedisTemplate<String, String> saRedisTemplate;

    @Autowired
    public RefreshTokenUtil(@Qualifier("saRedisTemplate") RedisTemplate<String, String> saRedisTemplate) {
        this.saRedisTemplate = saRedisTemplate;
    }

    public String generateRefreshToken() {
        StringBuilder refreshToken = new StringBuilder(TOKEN_LENGTH);

        for (int i = 0; i < TOKEN_LENGTH; i++) {
            int index = RANDOM.nextInt(CHARACTERS.length());
            refreshToken.append(CHARACTERS.charAt(index));
        }

        // 生成Redis存储的Key
        String refreshKey = "Authorization:login:refresh-token:"
                + StpUtil.getLoginIdAsString()
                + ":" + StpUtil.getLoginDevice();

        // 设置Token有效期为当前时间的三个月后
        saRedisTemplate.opsForValue().set(refreshKey, refreshToken.toString(), 180, TimeUnit.DAYS);

        return refreshToken.toString();
    }

    public TokenVO getAccessToken(String refreshToken) throws UnauthorizedException {
        String userId = StpUtil.getLoginIdAsString();
        String loginDevice = StpUtil.getLoginDevice();

        String oldToken = StpUtil.getTokenValue();
        String oldTokenKey = "Authorization:login:old-access-token:" + oldToken;
        saRedisTemplate.opsForValue().set(oldTokenKey, userId, 1, TimeUnit.MINUTES);

        String refreshKey = "Authorization:login:refresh-token:" + userId + ":" + loginDevice;
        if (!refreshToken.equals(saRedisTemplate.opsForValue().get(refreshKey))) {
            throw new UnauthorizedException(AuthMessage.REFRESH_TOKEN_NOT_EXISTS.getMessage());
        }

        StpUtil.logout();
        StpUtil.login(userId, loginDevice);
        return TokenVO
                .builder()
                .accessToken(StpUtil.getTokenValue())
                .expiresIn(StpUtil.getTokenTimeout())
                .refreshToken(generateRefreshToken())
                .userId(userId)
                .build();
    }

    public void deleteRefreshToken() {
        String userId = StpUtil.getLoginIdAsString();
        String loginDevice = StpUtil.getLoginDevice();

        String refreshKey = "Authorization:login:refresh-token:" + userId + ":" + loginDevice;

        saRedisTemplate.delete(refreshKey);
    }

}
