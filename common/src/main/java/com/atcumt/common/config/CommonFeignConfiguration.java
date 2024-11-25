package com.atcumt.common.config;

import cn.dev33.satoken.same.SaSameUtil;
import cn.dev33.satoken.stp.StpUtil;
import com.atcumt.common.utils.UserContext;
import feign.Feign;
import feign.Logger;
import feign.Request;
import feign.RequestInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConditionalOnBean(Feign.class)
public class CommonFeignConfiguration {
    @Bean
    public Logger.Level feignLogLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    public RequestInterceptor authRequestInterceptor() {
        return template -> {
            // 获取登录用户
            String userId = UserContext.getUserId();
            if (userId == null) {
                // 如果为空则直接跳过
                return;
            }
            String tokenName = StpUtil.getTokenName();
            String tokenValue = StpUtil.getTokenValue();
            // 如果不为空则放入请求头中，传递给下游微服务
            template
                    .header("X-User-ID", userId)
                    .header(tokenName, tokenValue)
                    .header(SaSameUtil.SAME_TOKEN, SaSameUtil.getToken());
        };
    }

    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(Duration.ofMillis(5000), Duration.ofMillis(30000), true);
    }
}
