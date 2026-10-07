package com.example.course_management.repository;

import com.example.course_management.entity.LessonQuizVersion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonQuizRepository extends JpaRepository<LessonQuizVersion, Integer> {
  Optional<LessonQuizVersion> findFirstByLesson_LessonIdOrderByRevisionDesc(Integer lessonId);
}
