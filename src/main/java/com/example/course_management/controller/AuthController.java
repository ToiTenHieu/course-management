package com.example.course_management.controller;

import com.example.course_management.dto.request.LoginRequest;
import com.example.course_management.dto.response.ApiResponse;
import com.example.course_management.dto.response.UserProfileResponse;
import com.example.course_management.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<UserProfileResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
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
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        authService.logout(request);
        return ApiResponse.success("Đăng xuất thành công", null);
    }
}