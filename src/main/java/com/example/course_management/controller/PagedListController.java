package com.example.course_management.controller;

import com.example.course_management.dto.response.*;
import com.example.course_management.entity.*;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.PagedListService;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lists")
public class PagedListController {
  private final PagedListService lists;

  public PagedListController(PagedListService lists) {
    this.lists = lists;
  }

  @GetMapping("/users")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<PageResponse<UserResponse>> users(
      @RequestParam(defaultValue = "") String search,
      @RequestParam(required = false) Role role,
      @RequestParam(defaultValue = "") String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("OK", lists.users(search, role, status, page, size, actor));
  }

  @GetMapping("/payments")
  @PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
  public ApiResponse<PageResponse<PaymentResponse>> payments(
      @RequestParam(defaultValue = "") String search,
      @RequestParam(required = false) PaymentStatus status,
      @RequestParam(defaultValue = "new") String sort,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("OK", lists.payments(search, status, sort, page, size, actor));
  }

  @GetMapping("/enrollments")
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<PageResponse<LearningCourseResponse>> learning(
      @RequestParam(defaultValue = "") String search,
      @RequestParam(required = false) EnrollmentStatus status,
      @RequestParam(defaultValue = "new") String sort,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "9") int size,
      @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("OK", lists.learning(search, status, sort, page, size, actor));
  }

  @GetMapping("/notifications")
  public ApiResponse<PageResponse<NotificationResponse>> notifications(
      @RequestParam(defaultValue = "") String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("OK", lists.notifications(status, page, size, actor));
  }

  @GetMapping("/summary")
  public ApiResponse<Map<String, Long>> summary(@AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("OK", lists.summary(actor));
  }

  @GetMapping("/reviews")
  public ApiResponse<PageResponse<ReviewResponse>> reviews(
      @RequestParam Integer courseId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "6") int size,
      @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("OK", lists.reviews(courseId, page, size, actor));
  }

  @GetMapping("/own-review/{courseId}")
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<ReviewResponse> ownReview(
      @PathVariable Integer courseId, @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("OK", lists.ownReview(courseId, actor));
  }

  @PutMapping("/notifications/read-all")
  public ApiResponse<Integer> markAllRead(@AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("Đã đánh dấu thông báo của bạn là đã đọc", lists.markAllRead(actor));
  }

  @GetMapping("/course-state/{courseId}")
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<Map<String, Object>> courseState(
      @PathVariable Integer courseId, @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("OK", lists.courseState(courseId, actor));
  }

  @GetMapping("/student-report/{userId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<Map<String, Object>> studentReport(
      @PathVariable Integer userId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "8") int size,
      @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("OK", lists.studentReport(userId, page, size, actor));
  }

  @GetMapping("/teacher-report/{userId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<Map<String, Object>> teacherReport(
      @PathVariable Integer userId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "8") int size,
      @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("OK", lists.teacherReport(userId, page, size, actor));
  }
}
