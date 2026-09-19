package com.son.blog.service;

import com.son.blog.dto.AuthResponse;
import com.son.blog.dto.LoginRequest;
import com.son.blog.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    void logout();
}
