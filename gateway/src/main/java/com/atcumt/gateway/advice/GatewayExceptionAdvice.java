package com.atcumt.gateway.advice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)  // 优先级较高，确保捕获到异常
public class GatewayExceptionAdvice implements WebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    @NonNull
    public Mono<Void> handle(ServerWebExchange exchange, @NonNull Throwable ex) {
        // 打印日志
        log.error("捕获到异常 -> ", ex);

        // 构造 JSON 响应体
        JsonNode errorResponse = objectMapper.createObjectNode()
                .put("code", HttpStatus.NOT_FOUND.value())
                .put("msg", "无效请求")
                .putNull("data");

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
