package com.example.course_management.service;

import com.example.course_management.dto.response.EnrollmentDetailResponse;
import com.example.course_management.dto.response.EnrollmentResponse;
import com.example.course_management.security.CustomUserDetails;

import java.util.List;

public interface EnrollmentService {
    List<EnrollmentResponse> getMyEnrollments(CustomUserDetails actor);
    EnrollmentResponse enroll(Integer courseId, CustomUserDetails actor);
    EnrollmentDetailResponse getEnrollmentDetail(Integer enrollmentId, CustomUserDetails actor);
    EnrollmentDetailResponse completeLesson(Integer enrollmentId, Integer lessonId, CustomUserDetails actor);
}