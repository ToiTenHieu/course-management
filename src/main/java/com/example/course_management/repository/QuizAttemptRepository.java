package com.example.course_management.repository;

import com.example.course_management.entity.QuizAttempt;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Integer> {
  Optional<QuizAttempt> findByStudent_UserIdAndSubmissionKey(Integer studentId, String key);

  @EntityGraph(attributePaths = {"quizVersion", "quizVersion.lesson"})
  Page<QuizAttempt> findByStudent_UserIdAndQuizVersion_Lesson_LessonId(
      Integer studentId, Integer lessonId, Pageable pageable);

  @Query(
      "SELECT COUNT(a), COUNT(DISTINCT a.student.userId), SUM(CASE WHEN a.passed = true THEN 1 ELSE"
          + " 0 END), AVG(a.score) FROM QuizAttempt a WHERE a.quizVersion.quizVersionId ="
          + " :versionId")
  List<Object[]> summarize(Integer versionId);

  @Query(
      "SELECT b.quizQuestion.quizQuestionId, COUNT(b), SUM(CASE WHEN b.selectedIndex <>"
          + " b.quizQuestion.correctIndex THEN 1 ELSE 0 END) FROM QuizAnswer b WHERE"
          + " b.attempt.quizVersion.quizVersionId = :versionId GROUP BY"
          + " b.quizQuestion.quizQuestionId")
  List<Object[]> questionStatistics(Integer versionId);
}
