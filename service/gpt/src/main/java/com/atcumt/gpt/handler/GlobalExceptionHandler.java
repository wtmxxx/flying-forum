package com.atcumt.gpt.handler;

import com.atcumt.model.common.Result;
import com.atcumt.model.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 处理所有异常并返回统一的Result格式
    @ExceptionHandler(Exception.class)
    public Result handleAllExceptions(Exception ex) throws Exception {
        log.error(ex.getMessage());
        // 返回统一的Result对象，包含500错误信息
        return Result.failure(ResultCode.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    // 处理自定义异常
//    @ExceptionHandler(CustomException.class)
//    public Result<String> handleCustomException(CustomException ex) {
//        // 返回统一的Result对象，使用自定义错误码和消息
//        return Result.failure(ResultCode.BAD_REQUEST, ex.getMessage());
//    }

    // 处理资源未找到的异常
//    @ExceptionHandler(ResourceNotFoundException.class)
//    public Result<String> handleResourceNotFoundException(ResourceNotFoundException ex) {
//        // 返回统一的Result对象，404 Not Found
//        return Result.failure(ResultCode.NOT_FOUND, ex.getMessage());
//    }
}
