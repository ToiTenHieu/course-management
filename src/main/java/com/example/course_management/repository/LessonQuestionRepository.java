package com.example.course_management.repository;

import com.example.course_management.entity.LessonQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;

public interface LessonQuestionRepository extends JpaRepository<LessonQuestion, Integer> {
  @EntityGraph(attributePaths = {"student", "answeredBy"})
  @Query(
      "SELECT q FROM LessonQuestion q WHERE q.lesson.lessonId = :lessonId AND (:manager = true OR"
          + " q.isHidden = false)")
  Page<LessonQuestion> findVisible(Integer lessonId, boolean manager, Pageable pageable);

  @Query("SELECT q.lesson.course.courseId FROM LessonQuestion q WHERE q.questionId = :id")
  java.util.Optional<Integer> findCourseId(Integer id);
}
