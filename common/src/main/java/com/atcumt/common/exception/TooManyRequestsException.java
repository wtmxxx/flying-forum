package com.atcumt.common.exception;

import com.atcumt.model.common.enums.ResultCode;

public class TooManyRequestsException extends CommonException {

    public TooManyRequestsException(String message) {
        super(message, ResultCode.BAD_REQUEST.getCode());
    }

    public TooManyRequestsException(String message, Throwable cause) {
        super(message, cause, ResultCode.BAD_REQUEST.getCode());
    }

    public TooManyRequestsException(Throwable cause) {
        super(cause, ResultCode.BAD_REQUEST.getCode());
    }
}
