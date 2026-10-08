package com.example.course_management.controller;

import com.example.course_management.dto.request.SaveLessonDraftRequest;
import com.example.course_management.dto.response.ApiResponse;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.LessonDraftService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
public class LessonDraftController {
  private final LessonDraftService drafts;
  public LessonDraftController(LessonDraftService drafts) { this.drafts = drafts; }

  @GetMapping("/api/courses/{courseId}/lesson-drafts/{key}")
  public ApiResponse<LessonDraftService.Draft> get(@PathVariable int courseId,
      @PathVariable int key, @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("OK", drafts.get(courseId, key, actor));
  }
  @PutMapping("/api/courses/{courseId}/lesson-drafts/{key}")
  public ApiResponse<LessonDraftService.Draft> save(@PathVariable int courseId,
      @PathVariable int key, @Valid @RequestBody SaveLessonDraftRequest request,
      @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("Đã lưu bản nháp", drafts.save(courseId, key, request, actor));
  }
  @DeleteMapping("/api/courses/{courseId}/lesson-drafts/{key}")
  public ApiResponse<Void> delete(@PathVariable int courseId, @PathVariable int key,
      @RequestParam int revision, @AuthenticationPrincipal CustomUserDetails actor) {
    drafts.delete(courseId, key, revision, actor);
    return ApiResponse.success("Đã bỏ bản nháp", null);
  }
  @GetMapping("/api/lessons/{id}/content-versions")
  public ApiResponse<List<LessonDraftService.Version>> history(@PathVariable int id,
      @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("OK", drafts.history(id, actor));
  }
}
