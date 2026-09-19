package com.son.blog.service;

import com.son.blog.dto.UserRequest;
import com.son.blog.dto.UserResponse;

import java.util.List;

public interface UserService {
    UserResponse createUser(UserRequest request);
    List<UserResponse> getAllUsers();
    UserResponse getUserById(Long id);
    UserResponse updateUser(Long id, UserRequest request);
    void deleteUser(Long id);
    UserResponse getUserByUsername(String username);
}