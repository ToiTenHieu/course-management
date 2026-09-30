package com.example.course_management.controller;

import com.example.course_management.dto.request.*;
import com.example.course_management.dto.response.ApiResponse;
import com.example.course_management.dto.response.LessonPreviewResponse;
import com.example.course_management.dto.response.LessonResponse;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.LessonService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class LessonController {

    private final LessonService lessonService;

    public LessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    // Endpoint 16: GET /api/courses/{course_id}/lessons — AUTH
    @GetMapping("/api/courses/{courseId}/lessons")
    public ApiResponse<List<LessonResponse>> getLessonsByCourse(
            @PathVariable Integer courseId,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ApiResponse.success("OK", lessonService.getLessonsByCourse(courseId, actor));
    }

    // Endpoint 17: GET /api/lessons/{lesson_id} — AUTH
    @GetMapping("/api/lessons/{lessonId}")
    public ApiResponse<LessonResponse> getLessonById(
            @PathVariable Integer lessonId,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ApiResponse.success("OK", lessonService.getLessonById(lessonId, actor));
    }

    // Endpoint 18: POST /api/courses/{course_id}/lessons — TEACHER, ADMIN
    @PostMapping("/api/courses/{courseId}/lessons")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<LessonResponse> createLesson(
            @PathVariable Integer courseId,
            @Valid @RequestBody CreateLessonRequest request,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ApiResponse.success("Thêm bài học thành công", lessonService.createLesson(courseId, request, actor));
    }

    // Endpoint 19: PUT /api/lessons/{lesson_id} — TEACHER, ADMIN
    @PutMapping("/api/lessons/{lessonId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<LessonResponse> updateLesson(
            @PathVariable Integer lessonId,
            @Valid @RequestBody UpdateLessonRequest request,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ApiResponse.success("Cập nhật bài học thành công", lessonService.updateLesson(lessonId, request, actor));
    }

    // Endpoint 20: PUT /api/lessons/{lesson_id}/publish — TEACHER, ADMIN
    @PutMapping("/api/lessons/{lessonId}/publish")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<LessonResponse> updatePublishStatus(
            @PathVariable Integer lessonId,
            @Valid @RequestBody UpdateLessonPublishRequest request,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ApiResponse.success("Cập nhật trạng thái xuất bản thành công", lessonService.updatePublishStatus(lessonId, request, actor));
    }

    // Endpoint 21: DELETE /api/lessons/{lesson_id} — TEACHER, ADMIN
    @DeleteMapping("/api/lessons/{lessonId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<Void> deleteLesson(
            @PathVariable Integer lessonId,
            @AuthenticationPrincipal CustomUserDetails actor) {
        lessonService.deleteLesson(lessonId, actor);
        return ApiResponse.success("Xóa bài học thành công", null);
    }
    // Endpoint 44: GET /api/lessons/{lesson_id}/content_preview — AUTH
    @GetMapping("/api/lessons/{lessonId}/content_preview")
    public ApiResponse<LessonPreviewResponse> getContentPreview(
            @PathVariable Integer lessonId,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ApiResponse.success("OK", lessonService.getContentPreview(lessonId, actor));
    }
}