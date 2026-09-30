package com.example.course_management.service;

import com.example.course_management.dto.request.*;
import com.example.course_management.dto.response.CourseResponse;
import com.example.course_management.entity.CourseStatus;
import com.example.course_management.security.CustomUserDetails;

import java.util.List;

public interface CourseService {
    List<CourseResponse> getCourses(String search, Integer teacherId, CourseStatus status, CustomUserDetails actor);
    CourseResponse getCourseById(Integer courseId, CustomUserDetails actor);
    CourseResponse createCourse(CreateCourseRequest request);
    CourseResponse updateCourse(Integer courseId, UpdateCourseRequest request);
    CourseResponse updateStatus(Integer courseId, UpdateCourseStatusRequest request);
    void deleteCourse(Integer courseId);
}