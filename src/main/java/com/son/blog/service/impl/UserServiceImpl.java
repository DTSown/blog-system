package com.son.blog.service.impl;

import com.son.blog.dto.UserRequest;
import com.son.blog.dto.UserResponse;
import com.son.blog.entity.User;
import com.son.blog.exception.BadRequestException;
import com.son.blog.exception.ResourceNotFoundException;
import com.son.blog.repository.UserRepository;
import com.son.blog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.stream.Collectors;
import com.son.blog.dto.PageResponse;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    private boolean isCurrentUserSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null) {
            return auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        }
        return false;
    }

    @Override
    public UserResponse createUser(UserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already exists!");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists!");
        }

        String targetRole = request.getRole() != null ? request.getRole() : "ROLE_USER";
        if (("ROLE_ADMIN".equals(targetRole) || "ROLE_SUPER_ADMIN".equals(targetRole)) && !isCurrentUserSuperAdmin()) {
            throw new AccessDeniedException("You do not have permission to create an admin account");
        }

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BadRequestException("Password is required");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .role(targetRole)
                .build();

        User savedUser = userRepository.save(user);
        return mapToResponse(savedUser);
    }

    @Override
    public PageResponse<UserResponse> getAllUsers(Specification<User> spec, Pageable pageable) {
        Page<User> page = userRepository.findAll(spec, pageable);
        List<UserResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.<UserResponse>builder()
                .content(content)
                .pageNo(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Override
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapToResponse(user);
    }

    @Override
    public UserResponse updateUser(Long id, UserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (("ROLE_ADMIN".equals(user.getRole()) || "ROLE_SUPER_ADMIN".equals(user.getRole())) && !isCurrentUserSuperAdmin()) {
            throw new AccessDeniedException("You do not have permission to modify an admin account");
        }

        if (request.getRole() != null && ("ROLE_ADMIN".equals(request.getRole()) || "ROLE_SUPER_ADMIN".equals(request.getRole())) && !isCurrentUserSuperAdmin()) {
            throw new AccessDeniedException("You do not have permission to grant admin roles");
        }

        user.setEmail(request.getEmail());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        User updatedUser = userRepository.save(user);
        
        // Xóa cache cũ để hệ thống nạp lại thông tin mới nhất
        redisTemplate.delete("userCache:" + user.getUsername());
        
        return mapToResponse(updatedUser);
    }

    @Override
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (("ROLE_ADMIN".equals(user.getRole()) || "ROLE_SUPER_ADMIN".equals(user.getRole())) && !isCurrentUserSuperAdmin()) {
            throw new AccessDeniedException("You do not have permission to delete an admin account");
        }

        user.setDeleted(true);
        user.setDeletedAt(java.time.LocalDateTime.now());
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            user.setDeletedBy(auth.getName());
        }

        userRepository.save(user);
        
        // Xóa cache để ngăn user đã bị xóa tiếp tục truy cập trong 10 phút
        redisTemplate.delete("userCache:" + user.getUsername());
    }

    @Override
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        return mapToResponse(user);
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}