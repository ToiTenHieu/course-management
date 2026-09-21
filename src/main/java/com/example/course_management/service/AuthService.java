package com.example.course_management.service;

import com.example.course_management.dto.request.LoginRequest;
import com.example.course_management.dto.response.UserProfileResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
    UserProfileResponse login(LoginRequest request, HttpServletRequest httpRequest);
    UserProfileResponse getCurrentUser();
    boolean verifySession();
    void logout(HttpServletRequest request);
}