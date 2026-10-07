package com.example.course_management.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record QuizAttemptResponse(Summary summary, List<Feedback> feedback) {
  public record Summary(
      Integer attemptId,
      Integer lessonId,
      Integer quizVersionId,
      int revision,
      String title,
      int correctCount,
      int questionCount,
      int score,
      int passPercentage,
      boolean passed,
      LocalDateTime submittedAt) {}

  public record Feedback(
      String prompt,
      List<String> options,
      int selectedIndex,
      int correctIndex,
      boolean correct,
      String explanation) {}
}
