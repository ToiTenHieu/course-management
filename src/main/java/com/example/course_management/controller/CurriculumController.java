package com.example.course_management.controller;

import com.example.course_management.dto.response.ApiResponse;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.CurriculumService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/courses/{courseId}/curriculum")
@PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
public class CurriculumController {
  private final CurriculumService service;
  public CurriculumController(CurriculumService service) { this.service=service; }
  @GetMapping public ApiResponse<CurriculumService.Curriculum> get(@PathVariable int courseId,
      @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("OK",service.get(courseId,actor));
  }
  @PutMapping public ApiResponse<CurriculumService.Curriculum> save(@PathVariable int courseId,
      @Valid @RequestBody CurriculumService.Plan plan, @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("Đã lưu chương trình",service.save(courseId,plan,actor));
  }
}
