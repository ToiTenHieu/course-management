package com.example.course_management.service;

import com.example.course_management.dto.request.*;
import com.example.course_management.dto.response.UserResponse;
import com.example.course_management.entity.Role;
import com.example.course_management.security.CustomUserDetails;
import java.util.List;

public interface UserService {
  List<UserResponse> getUsers(Role role, Boolean isActive);

  UserResponse getUserById(Integer userId);

  UserResponse createUser(CreateUserRequest request);

  UserResponse updateRole(Integer userId, UpdateRoleRequest request);

  UserResponse updateStatus(Integer userId, UpdateStatusRequest request);

  void deleteUser(Integer userId);

  UserResponse updateProfile(Integer userId, UpdateUserRequest request);

  void changePassword(Integer userId, ChangePasswordRequest request, CustomUserDetails actor);
}
