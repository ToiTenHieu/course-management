package com.example.course_management.controller;

import com.example.course_management.dto.request.*;
import com.example.course_management.dto.response.ApiResponse;
import com.example.course_management.dto.response.CourseResponse;
import com.example.course_management.dto.response.PageResponse;
import com.example.course_management.entity.CourseStatus;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.CourseService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

  private final CourseService courseService;

  public CourseController(CourseService courseService) {
    this.courseService = courseService;
  }

  @GetMapping("/catalog")
  public ApiResponse<PageResponse<CourseResponse>> getCatalog(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) Integer teacherId,
      @RequestParam(required = false) CourseStatus status,
      @RequestParam(required = false) String category,
      @RequestParam(defaultValue = "false") boolean freeOnly,
      @RequestParam(defaultValue = "new") String sort,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "9") int size,
      @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success(
        "OK",
        courseService.getCatalog(
            search, teacherId, status, category, freeOnly, sort, page, size, actor));
  }

  @GetMapping("/categories")
  public ApiResponse<List<String>> getCategories(
      @RequestParam(required = false) Integer teacherId,
      @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("OK", courseService.getCategories(teacherId, actor));
  }

  // Endpoint 10 + 28 + 29 + 32: GET /api/courses — AUTH, lọc search/teacherId/status
  @GetMapping
  public ApiResponse<List<CourseResponse>> getCourses(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) Integer teacherId,
      @RequestParam(required = false) CourseStatus status,
      @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("OK", courseService.getCourses(search, teacherId, status, actor));
  }

  // Endpoint 11: GET /api/courses/{course_id} — AUTH
  @GetMapping("/{courseId}")
  public ApiResponse<CourseResponse> getCourseById(
      @PathVariable Integer courseId, @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("OK", courseService.getCourseById(courseId, actor));
  }

  // Endpoint 12: POST /api/courses — ADMIN
  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<CourseResponse> createCourse(@Valid @RequestBody CreateCourseRequest request) {
    return ApiResponse.success("Tạo khóa học thành công", courseService.createCourse(request));
  }

  // Teachers may edit metadata for their own course; pricing/assignment stay admin-only.
  @PutMapping("/{courseId}")
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
  public ApiResponse<CourseResponse> updateCourse(
      @PathVariable Integer courseId,
      @Valid @RequestBody UpdateCourseRequest request,
      @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success(
        "Cập nhật khóa học thành công", courseService.updateCourse(courseId, request, actor));
  }

  // Endpoint 14: PUT /api/courses/{course_id}/status — ADMIN
  @PutMapping("/{courseId}/status")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<CourseResponse> updateStatus(
      @PathVariable Integer courseId, @Valid @RequestBody UpdateCourseStatusRequest request) {
    return ApiResponse.success(
        "Cập nhật trạng thái thành công", courseService.updateStatus(courseId, request));
  }

  // Endpoint 15: DELETE /api/courses/{course_id} — ADMIN
  @DeleteMapping("/{courseId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<Void> deleteCourse(@PathVariable Integer courseId) {
    courseService.deleteCourse(courseId);
    return ApiResponse.success("Xóa khóa học thành công", null);
  }
}
