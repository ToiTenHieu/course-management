package com.example.course_management.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class TopCourseResponse {
    private Integer courseId;
    private String title;
    private Long enrollmentCount;
}