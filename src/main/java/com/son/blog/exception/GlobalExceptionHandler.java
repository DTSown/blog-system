package com.son.blog.exception;

import com.son.blog.dto.ApiResponse;
import com.son.blog.enums.ErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Object>> handleAppException(AppException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        ApiResponse<Object> response = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }
    
    // Fallback for remaining old exceptions just in case
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        ApiResponse<Object> response = ApiResponse.builder()
                .code(ErrorCode.POST_NOT_FOUND.getCode())
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(ErrorCode.POST_NOT_FOUND.getHttpStatus()).body(response);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadRequestException(BadRequestException ex) {
        ApiResponse<Object> response = ApiResponse.builder()
                .code(ErrorCode.VALIDATION_FAILED.getCode())
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.getHttpStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errors = new ArrayList<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            Map<String, String> fieldError = new HashMap<>();
            fieldError.put("field", ((FieldError) error).getField());
            fieldError.put("message", error.getDefaultMessage());
            errors.add(fieldError);
        });

        ApiResponse<List<Map<String, String>>> response = ApiResponse.<List<Map<String, String>>>builder()
                .code(ErrorCode.VALIDATION_FAILED.getCode())
                .message("Validation failed")
                .data(errors)
                .build();
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.getHttpStatus()).body(response);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentialsException(BadCredentialsException ex) {
        ApiResponse<Object> response = ApiResponse.builder()
                .code(ErrorCode.INVALID_CREDENTIALS.getCode())
                .message("Invalid username or password")
                .build();
        return ResponseEntity.status(ErrorCode.INVALID_CREDENTIALS.getHttpStatus()).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(AccessDeniedException ex) {
        ApiResponse<Object> response = ApiResponse.builder()
                .code(ErrorCode.UNAUTHORIZED.getCode())
                .message("You don't have permission to access this resource")
                .build();
        return ResponseEntity.status(ErrorCode.UNAUTHORIZED.getHttpStatus()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGlobalException(Exception ex) {
        ApiResponse<Object> response = ApiResponse.builder()
                .code(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode())
                .message("An unexpected error occurred: " + ex.getMessage())
                .build();
        return ResponseEntity.status(ErrorCode.UNCATEGORIZED_EXCEPTION.getHttpStatus()).body(response);
    }
}
