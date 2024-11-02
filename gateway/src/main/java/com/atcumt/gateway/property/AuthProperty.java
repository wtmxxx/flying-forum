package com.atcumt.gateway.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "cumt-forum.auth")
public class AuthProperty {
    private List<String> includePaths;
    private List<String> excludePaths;
}

