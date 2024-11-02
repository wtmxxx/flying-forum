package com.atcumt.common.utils;

import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTValidator;
import cn.hutool.jwt.signers.JWTSigner;
import cn.hutool.jwt.signers.JWTSignerUtil;
import com.atcumt.common.exception.UnauthorizedException;
import com.atcumt.common.property.JwtProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.encrypt.KeyStoreKeyFactory;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.time.Duration;
import java.util.Date;
import java.util.Map;

@Component
@EnableConfigurationProperties(JwtProperty.class)
public class JwtTool {
    private final JwtProperty jwtProperty;

    private final JWTSigner jwtSigner;

    public JwtTool(JwtProperty jwtProperty) {
        this.jwtProperty = jwtProperty;

        // 获取秘钥工厂
        KeyStoreKeyFactory keyStoreKeyFactory =
                new KeyStoreKeyFactory(
                        jwtProperty.getLocation(),
                        jwtProperty.getPassword().toCharArray());
        //读取钥匙对
        KeyPair keyPair = keyStoreKeyFactory.getKeyPair(
                jwtProperty.getAlias(),
                jwtProperty.getPassword().toCharArray());

        this.jwtSigner = JWTSignerUtil.createSigner("rs256", keyPair);
    }

    public String createToken(String userId) {
        return this.createToken(userId, jwtProperty.getTtl());
    }

    public String createToken(String userId, Duration ttl) {
        return JWT.create()
                .addPayloads(Map.of("user_id", userId))
                .setExpiresAt(new Date(System.currentTimeMillis() + ttl.toMillis()))
                .setIssuer("atcumt")
                .sign(jwtSigner);
    }

    public String parseToken(String token) {
        // 1.校验token是否为空
        if (token == null) {
            throw new UnauthorizedException("用户未登录");
        }
        // 2.校验并解析jwt
        JWT jwt;
        try {
            jwt = JWT.of(token).setSigner(jwtSigner);
        } catch (Exception e) {
            throw new UnauthorizedException("无效的token", e);
        }
        // 2.校验jwt是否有效
        if (!jwt.verify()) {
            // 验证失败
            throw new UnauthorizedException("无效的token");
        }
        // 3.校验是否过期
        try {
            JWTValidator.of(jwt).validateDate();
        } catch (ValidateException e) {
            throw new UnauthorizedException("token已经过期");
        }
        // 4.数据格式校验
        Object userPayload = jwt.getPayload("user_id");
        if (userPayload == null) {
            // 数据为空
            throw new UnauthorizedException("无效的token");
        }

        // 5.数据解析
        try {
            return userPayload.toString();
        } catch (RuntimeException e) {
            // 数据格式有误
            throw new UnauthorizedException("无效的token");
        }
    }
}
