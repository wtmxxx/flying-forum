package com.atcumt.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Configuration
public class JwtConfiguration {

    private final JwtKeyProvider jwtKeyProvider;

    public JwtConfiguration(JwtKeyProvider jwtKeyProvider) {
        this.jwtKeyProvider = jwtKeyProvider;
    }

    // 配置 JwtEncoder，用于生成签名的 JWT
//    @Bean
//    public JwtEncoder jwtEncoder() {
//        // 使用 NimbusJwtEncoder，通过公钥和私钥创建 Encoder
//        return NimbusJwtEncoder.privateKey(jwtKeyProvider.getPrivateKey()).build();
//    }

    // 配置 JwtDecoder，用于解析和验证 JWT
    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withPublicKey((RSAPublicKey) jwtKeyProvider.getPublicKey()).build();
    }

    // 自定义 JWT 内容，包括 user_id 和 role 信息
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer() {
        return context -> {
            context.getJwsHeader().algorithm(SignatureAlgorithm.RS256); // 使用 RS256 算法
            context.getClaims().claim("user_id", context.getPrincipal().getName()); // 添加 user_id
            context.getClaims().claim("role", "USER"); // 添加角色信息
            context.getClaims().expiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
            context.getClaims().issuedAt(Instant.now()).build();
        };
    }
}
