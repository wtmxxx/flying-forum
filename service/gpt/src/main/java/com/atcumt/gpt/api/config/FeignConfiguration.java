package com.atcumt.gpt.api.config;

import com.atcumt.common.utils.UserContext;
import com.atcumt.gpt.api.client.fallback.UserClientFallback;
import feign.Logger;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfiguration {
    @Bean
    public Logger.Level feignLogLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    public RequestInterceptor AuthRequestInterceptor() {
        return template -> {
            // 获取登录用户
            String userId = UserContext.getUser();
            if (userId == null) {
                // 如果为空则直接跳过
                return;
            }
            // 如果不为空则放入请求头中，传递给下游微服务
            template.header("user-id", userId);
        };
    }

    @Bean
    UserClientFallback userClientFallback() {
        return new UserClientFallback();
    }
}
