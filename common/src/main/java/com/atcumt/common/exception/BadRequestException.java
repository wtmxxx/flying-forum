package com.atcumt.common.exception;

import com.atcumt.model.common.ResultCode;

public class BadRequestException extends CommonException {

    public BadRequestException(String message) {
        super(message, ResultCode.BAD_REQUEST.getCode());
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause, ResultCode.BAD_REQUEST.getCode());
    }

    public BadRequestException(Throwable cause) {
        super(cause, ResultCode.BAD_REQUEST.getCode());
    }
}
