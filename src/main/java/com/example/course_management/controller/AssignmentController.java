package com.example.course_management.controller;

import com.example.course_management.dto.response.*;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.AssignmentService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class AssignmentController {
  private final AssignmentService service;
  public AssignmentController(AssignmentService service) { this.service=service; }
  @GetMapping("/api/lessons/{id}/assignment")
  public ApiResponse<AssignmentService.View> view(@PathVariable int id,@AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("OK",service.view(id,actor));
  }
  @PutMapping("/api/lessons/{id}/assignment") @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
  public ApiResponse<AssignmentService.Definition> save(@PathVariable int id,@Valid @RequestBody AssignmentService.Save r,
      @AuthenticationPrincipal CustomUserDetails actor) { return ApiResponse.success("Đã lưu bài tập",service.save(id,r,actor)); }
  @PostMapping("/api/lessons/{id}/assignment/submissions") @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<AssignmentService.Submission> submit(@PathVariable int id,@RequestParam int expectedRevision,
      @RequestParam String submissionKey,@RequestParam(defaultValue="") String answer,
      @RequestParam(required=false) MultipartFile file,@AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("Đã nộp bài",service.submit(id,expectedRevision,submissionKey,answer,file,actor));
  }
  @GetMapping("/api/lessons/{id}/assignment/submissions") @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
  public ApiResponse<PageResponse<AssignmentService.Submission>> list(@PathVariable int id,
      @RequestParam(defaultValue="false") boolean ungradedOnly,@RequestParam(defaultValue="0") int page,
      @RequestParam(defaultValue="10") int size,@AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("OK",service.list(id,ungradedOnly,page,size,actor));
  }
  @PutMapping("/api/assignment-submissions/{id}/grade") @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
  public ApiResponse<AssignmentService.Submission> grade(@PathVariable int id,@Valid @RequestBody AssignmentService.Grade r,
      @AuthenticationPrincipal CustomUserDetails actor) { return ApiResponse.success("Đã lưu điểm",service.grade(id,r,actor)); }
  @GetMapping("/api/assignment-submissions/{id}/file")
  public ResponseEntity<byte[]> file(@PathVariable int id,@AuthenticationPrincipal CustomUserDetails actor) {
    var f=service.download(id,actor);
    return ResponseEntity.ok().contentType(MediaType.parseMediaType(f.mediaType()))
        .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(f.name(),StandardCharsets.UTF_8).build().toString())
        .header("X-Content-Type-Options","nosniff").header("Content-Security-Policy","default-src 'none'; sandbox")
        .cacheControl(CacheControl.noStore()).body(f.content());
  }
}
