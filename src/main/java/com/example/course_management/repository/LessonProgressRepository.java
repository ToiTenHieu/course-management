package com.example.course_management.repository;

import com.example.course_management.entity.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Integer> {
    List<LessonProgress> findByEnrollment_EnrollmentId(Integer enrollmentId);
    Optional<LessonProgress> findByEnrollment_EnrollmentIdAndLesson_LessonId(Integer enrollmentId, Integer lessonId);
}