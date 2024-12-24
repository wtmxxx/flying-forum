package com.atcumt.auth.utils;

import cn.hutool.json.JSONObject;
import cn.hutool.jwt.JWTUtil;
import com.atcumt.common.exception.AuthorizationException;
import com.atcumt.model.auth.entity.AppleAuth;
import com.atcumt.model.common.enums.AuthMessage;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class AppleAuthUtil {
    private final WebClient webClient;
    @Value("${apple.team-id}")
    private String appleTeamId;
    @Value("${apple.client-id}")
    private String appleClientId;
    @Value("${apple.key-id}")
    private String appleKeyId;
    @Value("${apple.private-key-path}")
    private String privateKeyPath;

    // 获取Apple的id_token
    public String getAppleIdToken(String appleAuthorizationCode) throws Exception {
        // 设置请求参数
        String clientSecret = getAppleClientSecret();
        // 发送 POST 请求并处理响应
        JSONObject response = null;
        try {
            response = webClient.post()
                    .uri(uriBuilder -> uriBuilder.scheme("https")
                            .host("appleid.apple.com")
                            .path("/auth/oauth2/v2/token")
                            .queryParam("client_id", appleClientId)
                            .queryParam("client_secret", clientSecret)
                            .queryParam("code", appleAuthorizationCode)
                            .queryParam("grant_type", "authorization_code")
                            .build())
                    .retrieve()
                    .bodyToMono(JSONObject.class)
                    .block();
        } catch (Exception e) {
            throw new AuthorizationException(AuthMessage.APPLE_AUTH_FAILURE.getMessage());
        }

        if (response == null) {
            throw new AuthorizationException(AuthMessage.APPLE_AUTH_FAILURE.getMessage());
        }

        return response.get("id_token", String.class);
    }

    // 获取Apple用户信息
    public AppleAuth getAppleInfo(String appleIdToken) throws AuthorizationException {
        try {
            // 解析JWT token
            JSONObject jwt = JWTUtil.parseToken(appleIdToken).getPayload().getClaimsJson();

            // 提取用户信息
            String appleId = jwt.get("sub", String.class);  // Apple ID（sub）
            String email = jwt.get("email", String.class);  // 邮箱
            String name = jwt.get("name", String.class);  // 姓名

            // 将用户信息封装到VO对象中
            AppleAuth appleAuth = new AppleAuth();
            appleAuth.setAppleId(appleId);
            appleAuth.setAppleEmail(email);
            appleAuth.setAppleName(name);

            return appleAuth;

        } catch (Exception e) {
            throw new AuthorizationException(AuthMessage.APPLE_AUTH_FAILURE.getMessage());
        }
    }

    public String getAppleClientSecret() throws Exception {
        try {
            // 读取私钥文件内容
            StringBuilder keyBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    AppleAuthUtil.class.getClassLoader().getResourceAsStream(privateKeyPath),
                    StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.contains("-----")) {
                        keyBuilder.append(line);
                    }
                }
            }
            String privateKeyContent = keyBuilder.toString();

//            System.out.println("Private Key Content: " + privateKeyContent);

            byte[] keyBytes = Base64.getDecoder().decode(privateKeyContent);

            // 生成 EC 私钥
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            ECPrivateKey privateKey = (ECPrivateKey) keyFactory.generatePrivate(spec);

            // 配置签名算法
            Algorithm algorithm = Algorithm.ECDSA256(null, privateKey);

            // 生成 JWT
            long currentTime = System.currentTimeMillis();
            String token = JWT.create()
                    .withKeyId(appleKeyId) // 设置 kid
                    .withIssuer(appleTeamId) // 设置 iss
                    .withSubject(appleClientId) // 设置 sub
                    .withAudience("https://appleid.apple.com") // 设置 aud
                    .withIssuedAt(new Date(currentTime)) // 设置 iat
                    .withExpiresAt(new Date(currentTime + 86400L * 180 * 1000)) // 设置 exp
                    .sign(algorithm); // 签名

            return token;
        } catch (Exception e) {
            throw new AuthorizationException(AuthMessage.APPLE_AUTH_FAILURE.getMessage());
        }
    }
}
