package com.son.blog.service.impl;

import com.son.blog.dto.AuthResponse;
import com.son.blog.dto.LoginRequest;
import com.son.blog.dto.RegisterRequest;
import com.son.blog.entity.User;
import com.son.blog.exception.BadRequestException;
import com.son.blog.repository.UserRepository;
import com.son.blog.security.CustomUserDetails;
import com.son.blog.security.JwtUtil;
import com.son.blog.service.AuthService;
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

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already in use");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .role(request.getRole() != null ? request.getRole() : "ROLE_USER")
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
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

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
        SecurityContextHolder.clearContext();
    }
}
