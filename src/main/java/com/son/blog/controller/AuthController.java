package com.son.blog.controller;

import com.son.blog.dto.ApiResponse;
import com.son.blog.dto.AuthResponse;
import com.son.blog.dto.LoginRequest;
import com.son.blog.dto.RegisterRequest;
import com.son.blog.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return new ResponseEntity<>(ApiResponse.success(authService.register(request)), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout() {
        authService.logout();
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }
}
