package com.example.course_management.repository;

import com.example.course_management.entity.CourseChapter;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseChapterRepository extends JpaRepository<CourseChapter, Integer> {
  List<CourseChapter> findByCourse_CourseIdOrderByOrderIndexAscChapterIdAsc(Integer courseId);
}
