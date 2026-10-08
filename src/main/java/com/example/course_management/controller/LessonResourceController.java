package com.example.course_management.controller;

import com.example.course_management.dto.response.ApiResponse;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.LessonResourceService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class LessonResourceController {
  private final LessonResourceService resources;
  public LessonResourceController(LessonResourceService resources) { this.resources = resources; }

  @GetMapping("/api/lessons/{id}/resources")
  public ApiResponse<List<LessonResourceService.Resource>> list(@PathVariable Integer id,
      @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("OK", resources.list(id, actor));
  }

  @PostMapping("/api/lessons/{id}/resources")
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
  public ApiResponse<List<LessonResourceService.Resource>> upload(@PathVariable Integer id,
      @RequestParam MultipartFile file, @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("Đã tải tài liệu", resources.upload(id, file, actor));
  }

  @GetMapping("/api/lesson-resources/{id}/content")
  public ResponseEntity<byte[]> content(@PathVariable Integer id,
      @AuthenticationPrincipal CustomUserDetails actor) {
    var result = resources.download(id, actor);
    var resource = result.resource();
    var disposition = resource.mediaType().startsWith("image/")
        ? ContentDisposition.inline() : ContentDisposition.attachment();
    return ResponseEntity.ok().contentType(MediaType.parseMediaType(resource.mediaType()))
        .header(HttpHeaders.CONTENT_DISPOSITION, disposition.filename(resource.name(), StandardCharsets.UTF_8).build().toString())
        .header("X-Content-Type-Options", "nosniff")
        .header("Content-Security-Policy", "default-src 'none'; sandbox")
        .cacheControl(CacheControl.noStore()).body(result.content());
  }

  @DeleteMapping("/api/lesson-resources/{id}")
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
  public ApiResponse<Void> delete(@PathVariable Integer id,
      @AuthenticationPrincipal CustomUserDetails actor) {
    resources.delete(id, actor);
    return ApiResponse.success("Đã xóa tài liệu", null);
  }
}
