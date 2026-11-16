package com.project.powcast.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 로직 실패 오류
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, String>> handleBusinessException(
            BusinessException e) {

        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(Map.of("message", e.getMessage()));
    }
}
