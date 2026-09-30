package com.example.course_management.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter @Builder
public class ReviewResponse {
    private Integer reviewId;
    private Integer courseId;
    private Integer studentId;
    private String studentName;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}