package com.example.course_management.controller;

import com.example.course_management.dto.request.*;
import com.example.course_management.dto.response.ApiResponse;
import com.example.course_management.dto.response.UserResponse;
import com.example.course_management.entity.Role;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  // Endpoint 4 (+ 31): GET /api/users — ADMIN, lọc theo role/status
  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<List<UserResponse>> getUsers(
      @RequestParam(required = false) Role role, @RequestParam(required = false) String status) {
    if (status != null
        && !status.equalsIgnoreCase("active")
        && !status.equalsIgnoreCase("inactive"))
      throw new com.example.course_management.exception.BadRequestException(
          "Trạng thái phải là active hoặc inactive");
    Boolean isActive = status == null ? null : status.equalsIgnoreCase("active");
    return ApiResponse.success("OK", userService.getUsers(role, isActive));
  }

  // Endpoint 5: GET /api/users/{user_id} — ADMIN
  @GetMapping("/{userId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<UserResponse> getUserById(@PathVariable Integer userId) {
    return ApiResponse.success("OK", userService.getUserById(userId));
  }

  // Endpoint 6: POST /api/users — ADMIN
  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
    return ApiResponse.success("Tạo người dùng thành công", userService.createUser(request));
  }

  // Endpoint 7: PUT /api/users/{user_id}/role — ADMIN
  @PutMapping("/{userId}/role")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<UserResponse> updateRole(
      @PathVariable Integer userId, @Valid @RequestBody UpdateRoleRequest request) {
    return ApiResponse.success("Cập nhật role thành công", userService.updateRole(userId, request));
  }

  // Endpoint 8: PUT /api/users/{user_id}/status — ADMIN
  @PutMapping("/{userId}/status")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<UserResponse> updateStatus(
      @PathVariable Integer userId, @Valid @RequestBody UpdateStatusRequest request) {
    return ApiResponse.success(
        "Cập nhật trạng thái thành công", userService.updateStatus(userId, request));
  }

  // Endpoint 9: DELETE /api/users/{user_id} — ADMIN
  @DeleteMapping("/{userId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<Void> deleteUser(@PathVariable Integer userId) {
    userService.deleteUser(userId);
    return ApiResponse.success("Xóa người dùng thành công", null);
  }

  // Endpoint 26: PUT /api/users/{user_id} — OWNER hoặc ADMIN
  @PutMapping("/{userId}")
  @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.user.userId")
  public ApiResponse<UserResponse> updateProfile(
      @PathVariable Integer userId, @Valid @RequestBody UpdateUserRequest request) {
    return ApiResponse.success(
        "Cập nhật hồ sơ thành công", userService.updateProfile(userId, request));
  }

  // Save administrative profile/role edits in one transaction.
  @PutMapping("/{userId}/management")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<UserResponse> manageUser(
      @PathVariable Integer userId, @Valid @RequestBody ManageUserRequest request) {
    return ApiResponse.success("Đã cập nhật thông tin và vai trò", userService.manageUser(userId, request));
  }

  // Endpoint 27: PUT /api/users/{user_id}/password — OWNER hoặc ADMIN
  @PutMapping("/{userId}/password")
  @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.user.userId")
  public ApiResponse<Void> changePassword(
      @PathVariable Integer userId,
      @Valid @RequestBody ChangePasswordRequest request,
      @AuthenticationPrincipal CustomUserDetails actor) {
    userService.changePassword(userId, request, actor);
    return ApiResponse.success("Đổi mật khẩu thành công", null);
  }
}
