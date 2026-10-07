package com.example.course_management.controller;

import com.example.course_management.dto.request.*;
import com.example.course_management.dto.response.ApiResponse;
import com.example.course_management.dto.response.UserProfileResponse;
import com.example.course_management.entity.Role;
import com.example.course_management.service.AuthService;
import com.example.course_management.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;

  private final UserService userService;

  @Value("${app.demo.enabled:false}")
  private boolean demo;

  public AuthController(AuthService authService, UserService userService) {
    this.authService = authService;
    this.userService = userService;
  }

  @GetMapping("/csrf")
  public ApiResponse<Map<String, String>> csrf(CsrfToken token) {
    return ApiResponse.success(
        "OK", Map.of("headerName", token.getHeaderName(), "token", token.getToken()));
  }

  @GetMapping("/config")
  public ApiResponse<Map<String, Boolean>> config() {
    return ApiResponse.success("OK", Map.of("demo", demo));
  }

  @PostMapping("/register")
  public ApiResponse<?> register(@Valid @RequestBody RegisterRequest request) {
    var user = new CreateUserRequest();
    user.setUsername(request.getUsername());
    user.setPassword(request.getPassword());
    user.setEmail(request.getEmail());
    user.setFullName(request.getFullName());
    user.setRole(Role.STUDENT);
    return ApiResponse.success(
        "Tạo tài khoản thành công, bạn có thể đăng nhập", userService.createUser(user));
  }

  @PostMapping("/login")
  public ApiResponse<UserProfileResponse> login(
      @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
    UserProfileResponse profile = authService.login(request, httpRequest);
    return ApiResponse.success("Đăng nhập thành công", profile);
  }

  @PostMapping("/verify")
  public ApiResponse<Boolean> verify() {
    boolean valid = authService.verifySession();
    return ApiResponse.success("Kiểm tra session", valid);
  }

  @GetMapping("/me")
  public ApiResponse<UserProfileResponse> me() {
    UserProfileResponse profile = authService.getCurrentUser();
    return ApiResponse.success("Lấy thông tin thành công", profile);
  }

  @GetMapping("/session")
  public ApiResponse<UserProfileResponse> session() {
    return ApiResponse.success(
        "OK", authService.verifySession() ? authService.getCurrentUser() : null);
  }

  @PostMapping("/logout")
  public ApiResponse<Void> logout(HttpServletRequest request) {
    authService.logout(request);
    return ApiResponse.success("Đăng xuất thành công", null);
  }
}
