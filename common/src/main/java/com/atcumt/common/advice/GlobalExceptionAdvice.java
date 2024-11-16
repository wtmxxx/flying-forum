package com.atcumt.common.advice;

import cn.hutool.core.exceptions.ExceptionUtil;
import com.atcumt.common.exception.*;
import com.atcumt.common.utils.WebUtil;
import com.atcumt.model.common.AuthMessage;
import com.atcumt.model.common.Result;
import com.atcumt.model.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class GlobalExceptionAdvice {

    @ExceptionHandler(DbException.class)
    public Object handleDbException(DbException e) {
        log.error("mysql数据库操作异常 -> ", e);
        return processResponse(e);
    }

    @ExceptionHandler(CommonException.class)
    public Object handleAuthorizationException(CommonException e) {
        log.error("自定义异常 -> {} , 异常原因：{}  ", e.getClass().getName(), e.getMessage());
        log.debug("", e);
        return processResponse(e);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getAllErrors()
                .stream().map(ObjectError::getDefaultMessage)
                .collect(Collectors.joining("|"));
        log.error("请求参数校验异常 -> {}", msg);
        log.debug("", e);
        return processResponse(new BadRequestException(msg));
    }

    @ExceptionHandler(BindException.class)
    public Object handleBindException(BindException e) {
        log.error("请求参数绑定异常 -> BindException， {}", e.getMessage());
        log.debug("", e);
        return processResponse(new BadRequestException("请求参数格式错误"));
    }

    @ExceptionHandler(AuthorizationException.class)
    public Object handleAuthorizationException(AuthorizationException e) {
        log.error("鉴权异常 -> {} , 异常原因：{}  ", e.getClass().getName(), e.getMessage());
        log.debug("", e);
        return processResponse(e);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public Object handleUnauthorizedException(UnauthorizedException e) {
        log.error("未授权异常 -> {} , 异常原因：{}  ", e.getClass().getName(), e.getMessage());
        log.debug("", e);
        return processResponse(new UnauthorizedException(AuthMessage.UNIFIED_AUTH_FAILURE.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public Object handleRuntimeException(RuntimeException e) throws Throwable {
        log.error("运行时异常 uri : {} -> ", Objects.requireNonNull(WebUtil.getRequest()).getRequestURI(), e);

        Throwable finalCause = ExceptionUtil.getRootCause(e);
        return processResponse(new CommonException(finalCause.getLocalizedMessage(), ResultCode.FAILURE.getCode()));
    }

    @ExceptionHandler(Exception.class)
    public Object handleException(Exception e) {
        log.error("其他异常 uri : {} -> ", Objects.requireNonNull(WebUtil.getRequest()).getRequestURI(), e);
        return processResponse(new CommonException("服务器内部异常", 500));
    }

    private ResponseEntity<Result<Void>> processResponse(CommonException e) {
        return ResponseEntity.status(e.getCode()).body(Result.failure(e.getCode(), e.getMessage()));
    }

}
