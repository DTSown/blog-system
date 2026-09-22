package com.son.blog.service;

import com.son.blog.dto.UserRequest;
import com.son.blog.dto.UserResponse;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import com.son.blog.entity.User;
import com.son.blog.dto.PageResponse;

public interface UserService {
    UserResponse createUser(UserRequest request);

    PageResponse<UserResponse> getAllUsers(Specification<User> spec, Pageable pageable);

    UserResponse getUserById(Long id);

    UserResponse updateUser(Long id, UserRequest request);

    void deleteUser(Long id);

    UserResponse getUserByUsername(String username);
}