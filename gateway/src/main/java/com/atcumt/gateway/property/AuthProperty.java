package com.atcumt.gateway.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "auth")
@RefreshScope
public class AuthProperty {
    private List<String> includePaths;
    private List<String> excludePaths;
}