package com.atcumt.common.exception;

import com.atcumt.model.common.ResultCode;

public class ForbiddenException extends CommonException {

    public ForbiddenException(String message) {
        super(message, ResultCode.FORBIDDEN.getCode());
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause, ResultCode.FORBIDDEN.getCode());
    }

    public ForbiddenException(Throwable cause) {
        super(cause, ResultCode.FORBIDDEN.getCode());
    }
}
