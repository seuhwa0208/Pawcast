package com.project.powcast.product.controller;
import com.project.powcast.common.exception.ErrorCode;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.util.Map;
@Order(0)
@RestControllerAdvice(basePackageClasses=ProductController.class)
public class ProductExceptionHandler {
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String,String>> invalidRequest(Exception e) {
        var code=ErrorCode.INVALID_PRODUCT_REQUEST;
        return ResponseEntity.status(code.getStatus()).body(Map.of("message",code.getMessage()));
    }
}
