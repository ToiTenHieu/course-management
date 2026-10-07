package com.example.course_management.controller;

import com.example.course_management.dto.request.ReviewRequest;
import com.example.course_management.dto.response.ApiResponse;
import com.example.course_management.dto.response.ReviewResponse;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.ReviewService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class ReviewController {

  private final ReviewService reviewService;

  public ReviewController(ReviewService reviewService) {
    this.reviewService = reviewService;
  }

  // Endpoint 40 — AUTH
  @GetMapping("/api/courses/{courseId}/reviews")
  public ApiResponse<List<ReviewResponse>> getReviews(
      @PathVariable Integer courseId, @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("OK", reviewService.getReviews(courseId, actor));
  }

  // Endpoint 41 — STUDENT
  @PostMapping("/api/courses/{courseId}/reviews")
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<ReviewResponse> create(
      @PathVariable Integer courseId,
      @Valid @RequestBody ReviewRequest request,
      @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success(
        "Gửi đánh giá thành công", reviewService.create(courseId, request, actor));
  }

  // Endpoint 42 — OWNER, ADMIN (kiểm tra trong Service)
  @PutMapping("/api/reviews/{reviewId}")
  public ApiResponse<ReviewResponse> update(
      @PathVariable Integer reviewId,
      @Valid @RequestBody ReviewRequest request,
      @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success(
        "Cập nhật đánh giá thành công", reviewService.update(reviewId, request, actor));
  }

  // Endpoint 43 — OWNER, ADMIN
  @DeleteMapping("/api/reviews/{reviewId}")
  public ApiResponse<Void> delete(
      @PathVariable Integer reviewId, @AuthenticationPrincipal CustomUserDetails actor) {
    reviewService.delete(reviewId, actor);
    return ApiResponse.success("Xóa đánh giá thành công", null);
  }
}
