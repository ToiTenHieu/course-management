package com.example.course_management.controller;

import com.example.course_management.dto.request.*;
import com.example.course_management.dto.response.*;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.AiAssistantService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class AiController {
  private final AiAssistantService service;

  public AiController(AiAssistantService service) {
    this.service = service;
  }

  @PostMapping("/api/lessons/{lessonId}/ai/tutor")
  public ApiResponse<AiTutorResponse> tutor(
      @PathVariable Integer lessonId,
      @Valid @RequestBody AiTutorRequest request,
      @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("OK", service.tutor(lessonId, request, actor));
  }

  @PostMapping("/api/lessons/{lessonId}/ai/summary")
  public ApiResponse<AiTutorResponse> summary(
      @PathVariable Integer lessonId, @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("OK", service.summary(lessonId, actor));
  }

  @PostMapping("/api/assignment-submissions/{id}/ai-grade")
  @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
  public ApiResponse<AiAssistantService.GradeSuggestion> gradeSuggestion(
      @PathVariable int id, @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success(
        "AI đã đề xuất điểm. Hãy xem lại trước khi lưu", service.gradeSuggestion(id, actor));
  }

  @PostMapping("/api/courses/{courseId}/ai/insights")
  @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
  public ApiResponse<AiTutorResponse> insights(
      @PathVariable int courseId, @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("OK", service.classInsights(courseId, actor));
  }

  @PostMapping("/api/ai/course-recommendations")
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<List<AiAssistantService.CourseSuggestion>> recommendations(
      @Valid @RequestBody AiRecommendationRequest request,
      @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("OK", service.recommendations(request.goal(), actor));
  }

  @PostMapping("/api/lessons/{lessonId}/quiz/ai-draft")
  @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
  public ApiResponse<List<SaveQuizRequest.Question>> quizDraft(
      @PathVariable Integer lessonId,
      @Valid @RequestBody AiQuizDraftRequest request,
      @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success(
        "AI đã soạn nháp. Hãy kiểm tra trước khi lưu", service.quizDraft(lessonId, request.count(), actor));
  }
}
