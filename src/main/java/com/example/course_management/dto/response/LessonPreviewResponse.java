package com.example.course_management.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class LessonPreviewResponse {
    private Integer lessonId;
    private String title;
    private Integer orderIndex;
    private String preview;
    private Boolean hasVideoOrDocument;
}