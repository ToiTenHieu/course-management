package com.example.course_management.dto.response;

import java.util.List;

public record QuizResponse(
    Integer quizVersionId,
    Integer lessonId,
    int revision,
    String title,
    int passPercentage,
    boolean published,
    List<Question> questions) {
  @com.fasterxml.jackson.annotation.JsonInclude(
      com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
  public record Question(
      Integer questionId,
      String prompt,
      List<String> options,
      Integer correctIndex,
      String explanation) {}
}
