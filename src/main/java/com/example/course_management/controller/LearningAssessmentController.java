package com.example.course_management.controller;

import com.example.course_management.dto.response.ApiResponse;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.LearningAssessmentService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/learning-assessment")
@PreAuthorize("hasRole('STUDENT')")
public class LearningAssessmentController {
  private final LearningAssessmentService service;
  public LearningAssessmentController(LearningAssessmentService service) {this.service=service;}
  @GetMapping public ApiResponse<LearningAssessmentService.Definition> definition() {
    return ApiResponse.success("OK",service.definition());
  }
  @GetMapping("/latest") public ApiResponse<LearningAssessmentService.Result> latest(@AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("OK",service.latest(actor));
  }
  @PostMapping public ApiResponse<LearningAssessmentService.Result> submit(@Valid @RequestBody LearningAssessmentService.Submit request,@AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("Đã lưu lộ trình gợi ý",service.submit(request,actor));
  }
}
