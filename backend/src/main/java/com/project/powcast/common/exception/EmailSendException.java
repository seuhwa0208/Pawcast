package com.project.powcast.common.exception;

import lombok.Getter;

@Getter
public class EmailSendException extends RuntimeException {
    private final ErrorCode errorCode;
    public EmailSendException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
