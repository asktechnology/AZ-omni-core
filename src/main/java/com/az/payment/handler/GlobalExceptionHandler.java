package com.az.payment.handler;


import com.az.payment.config.logging.ErrorLogger;
import com.az.payment.config.logging.SummaryLogger;
import com.az.payment.constants.Code;
import com.az.payment.exception.BusinessException;
import com.az.payment.exception.ValidationException;
import com.az.payment.response.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @Autowired
    private ErrorLogger errorLogger;

    @Autowired
    private SummaryLogger summaryLogger;


    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse> handleCustomerNotFoundException(BusinessException ex, HttpServletRequest request) {
        // Log error
        String errorRef = errorLogger.logError(ex, request.getMethod(), request.getRequestURI());
        summaryLogger.logError(errorRef, request.getMethod(), request.getRequestURI(), HttpStatus.NOT_FOUND.value());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(Code.NOTFOUND, ex.getMessage() + " [Ref: " + errorRef + "]"));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse> handleEntityNotFoundException(EntityNotFoundException ex, HttpServletRequest request) {
        // Log error
        String errorRef = errorLogger.logError(new Exception(ex), request.getMethod(), request.getRequestURI());
        summaryLogger.logError(errorRef, request.getMethod(), request.getRequestURI(), HttpStatus.NOT_FOUND.value());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(Code.NOTFOUND, ex.getMessage() + " [Ref: " + errorRef + "]"));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse> handleValidationException(ValidationException ex, HttpServletRequest request) {
        // Log error
        String errorRef = errorLogger.logError(ex, request.getMethod(), request.getRequestURI());
        summaryLogger.logError(errorRef, request.getMethod(), request.getRequestURI(), HttpStatus.BAD_REQUEST.value());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(Code.INVALID, ex.getMessage() + " [Ref: " + errorRef + "]"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        // Log error
        String errorRef = errorLogger.logError(new Exception(ex), request.getMethod(), request.getRequestURI());
        summaryLogger.logError(errorRef, request.getMethod(), request.getRequestURI(), HttpStatus.BAD_REQUEST.value());

        var errors = new HashMap<String, String>();
        ex.getBindingResult().getAllErrors()
                .forEach(error -> {
                    var fieldName = ((FieldError) error).getField();
                    var errorMessage = error.getDefaultMessage();
                    errors.put(fieldName, errorMessage);
                });
        errors.put("errorRef", errorRef);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(Code.BAD_REQUEST, errors));
    }
}
