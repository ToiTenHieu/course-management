package com.example.course_management.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Builder
public class EnrollmentResponse {
    private Integer enrollmentId;
    private Integer courseId;
    private String courseTitle;
    private LocalDateTime enrollmentDate;
    private String status;
    private BigDecimal progressPercentage;
}