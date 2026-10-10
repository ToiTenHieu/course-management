package com.example.course_management.controller;

import com.example.course_management.dto.response.ApiResponse;
import com.example.course_management.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class PasswordRecoveryController {
  public record Request(@NotBlank @Email(message="Email không hợp lệ") @Size(max=100) String email) {}
  public record Reset(@NotBlank @Size(max=100) String token,@NotBlank @Size(min=8,max=64) String newPassword) {}
  private final PasswordRecoveryService service;
  private final PasswordRecoveryDelivery delivery;
  public PasswordRecoveryController(PasswordRecoveryService service,PasswordRecoveryDelivery delivery) {this.service=service;this.delivery=delivery;}
  @PostMapping("/api/auth/forgot-password") public ResponseEntity<ApiResponse<Void>> request(@Valid @RequestBody Request body,HttpServletRequest request) {
    service.request(body.email(),request.getRemoteAddr());
    return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).body(ApiResponse.success(PasswordRecoveryService.ACCEPTED,null));
  }
  @PostMapping("/api/auth/reset-password") public ResponseEntity<ApiResponse<Void>> reset(@Valid @RequestBody Reset body,HttpServletRequest request) {
    service.reset(body.token(),body.newPassword(),request.getRemoteAddr());
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success("Đã đổi mật khẩu. Hãy đăng nhập bằng mật khẩu mới.",null));
  }
  @GetMapping("/api/demo/recovery-mailbox") @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<List<PasswordRecoveryDelivery.Message>>> mailbox() {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("Referrer-Policy","no-referrer").body(ApiResponse.success("OK",delivery.list()));
  }
}
