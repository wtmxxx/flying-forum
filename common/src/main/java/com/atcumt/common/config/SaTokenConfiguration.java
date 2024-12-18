package com.atcumt.common.config;

import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.same.SaSameUtil;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 权限认证 配置类
 */
@Configuration
@ConditionalOnClass({DispatcherServlet.class, StpUtil.class})
@ConditionalOnMissingClass({"com.atcumt.gateway.GatewayApplication"})
public class SaTokenConfiguration implements WebMvcConfigurer {
    // 注册 Sa-Token 全局过滤器
    @Bean
    public SaServletFilter getSaServletFilter() {
        return new SaServletFilter()
                .addInclude("/**")
                .addExclude("/favicon.ico",
                        "/webjars/**",
                        "/swagger-ui*/**",
                        "/doc.html",
                        "/v3/api-docs/**"
                )
                .setAuth(obj -> {
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
