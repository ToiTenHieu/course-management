package com.example.course_management.dto.response;

import java.time.LocalDateTime;

public record LessonQuestionResponse(
    Integer questionId,
    Integer lessonId,
    Integer studentId,
    String studentName,
    String body,
    String answer,
    String answeredByName,
    LocalDateTime answeredAt,
    boolean hidden,
    LocalDateTime createdAt) {}
