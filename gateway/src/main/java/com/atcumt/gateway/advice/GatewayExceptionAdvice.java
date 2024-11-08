package com.atcumt.gateway.advice;

import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
@RestControllerAdvice
@Order(Integer.MIN_VALUE)  // 优先级较高，确保捕获到异常
public class GatewayExceptionAdvice implements WebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        // 打印日志
        log.error("捕获到异常 -> ", ex);

        // 构造 JSON 响应体
        JSONObject errorResponse = JSONUtil
                .createObj(new JSONConfig().setIgnoreNullValue(false))
                .set("code", 404)
                .set("msg", "请求路径无效")
                .set("data", null);

        // 将 JSON 转换为字节数组
        byte[] responseBytes;
        responseBytes = errorResponse.toString().getBytes(StandardCharsets.UTF_8);

        // 设置响应头
        exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // 将字节数组写入响应
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                .bufferFactory().wrap(responseBytes)));
    }
}
