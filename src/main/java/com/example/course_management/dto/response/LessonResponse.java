package com.example.course_management.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class LessonResponse {
    private Integer lessonId;
    private Integer courseId;
    private String title;
    private String contentUrl;
    private String textContent;
    private Integer orderIndex;
    private Boolean isPublished;
}