package com.atcumt.auth.config;

import com.atcumt.common.property.JwtProperty;
import lombok.Getter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;

@Getter
@Component
@EnableConfigurationProperties(JwtProperty.class)
public class JwtKeyProvider {

    private final JwtProperty jwtProperty;
    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public JwtKeyProvider(JwtProperty jwtProperty) {
        this.jwtProperty = jwtProperty;

        // 通过 try-with-resources 确保 InputStream 自动关闭
        try (InputStream keyStoreStream = jwtProperty.getLocation().getInputStream()) {
            char[] password = jwtProperty.getPassword().toCharArray();

            KeyStore keyStore = KeyStore.getInstance(jwtProperty.getType());
            keyStore.load(keyStoreStream, password);

            this.privateKey = (PrivateKey) keyStore.getKey(jwtProperty.getAlias(), password);
            this.publicKey = keyStore.getCertificate(jwtProperty.getAlias()).getPublicKey();
        } catch (Exception e) {
            throw new IllegalStateException("无法加载密钥库或获取密钥对", e);
        }
    }
}
