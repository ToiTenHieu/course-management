package com.example.course_management.controller;

import com.example.course_management.dto.response.*;
import com.example.course_management.entity.CourseStatus;
import com.example.course_management.service.CourseService;
import com.example.course_management.service.TeacherProfileService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/** Public marketing metadata only. Lesson content continues to require enrollment. */
@RestController
@RequestMapping("/api/discovery")
public class DiscoveryController {
  private final CourseService courses;
  private final TeacherProfileService profiles;

  public DiscoveryController(CourseService courses, TeacherProfileService profiles) {
    this.courses = courses;
    this.profiles = profiles;
  }

  @GetMapping("/catalog")
  public ApiResponse<PageResponse<CourseResponse>> catalog(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) String category,
      @RequestParam(defaultValue = "false") boolean freeOnly,
      @RequestParam(defaultValue = "new") String sort,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "9") int size) {
    return ApiResponse.success(
        "OK",
        courses.getCatalog(
            search, null, CourseStatus.PUBLISHED, category, freeOnly, sort, page, size, null));
  }

  @GetMapping("/categories")
  public ApiResponse<List<String>> categories() {
    return ApiResponse.success("OK", courses.getCategories(null, null));
  }

  @GetMapping("/courses/{courseId}")
  public ApiResponse<CourseResponse> detail(@PathVariable Integer courseId) {
    return ApiResponse.success("OK", courses.getPublicCourse(courseId));
  }

  @GetMapping("/teachers/{teacherId}")
  public ApiResponse<TeacherProfileResponse> teacher(@PathVariable Integer teacherId) {
    return ApiResponse.success("OK", profiles.getPublicProfile(teacherId));
  }

  @GetMapping("/teachers/{teacherId}/courses")
  public ApiResponse<PageResponse<CourseResponse>> teacherCourses(
      @PathVariable Integer teacherId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "9") int size) {
    profiles.getPublicProfile(teacherId);
    return ApiResponse.success("OK", courses.getCatalog(
        null, teacherId, CourseStatus.PUBLISHED, null, false, "new", page, size, null));
  }
}
