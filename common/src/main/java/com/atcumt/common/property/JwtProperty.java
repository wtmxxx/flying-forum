package com.atcumt.common.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperty {
    private Resource location;
    private String password;
    private String alias;
    private Duration ttl = Duration.ofDays(7);
    private String type = "JKS";
}
