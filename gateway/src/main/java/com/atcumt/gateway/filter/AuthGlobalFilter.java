package com.atcumt.gateway.filter;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.text.AntPathMatcher;
import com.atcumt.common.exception.UnauthorizedException;
import com.atcumt.common.utils.JwtTool;
import com.atcumt.gateway.property.AuthProperty;
import com.atcumt.model.common.ResultCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@EnableConfigurationProperties(AuthProperty.class)
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final JwtTool jwtTool;
    private final AuthProperty authProperty;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Autowired
    AuthGlobalFilter(JwtTool jwtTool, AuthProperty authProperty) {
        this.jwtTool = jwtTool;
        this.authProperty = authProperty;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1.获取Request
        ServerHttpRequest request = exchange.getRequest();
        // 2.判断是否不需要拦截
        if (isExclude(request.getPath().toString())) {
            // 无需拦截，直接放行
            return chain.filter(exchange);
        }
        // 3.获取请求头中的token
        String token = null;
        List<String> headers = request.getHeaders().get("Authorization");
        if (!CollectionUtil.isEmpty(headers)) {
            token = headers.getFirst();
        }
        // 4.校验并解析token
        String userId;
        try {
            userId = jwtTool.parseToken(token);
        } catch (UnauthorizedException e) {
            // 如果无效，拦截
            ServerHttpResponse response = exchange.getResponse();
            response.setRawStatusCode(ResultCode.UNAUTHORIZED.getCode());
            return response.setComplete();
        }
        // 5.如果有效，传递用户信息
        System.out.println(userId);
        ServerWebExchange webExchange = exchange.mutate()
                .request(r -> r.header("user-id", userId))
                .build();
        // 6.放行
        return chain.filter(webExchange);
    }

    private boolean isExclude(String antPath) {
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
