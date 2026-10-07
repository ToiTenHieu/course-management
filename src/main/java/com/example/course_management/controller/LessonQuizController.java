package com.example.course_management.controller;

import com.example.course_management.dto.request.*;
import com.example.course_management.dto.response.*;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.LessonQuizService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class LessonQuizController {
  private final LessonQuizService service;

  public LessonQuizController(LessonQuizService service) {
    this.service = service;
  }

  @GetMapping("/api/lessons/{lessonId}/quiz")
  public ApiResponse<QuizResponse> get(
      @PathVariable Integer lessonId, @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("OK", service.get(lessonId, actor));
  }

  @PutMapping("/api/lessons/{lessonId}/quiz")
  @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
  public ApiResponse<QuizResponse> save(
      @PathVariable Integer lessonId,
      @Valid @RequestBody SaveQuizRequest request,
      @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("Đã lưu phiên bản quiz", service.save(lessonId, request, actor));
  }

  @PostMapping("/api/lessons/{lessonId}/quiz/attempts")
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<QuizAttemptResponse> submit(
      @PathVariable Integer lessonId,
      @Valid @RequestBody SubmitQuizRequest request,
      @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("Đã chấm bài", service.submit(lessonId, request, actor));
  }

  @GetMapping("/api/lessons/{lessonId}/quiz/attempts")
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<PageResponse<QuizAttemptResponse.Summary>> history(
      @PathVariable Integer lessonId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "5") int size,
      @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("OK", service.history(lessonId, page, size, actor));
  }

  @GetMapping("/api/quiz-attempts/{id}")
  public ApiResponse<QuizAttemptResponse> attempt(
      @PathVariable Integer id, @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("OK", service.attempt(id, actor));
  }

  @GetMapping("/api/lessons/{lessonId}/quiz/statistics")
  @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
  public ApiResponse<QuizStatisticsResponse> statistics(
      @PathVariable Integer lessonId, @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("OK", service.statistics(lessonId, actor));
  }
}
