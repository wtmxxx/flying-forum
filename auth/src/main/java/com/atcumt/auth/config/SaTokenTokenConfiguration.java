package com.atcumt.auth.config;

import cn.dev33.satoken.strategy.SaStrategy;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;

@Configuration
public class SaTokenTokenConfiguration {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_LENGTH = 256;
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    /**
     * 重写 Sa-Token 框架内部算法策略
     */
    @PostConstruct
    public void rewriteSaStrategy() {
        // 重写 Token 生成策略
        SaStrategy.instance.createToken = (loginId, loginType) -> {
            StringBuilder accessToken = new StringBuilder(TOKEN_LENGTH);

            for (int i = 0; i < TOKEN_LENGTH; i++) {
                int index = RANDOM.nextInt(CHARACTERS.length());
                accessToken.append(CHARACTERS.charAt(index));
            }
            return accessToken.toString();
        };
    }
}
