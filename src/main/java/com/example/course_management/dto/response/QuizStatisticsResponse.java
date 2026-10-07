package com.example.course_management.dto.response;

import java.util.List;

public record QuizStatisticsResponse(
    Integer quizVersionId,
    int revision,
    long attempts,
    long students,
    long passedAttempts,
    Double averageScore,
    List<Question> questions) {
  public record Question(Integer questionId, String prompt, long answered, long incorrect) {}
}
