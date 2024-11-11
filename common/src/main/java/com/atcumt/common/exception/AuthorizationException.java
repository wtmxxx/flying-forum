package com.atcumt.common.exception;

import com.atcumt.model.common.ResultCode;
import lombok.Getter;

@Getter
public class AuthorizationException extends CommonException {
    private final int code;

    public AuthorizationException(String message) {
        super(message, ResultCode.FAILURE.getCode());
        this.code = ResultCode.FAILURE.getCode();
    }

    public AuthorizationException(String message, int code) {
        super(message, code);
        this.code = code;
    }

    public AuthorizationException(String message, Throwable cause, int code) {
        super(message, cause, code);
        this.code = code;
    }

    public AuthorizationException(Throwable cause, int code) {
        super(cause, code);
        this.code = code;
    }
}
