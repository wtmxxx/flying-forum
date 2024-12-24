package com.atcumt.common.exception;

import com.atcumt.model.common.enums.ResultCode;

public class DbException extends CommonException {

    public DbException(String message) {
        super(message, ResultCode.INTERNAL_SERVER_ERROR.getCode());
    }

    public DbException(String message, Throwable cause) {
        super(message, cause, ResultCode.INTERNAL_SERVER_ERROR.getCode());
    }

    public DbException(Throwable cause) {
        super(cause, ResultCode.INTERNAL_SERVER_ERROR.getCode());
    }
}
