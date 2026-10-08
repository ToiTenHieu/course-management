package com.example.course_management.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LearningCourseResponse(
    Integer enrollmentId,
    Integer courseId,
    String courseTitle,
    String status,
    BigDecimal progressPercentage,
    LocalDateTime enrollmentDate,
    CourseResponse course) {}
