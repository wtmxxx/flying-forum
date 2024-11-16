package com.atcumt.gateway.filter;

import cn.dev33.satoken.same.SaSameUtil;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.text.AntPathMatcher;
import com.atcumt.common.exception.UnauthorizedException;
import com.atcumt.gateway.property.AuthProperty;
import com.atcumt.model.common.ResultCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


@Component
@RefreshScope
@EnableConfigurationProperties(AuthProperty.class)
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final AuthProperty authProperty;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();
    @Autowired
    AuthGlobalFilter(AuthProperty authProperty) {
        this.authProperty = authProperty;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1.获取Request
        ServerHttpRequest request = exchange.getRequest();
        // 2.判断是否不需要拦截
        if (isExclude(request.getPath().toString())) {
            // 传递网关鉴定SameToken
            ServerWebExchange webExchange = exchange.mutate()
                    .request(r -> r
                            .header(SaSameUtil.SAME_TOKEN, SaSameUtil.getToken())
                    ).build();
            // 放行
            return chain.filter(webExchange);
        }
        // 3.sa-token自动获取请求头中的Token
        // 4.校验并解析Token
        String userId;
        try {
            userId = StpUtil.getLoginIdAsString();
        } catch (UnauthorizedException e) {
            // 如果无效，拦截
            ServerHttpResponse response = exchange.getResponse();
            response.setRawStatusCode(ResultCode.UNAUTHORIZED.getCode());
            return response.setComplete();
        }
        // 5.如果有效，传递用户信息和网关鉴定SameToken
        ServerWebExchange webExchange = exchange.mutate()
                .request(r -> r
                        .header("X-User-ID", userId)
                        .header(SaSameUtil.SAME_TOKEN, SaSameUtil.getToken())
                ).build();
        // 6.放行
        return chain.filter(webExchange);
    }

    private boolean isExclude(String antPath) {
        // 首先检查include
        for (String pathPattern : authProperty.getIncludePaths()) {
            if (antPathMatcher.match(pathPattern, antPath)) {
                return false;
            }
        }
        // 其次检查exclude
        for (String pathPattern : authProperty.getExcludePaths()) {
            if (antPathMatcher.match(pathPattern, antPath)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getOrder() {
        // 最高次序
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
