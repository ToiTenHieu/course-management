package com.example.course_management.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter @Builder
public class EnrollmentDetailResponse {
    private Integer enrollmentId;
    private Integer courseId;
    private String courseTitle;
    private String status;
    private BigDecimal progressPercentage;
    private List<LessonProgressItem> lessons;

    @Getter @Builder
    public static class LessonProgressItem {
        private Integer lessonId;
        private String title;
        private Integer orderIndex;
        private Boolean isCompleted;
    }
}