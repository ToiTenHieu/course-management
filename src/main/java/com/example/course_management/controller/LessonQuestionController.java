package com.example.course_management.controller;

import com.example.course_management.dto.request.*;
import com.example.course_management.dto.response.*;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.LessonQuestionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class LessonQuestionController {
  private final LessonQuestionService service;

  public LessonQuestionController(LessonQuestionService service) {
    this.service = service;
  }

  @GetMapping("/api/lessons/{lessonId}/questions")
  public ApiResponse<PageResponse<LessonQuestionResponse>> list(
      @PathVariable Integer lessonId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("OK", service.list(lessonId, page, size, actor));
  }

  @GetMapping("/api/questions/{id}")
  public ApiResponse<LessonQuestionResponse> get(
      @PathVariable Integer id, @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("OK", service.get(id, actor));
  }

  @PostMapping("/api/lessons/{lessonId}/questions")
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<LessonQuestionResponse> ask(
      @PathVariable Integer lessonId,
      @Valid @RequestBody QuestionTextRequest request,
      @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("Đã gửi câu hỏi", service.ask(lessonId, request.body(), actor));
  }

  @PutMapping("/api/questions/{id}/answer")
  @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
  public ApiResponse<LessonQuestionResponse> answer(
      @PathVariable Integer id,
      @Valid @RequestBody QuestionTextRequest request,
      @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("Đã lưu phản hồi", service.answer(id, request.body(), actor));
  }

  @PutMapping("/api/questions/{id}/visibility")
  @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
  public ApiResponse<LessonQuestionResponse> visibility(
      @PathVariable Integer id,
      @Valid @RequestBody QuestionVisibilityRequest request,
      @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success(
        "Đã cập nhật câu hỏi", service.visibility(id, request.hidden(), actor));
  }
}
