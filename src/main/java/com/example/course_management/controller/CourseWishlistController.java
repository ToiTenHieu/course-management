package com.example.course_management.controller;

import com.example.course_management.dto.response.*;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.CourseWishlistService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wishlist")
@PreAuthorize("hasRole('STUDENT')")
public class CourseWishlistController {
  private final CourseWishlistService wishlist;

  public CourseWishlistController(CourseWishlistService wishlist) { this.wishlist = wishlist; }

  @GetMapping
  public ApiResponse<PageResponse<CourseResponse>> list(
      @RequestParam(defaultValue = "") String search, @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "9") int size, @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("OK", wishlist.list(search, page, size, actor));
  }

  @GetMapping("/status")
  public ApiResponse<List<Integer>> status(@RequestParam(defaultValue = "") List<Integer> ids,
      @AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("OK", wishlist.savedIds(ids, actor));
  }

  @PutMapping("/{courseId}")
  public ApiResponse<Void> save(@PathVariable Integer courseId, @AuthenticationPrincipal CustomUserDetails actor) {
    wishlist.save(courseId, actor);
    return ApiResponse.success("Đã lưu vào khóa quan tâm", null);
  }

  @DeleteMapping("/{courseId}")
  public ApiResponse<Void> remove(@PathVariable Integer courseId, @AuthenticationPrincipal CustomUserDetails actor) {
    wishlist.remove(courseId, actor);
    return ApiResponse.success("Đã bỏ lưu khóa học", null);
  }
}
