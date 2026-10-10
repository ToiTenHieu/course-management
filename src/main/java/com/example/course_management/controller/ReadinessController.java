package com.example.course_management.controller;

import com.example.course_management.config.ApplicationReadiness;
import com.example.course_management.dto.response.ApiResponse;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReadinessController {
  private final ApplicationReadiness readiness;

  public ReadinessController(ApplicationReadiness readiness) {
    this.readiness = readiness;
  }

  @GetMapping("/api/auth/ready")
  public ResponseEntity<ApiResponse<Map<String, Boolean>>> ready() {
    boolean ready = readiness.isReady();
    return ResponseEntity.status(ready ? 200 : 503)
        .cacheControl(org.springframework.http.CacheControl.noStore())
        .body(ApiResponse.success(ready ? "Sẵn sàng" : "Đang khởi động", Map.of("ready", ready)));
  }
}
