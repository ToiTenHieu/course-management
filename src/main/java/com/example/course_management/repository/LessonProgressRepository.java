package com.example.course_management.repository;

import com.example.course_management.entity.LessonProgress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Integer> {
  void deleteByLesson_LessonId(Integer lessonId);

  List<LessonProgress> findByEnrollment_EnrollmentId(Integer enrollmentId);

  Optional<LessonProgress> findByEnrollment_EnrollmentIdAndLesson_LessonId(
      Integer enrollmentId, Integer lessonId);
}
