package com.example.course_management.repository;

import com.example.course_management.entity.Lesson;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonRepository extends JpaRepository<Lesson, Integer> {
  List<Lesson> findByCourse_CourseIdAndIsPublishedTrueOrderByOrderIndex(Integer courseId);

  List<Lesson> findByCourse_CourseIdOrderByOrderIndex(Integer courseId);

  long countByCourse_CourseId(Integer courseId);
}
