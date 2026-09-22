package com.son.blog.service.impl;

import com.son.blog.dto.AuthResponse;
import com.son.blog.dto.LoginRequest;
import com.son.blog.dto.RegisterRequest;
import com.son.blog.entity.User;
import com.son.blog.enums.ErrorCode;
import com.son.blog.exception.AppException;
import com.son.blog.repository.UserRepository;
import com.son.blog.security.CustomUserDetails;
import com.son.blog.security.JwtUtil;
import com.son.blog.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final HttpServletRequest request;
    private final org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USERNAME_EXISTS);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_EXISTS);
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .role("ROLE_USER")
                .build();

        userRepository.save(user);
        
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String token = jwtUtil.generateToken(userDetails);
        
        return AuthResponse.builder()
                .accessToken(token)
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String rateLimiterKey = "login_attempts:" + request.getEmail();
        
        Integer attempts = (Integer) redisTemplate.opsForValue().get(rateLimiterKey);
        if (attempts != null && attempts >= 5) {
            throw new AppException(ErrorCode.TOO_MANY_REQUESTS);
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    if (attempts == null) {
                        redisTemplate.opsForValue().set(rateLimiterKey, 1, 5, java.util.concurrent.TimeUnit.MINUTES);
                    } else {
                        redisTemplate.opsForValue().increment(rateLimiterKey);
                    }
                    return new AppException(ErrorCode.INVALID_CREDENTIALS);
                });

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), request.getPassword())
            );
            redisTemplate.delete(rateLimiterKey);
        } catch (org.springframework.security.core.AuthenticationException e) {
            if (attempts == null) {
                redisTemplate.opsForValue().set(rateLimiterKey, 1, 5, java.util.concurrent.TimeUnit.MINUTES);
            } else {
                redisTemplate.opsForValue().increment(rateLimiterKey);
            }
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String token = jwtUtil.generateToken(userDetails);
        
        return AuthResponse.builder()
                .accessToken(token)
                .username(userDetails.getUsername())
                .email(userDetails.getUser().getEmail())
                .role(userDetails.getUser().getRole())
                .build();
    }

    @Override
    public void logout() {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String jti = jwtUtil.extractJti(token);
            if (jti != null) {
                long expirationMillis = jwtUtil.extractExpiration(token).getTime();
                long ttl = expirationMillis - System.currentTimeMillis();
                if (ttl > 0) {
                    String blacklistKey = "token_blacklist:" + jti;
                    redisTemplate.opsForValue().set(blacklistKey, "logout", ttl, java.util.concurrent.TimeUnit.MILLISECONDS);
                }
            }
        }
        SecurityContextHolder.clearContext();
    }
}
