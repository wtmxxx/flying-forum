package com.atcumt.gateway.filter;

import cn.dev33.satoken.same.SaSameUtil;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.text.AntPathMatcher;
import com.atcumt.common.exception.UnauthorizedException;
import com.atcumt.gateway.property.AuthProperty;
import com.atcumt.model.common.enums.ResultCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


@Component
@RefreshScope
@EnableConfigurationProperties(AuthProperty.class)
public class AuthGlobalFilter implements GlobalFilter, Ordered {
    private final AuthProperty authProperty;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();
    private final RedisTemplate<String, String> saRedisTemplate;
    private static final Cache<String, Object> localTokenCache = Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .maximumSize(1000)
            .build();
    private final ObjectMapper objectMapper;

    @Autowired
    AuthGlobalFilter(AuthProperty authProperty,
                     @Qualifier("saRedisTemplate") RedisTemplate<String, String> saRedisTemplate,
                     ObjectMapper objectMapper) {
        this.authProperty = authProperty;
        this.saRedisTemplate = saRedisTemplate;
        this.objectMapper = objectMapper;
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
        String tokenName;
        String tokenValue;
        try {
            tokenName = StpUtil.getTokenName();
            tokenValue = request.getHeaders().getFirst("Authorization");

            String tokenValueWithoutPrefix = Objects
                    .requireNonNull(tokenValue, ResultCode.UNAUTHORIZED.getMessage())
                    .substring(StpUtil.getStpLogic().getConfigOrGlobal().getTokenPrefix().length() + " ".length());

            // 从本地缓存Caffeine中获取登录ID，如果不存在则从Redis中获取
            Object loginId = localTokenCache.getIfPresent(tokenValueWithoutPrefix);
            if (loginId == null) {
                loginId = StpUtil.getLoginIdByToken(tokenValueWithoutPrefix);
            }
            if (loginId == null || !StpUtil.isLogin(loginId)) {
                // 新老Token切换
                String oldTokenKey = "Authorization:login:old-access-token:" + tokenValueWithoutPrefix;
                userId = saRedisTemplate.opsForValue().get(oldTokenKey);

                if (userId == null) {
                    throw new UnauthorizedException(ResultCode.UNAUTHORIZED.getMessage());
                }
            } else {
                localTokenCache.put(tokenValueWithoutPrefix, loginId);
                userId = String.valueOf(loginId);
            }
        } catch (Exception e) {
            // 如果无效，拦截
            ServerHttpResponse response = exchange.getResponse();

            // 构造 JSON 响应体 (未授权)
            JsonNode errorResponse = objectMapper.createObjectNode()
                    .put("code", ResultCode.UNAUTHORIZED.getCode())
                    .put("msg", ResultCode.UNAUTHORIZED.getMessage())
                    .putNull("data");

            // 将 JSON 转换为字节数组
            byte[] responseBytes;
            responseBytes = errorResponse.toString().getBytes(StandardCharsets.UTF_8);

            // 设置响应头
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

            // 将字节数组写入响应
            return response.writeWith(Mono.just(response
                    .bufferFactory().wrap(responseBytes)));
        }

        // 5.如果有效，传递用户信息和网关鉴定SameToken
        ServerWebExchange webExchange = exchange.mutate()
                .request(r -> r
                        .header("User-ID", userId)
                        .header(tokenName, tokenValue)
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
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
