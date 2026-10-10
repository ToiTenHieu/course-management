package com.example.course_management.controller;

import com.example.course_management.dto.request.SaveTeacherProfileRequest;
import com.example.course_management.dto.response.ApiResponse;
import com.example.course_management.dto.response.TeacherProfileResponse;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.TeacherProfileService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teacher-profile")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherProfileController {
  private final TeacherProfileService profiles;

  public TeacherProfileController(TeacherProfileService profiles) {
    this.profiles = profiles;
  }

  @GetMapping
  public ApiResponse<TeacherProfileResponse> ownProfile(@AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("OK", profiles.getPublicProfile(actor.getUser().getUserId()));
  }

  @PutMapping
  public ApiResponse<TeacherProfileResponse> save(
      @AuthenticationPrincipal CustomUserDetails actor,
      @Valid @RequestBody SaveTeacherProfileRequest request) {
    return ApiResponse.success("Đã lưu hồ sơ giảng viên", profiles.save(actor.getUser().getUserId(), request));
  }
}
