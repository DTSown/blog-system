package com.son.blog.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    USER_NOT_FOUND(1001, "User not found", HttpStatus.NOT_FOUND),
    POST_NOT_FOUND(1002, "Post not found", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1003, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1004, "You do not have permission", HttpStatus.FORBIDDEN),
    USERNAME_EXISTS(1005, "Username already exists", HttpStatus.BAD_REQUEST),
    EMAIL_EXISTS(1006, "Email already exists", HttpStatus.BAD_REQUEST),
    INVALID_CREDENTIALS(1007, "Invalid credentials", HttpStatus.BAD_REQUEST),
    SLUG_EXISTS(1008, "Slug already exists", HttpStatus.CONFLICT),
    FILE_UPLOAD_FAILED(1009, "Failed to upload file", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_NOT_FOUND(1010, "File not found", HttpStatus.NOT_FOUND),
    VALIDATION_FAILED(1011, "Validation failed", HttpStatus.BAD_REQUEST),
    TOO_MANY_REQUESTS(1012, "Too many login attempts, please try again later", HttpStatus.TOO_MANY_REQUESTS),
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized exception", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
