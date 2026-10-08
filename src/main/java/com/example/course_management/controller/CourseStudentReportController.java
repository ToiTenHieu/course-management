package com.example.course_management.controller;

import com.example.course_management.dto.response.ApiResponse;
import com.example.course_management.entity.EnrollmentStatus;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.CourseStudentReportService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class CourseStudentReportController {
  private final CourseStudentReportService reports;
  public CourseStudentReportController(CourseStudentReportService reports) { this.reports = reports; }

  @GetMapping("/api/courses/{courseId}/students")
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
  public ApiResponse<CourseStudentReportService.Report> report(@PathVariable int courseId,
      @RequestParam(defaultValue="") String search, @RequestParam(required=false) EnrollmentStatus status,
      @RequestParam(defaultValue="name") String sort, @RequestParam(defaultValue="0") int page,
      @RequestParam(defaultValue="10") int size, @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("OK",reports.get(courseId,search,status,sort,page,size,actor));
  }
}
