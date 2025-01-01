package com.atcumt.model.common.enums;


import lombok.Getter;

@Getter
public enum ResultCode {
    SUCCESS(200, "成功"),
    FAILURE(400, "失败"),
    BAD_REQUEST(400, "错误请求"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "未找到"),
    INTERNAL_SERVER_ERROR(500, "服务器内部异常"),
    ;

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}