package com.example.course_management.controller;

import com.example.course_management.dto.request.CreateEnrollmentRequest;
import com.example.course_management.dto.request.SaveLessonNoteRequest;
import com.example.course_management.dto.response.ApiResponse;
import com.example.course_management.dto.response.EnrollmentDetailResponse;
import com.example.course_management.dto.response.EnrollmentResponse;
import com.example.course_management.dto.response.LessonNoteResponse;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.EnrollmentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/enrollments")
@PreAuthorize("hasRole('STUDENT')")
public class EnrollmentController {

  private final EnrollmentService enrollmentService;

  public EnrollmentController(EnrollmentService enrollmentService) {
    this.enrollmentService = enrollmentService;
  }

  // Endpoint 22: GET /api/enrollments — STUDENT
  @GetMapping
  public ApiResponse<List<EnrollmentResponse>> getMyEnrollments(
      @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("OK", enrollmentService.getMyEnrollments(actor));
  }

  // Endpoint 23: POST /api/enrollments — STUDENT
  @PostMapping
  public ApiResponse<EnrollmentResponse> enroll(
      @Valid @RequestBody CreateEnrollmentRequest request,
      @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success(
        "Đăng ký khóa học thành công", enrollmentService.enroll(request.getCourseId(), actor));
  }

  // Endpoint 24: GET /api/enrollments/{enrollment_id} — STUDENT
  @GetMapping("/{enrollmentId}")
  public ApiResponse<EnrollmentDetailResponse> getDetail(
      @PathVariable Integer enrollmentId, @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("OK", enrollmentService.getEnrollmentDetail(enrollmentId, actor));
  }

  // Endpoint 25: PUT /api/enrollments/{enrollment_id}/complete_lesson/{lesson_id} — STUDENT
  @PutMapping("/{enrollmentId}/complete_lesson/{lessonId}")
  public ApiResponse<EnrollmentDetailResponse> completeLesson(
      @PathVariable Integer enrollmentId,
      @PathVariable Integer lessonId,
      @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success(
        "Đánh dấu hoàn thành thành công",
        enrollmentService.completeLesson(enrollmentId, lessonId, actor));
  }

  @GetMapping("/{enrollmentId}/notes/{lessonId}")
  public ApiResponse<LessonNoteResponse> getNote(
      @PathVariable Integer enrollmentId,
      @PathVariable Integer lessonId,
      @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("OK", enrollmentService.getNote(enrollmentId, lessonId, actor));
  }

  @PutMapping("/{enrollmentId}/notes/{lessonId}")
  public ApiResponse<LessonNoteResponse> saveNote(
      @PathVariable Integer enrollmentId,
      @PathVariable Integer lessonId,
      @Valid @RequestBody SaveLessonNoteRequest request,
      @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success(
        "Đã lưu ghi chú",
        enrollmentService.saveNote(enrollmentId, lessonId, request.note(), actor));
  }

  @PutMapping("/{enrollmentId}/access_lesson/{lessonId}")
  public ApiResponse<Void> accessLesson(
      @PathVariable Integer enrollmentId,
      @PathVariable Integer lessonId,
      @AuthenticationPrincipal CustomUserDetails actor) {
    enrollmentService.accessLesson(enrollmentId, lessonId, actor);
    return ApiResponse.success("OK", null);
  }
}
