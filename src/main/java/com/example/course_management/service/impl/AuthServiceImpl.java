package com.example.course_management.service.impl;

import com.example.course_management.dto.request.LoginRequest;
import com.example.course_management.dto.response.UserProfileResponse;
import com.example.course_management.exception.UnauthorizedException;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

  private final AuthenticationManager authenticationManager;

  public AuthServiceImpl(AuthenticationManager authenticationManager) {
    this.authenticationManager = authenticationManager;
  }

  @Override
  public UserProfileResponse login(LoginRequest request, HttpServletRequest httpRequest) {
    if (!com.example.course_management.security.PasswordPolicy.fitsBcrypt(request.getPassword()))
      throw new org.springframework.security.authentication.BadCredentialsException("Sai username hoặc password");
    Authentication auth =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

    if (httpRequest.getSession(false) != null) httpRequest.changeSessionId();
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(auth);
    SecurityContextHolder.setContext(context);

    httpRequest
        .getSession(true)
        .setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

    CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
    return toProfileResponse(userDetails);
  }

  @Override
  public UserProfileResponse getCurrentUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails)) {
      throw new UnauthorizedException("Chưa đăng nhập");
    }
    CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
    return toProfileResponse(userDetails);
  }

  @Override
  public boolean verifySession() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return auth != null && auth.getPrincipal() instanceof CustomUserDetails;
  }

  @Override
  public void logout(HttpServletRequest request) {
    HttpSession session = request.getSession(false); // false = không tạo mới nếu chưa có
    if (session != null) {
      session.invalidate(); // hủy session ở server — đây là bước quan trọng nhất
    }
    SecurityContextHolder.clearContext(); // xóa authentication khỏi context hiện tại
  }

  private UserProfileResponse toProfileResponse(CustomUserDetails userDetails) {
    var user = userDetails.getUser();
    return UserProfileResponse.builder()
        .userId(user.getUserId())
        .username(user.getUsername())
        .email(user.getEmail())
        .fullName(user.getFullName())
        .role(user.getRole().name())
        .isActive(user.getIsActive())
        .build();
  }
}
