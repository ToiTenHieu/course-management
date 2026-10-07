package com.example.course_management.service;

import com.example.course_management.dto.request.*;
import com.example.course_management.dto.response.CourseResponse;
import com.example.course_management.dto.response.PageResponse;
import com.example.course_management.entity.CourseStatus;
import com.example.course_management.security.CustomUserDetails;
import java.util.List;

public interface CourseService {
  PageResponse<CourseResponse> getCatalog(
      String search,
      Integer teacherId,
      CourseStatus status,
      String category,
      boolean freeOnly,
      String sort,
      int page,
      int size,
      CustomUserDetails actor);

  List<String> getCategories(Integer teacherId, CustomUserDetails actor);

  List<CourseResponse> getCourses(
      String search, Integer teacherId, CourseStatus status, CustomUserDetails actor);

  CourseResponse getCourseById(Integer courseId, CustomUserDetails actor);

  CourseResponse getPublicCourse(Integer courseId);

  CourseResponse createCourse(CreateCourseRequest request);

  CourseResponse updateCourse(
      Integer courseId, UpdateCourseRequest request, CustomUserDetails actor);

  CourseResponse updateStatus(Integer courseId, UpdateCourseStatusRequest request);

  void deleteCourse(Integer courseId);
}
