package com.atcumt.common.config;

import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.same.SaSameUtil;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 权限认证 配置类
 */
@Configuration
@ConditionalOnClass({DispatcherServlet.class, StpUtil.class})
@ConditionalOnMissingClass({"com.atcumt.gateway.GatewayApplication"})
public class SaTokenConfiguration implements WebMvcConfigurer {

    @Value("${spring.profiles.active:prod}")
    private String active;

    // 注册 Sa-Token 全局过滤器
    @Bean
    public SaServletFilter getSaServletFilter() {
        List<String> excludeList = new ArrayList<>(List.of(
                "/favicon.ico",
                "/webjars/**"
        ));
        // 如果是开发环境，排除 Swagger 和 Actuator 相关的路径
        if (active.equals("dev")) {
            excludeList.addAll(List.of(
                    "/swagger-ui*/**",
                    "/doc.html",
                    "/v3/api-docs/**",
                    "/actuator/**"
            ));
        }

        return new SaServletFilter()
                .addInclude("/**")
                .setExcludeList(excludeList)
                .setAuth(_ -> {
                    // 校验 Same-Token 身份凭证
                    SaSameUtil.checkCurrentRequestToken();
                })
                .setError(e -> SaResult.error(e.getMessage()));
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器，打开注解式鉴权功能
        registry.addInterceptor(new SaInterceptor()).addPathPatterns("/**");
    }
}
