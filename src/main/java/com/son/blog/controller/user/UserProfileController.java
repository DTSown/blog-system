package com.son.blog.controller.user;

import com.son.blog.dto.ApiResponse;
import com.son.blog.dto.UserRequest;
import com.son.blog.dto.UserResponse;
import com.son.blog.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return ResponseEntity.ok(ApiResponse.success(userService.getUserByUsername(username)));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(@Valid @RequestBody UserRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        UserResponse user = userService.getUserByUsername(username);
        // Note: this assumes userService.updateUser can handle updating own profile
        return ResponseEntity.ok(ApiResponse.success(userService.updateUser(user.getId(), request)));
    }
}
