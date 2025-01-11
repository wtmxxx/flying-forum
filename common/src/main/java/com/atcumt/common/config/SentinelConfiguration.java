package com.atcumt.common.config;

import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.csp.sentinel.adapter.spring.webflux.SentinelWebFluxFilter;
import com.alibaba.csp.sentinel.adapter.spring.webmvc.SentinelWebInterceptor;
import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

@Configuration
@Slf4j
public class SentinelConfiguration {
    // 普通服务的限流异常处理（WebMVC）
    @Bean
    @ConditionalOnBean(SentinelWebInterceptor.class)
    public BlockExceptionHandler commonBlockExceptionHandler() {
        return getBlockExceptionHandler();
    }

    // 网关的限流异常处理（WebFlux）
    @Bean
    @ConditionalOnBean(SentinelWebFluxFilter.class)
    public BlockExceptionHandler gatewayBlockExceptionHandler() {
        return getBlockExceptionHandler();
    }

    private BlockExceptionHandler getBlockExceptionHandler() {
        return new BlockExceptionHandler() {
            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response, BlockException e) throws Exception {
                // 打印日志
                log.warn("限流异常 -> SentinelBlockException { Blocked by Sentinel (flow limiting) }");

                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json;charset=UTF-8");

                // 构造 JSON 响应体
                JSONObject errorResponse = JSONUtil
                        .createObj(new JSONConfig().setIgnoreNullValue(false))
                        .set("code", HttpStatus.TOO_MANY_REQUESTS.value())
                        .set("msg", "请求过于频繁，请稍后再试")
                        .set("data", null);

                response.getWriter().write(errorResponse.toString());
            }
        };
    }
}
