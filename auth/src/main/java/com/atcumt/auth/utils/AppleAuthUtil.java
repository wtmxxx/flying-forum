package com.atcumt.auth.utils;

import cn.hutool.json.JSONObject;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.signers.JWTSigner;
import cn.hutool.jwt.signers.JWTSignerUtil;
import com.atcumt.common.exception.AuthorizationException;
import com.atcumt.model.auth.entity.AppleAuth;
import com.atcumt.model.common.AuthMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AppleAuthUtil {

    private static final String APPLE_PUBLIC_KEY_URL = "https://appleid.apple.com/auth/oauth2/v2/keys";
    private final WebClient webClient;
    @Value("${apple.client-id}")
    private String appleClientId;
    @Value("${apple.client-secret}")
    private String appleClientSecret;
    @Value("${apple.redirect-uri}")
    private String appleRedirectUri;

    // 获取Apple的id_token
    public String getAppleIdToken(String appleAuthorizationCode) {
        // 设置请求参数
        Map<String, String> requestParams = Map.of(
                "client_id", appleClientId,
                "client_secret", appleClientSecret,
                "code", appleAuthorizationCode,
                "grant_type", "authorization_code",
                "redirect_uri", appleRedirectUri
        );

        // 发送 POST 请求并处理响应
        JSONObject response = webClient.post()
                .uri("https://appleid.apple.com/auth/oauth2/v2/token")
                .bodyValue(requestParams)
                .retrieve()
                .bodyToMono(JSONObject.class)
                .block();

        if (response == null) {
            throw new AuthorizationException(AuthMessage.APPLE_AUTH_FAILURE.getMessage());
        }

        return response.get("id_token", String.class);
    }

    // 获取Apple用户信息
    public AppleAuth getAppleInfo(String appleIdToken) {
        try {
            // 解析JWT token
            JWT jwt = JWT.of(appleIdToken);

            // 获取JWT中的kid（key ID）
            String kid = jwt.getHeader().getClaim("kid").toString();

            // 获取Apple的公钥列表
            List<Map<String, Object>> publicKeys = getApplePublicKeys();

            // 根据kid查找对应的公钥
            RSAPublicKey rsaPublicKey = findPublicKeyByKid(publicKeys, kid);
            if (rsaPublicKey == null) {
                throw new AuthorizationException("Apple public key not found for kid: " + kid);
            }

            // 使用公钥验证JWT签名
            JWTSigner jwtSigner = JWTSignerUtil.es256(rsaPublicKey);
            if (!jwt.verify(jwtSigner)) {
                throw new AuthorizationException("JWT signature verification failed");
            }

            // 提取用户信息
            String appleId = jwt.getPayload("sub").toString();  // Apple ID（sub）
            String email = jwt.getPayload("email").toString();  // 邮箱
            String name = jwt.getPayload("name").toString();  // 姓名

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

    // 获取Apple的公钥列表
    private List<Map<String, Object>> getApplePublicKeys() {
        try {
            // 使用WebClient请求Apple的公钥
            JSONObject response = webClient.get()
                    .uri(APPLE_PUBLIC_KEY_URL)
                    .retrieve()
                    .bodyToMono(JSONObject.class)
                    .block();

            return (List<Map<String, Object>>) response.get("keys");
        } catch (Exception e) {
            throw new AuthorizationException("Failed to retrieve Apple public keys");
        }
    }

    // 根据kid查找对应的公钥
    private RSAPublicKey findPublicKeyByKid(List<Map<String, Object>> publicKeys, String kid) throws Exception {
        Optional<Map<String, Object>> keyOpt = publicKeys.stream()
                .filter(key -> key.get("kid").equals(kid))
                .findFirst();

        if (keyOpt.isPresent()) {
            Map<String, Object> key = keyOpt.get();
            String modulus = (String) key.get("n");
            String exponent = (String) key.get("e");

            // 解析成RSA公钥
            return (RSAPublicKey) generateRSAPublicKey(modulus, exponent);
        }

        return null;
    }

    // 生成RSA公钥
    private PublicKey generateRSAPublicKey(String modulus, String exponent) throws Exception {
        // 将 Base64 编码的字符串解码为字节数组
        byte[] modulusBytes = Base64.getUrlDecoder().decode(modulus);
        byte[] exponentBytes = Base64.getUrlDecoder().decode(exponent);

        // 创建 RSAPublicKeySpec
        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(
                new java.math.BigInteger(1, modulusBytes), // 模数
                new java.math.BigInteger(1, exponentBytes) // 指数
        );

        // 使用 KeyFactory 生成 RSA 公钥
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(keySpec);

        return publicKey;
    }
}
