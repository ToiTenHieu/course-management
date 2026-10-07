package com.example.course_management.service;

import com.example.course_management.dto.request.*;
import com.example.course_management.dto.response.LessonPreviewResponse;
import com.example.course_management.dto.response.LessonResponse;
import com.example.course_management.security.CustomUserDetails;
import java.util.List;

public interface LessonService {
  List<LessonResponse> getLessonsByCourse(Integer courseId, CustomUserDetails actor);

  LessonResponse getLessonById(Integer lessonId, CustomUserDetails actor);

  LessonResponse createLesson(
      Integer courseId, CreateLessonRequest request, CustomUserDetails actor);

  LessonResponse updateLesson(
      Integer lessonId, UpdateLessonRequest request, CustomUserDetails actor);

  LessonResponse updatePublishStatus(
      Integer lessonId, UpdateLessonPublishRequest request, CustomUserDetails actor);

  void deleteLesson(Integer lessonId, CustomUserDetails actor);

  LessonPreviewResponse getContentPreview(Integer lessonId, CustomUserDetails actor);
}
