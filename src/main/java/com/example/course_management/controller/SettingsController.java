package com.example.course_management.controller;

import com.example.course_management.config.LearningSettings;
import com.example.course_management.dto.request.SaveSettingsRequest;
import com.example.course_management.dto.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@PreAuthorize("hasRole('ADMIN')")
public class SettingsController {
  private final LearningSettings settings;

  public SettingsController(LearningSettings settings) { this.settings = settings; }

  @GetMapping
  public ApiResponse<Map<String, Object>> get() {
    return ApiResponse.success("OK", Map.of("values", settings.current(),
        "uploadCeilingBytes", settings.uploadCeilingBytes()));
  }

  @PutMapping
  public ApiResponse<SaveSettingsRequest> save(@Valid @RequestBody SaveSettingsRequest request) {
    return ApiResponse.success("Đã lưu cài đặt", settings.save(request));
  }
}
