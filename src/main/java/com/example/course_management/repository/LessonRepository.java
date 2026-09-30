package com.example.course_management.repository;

import com.example.course_management.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Integer> {
    List<Lesson> findByCourse_CourseIdAndIsPublishedTrueOrderByOrderIndex(Integer courseId);
    List<Lesson> findByCourse_CourseIdOrderByOrderIndex(Integer courseId);
    long countByCourse_CourseId(Integer courseId);
}