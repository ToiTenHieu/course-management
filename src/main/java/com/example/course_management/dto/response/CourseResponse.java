package com.example.course_management.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Builder @Setter
public class CourseResponse {
    private Integer courseId;
    private String title;
    private String description;
    private Integer teacherId;
    private String teacherName;
    private BigDecimal price;
    private Integer durationHours;
    private String status;
    private LocalDateTime createdAt;
    private List<LessonSummaryResponse> lessons;
}