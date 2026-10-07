package com.example.course_management.repository;

import com.example.course_management.entity.Lesson;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonRepository extends JpaRepository<Lesson, Integer> {
  List<Lesson> findByCourse_CourseIdAndIsPublishedTrueOrderByOrderIndex(Integer courseId);

  List<Lesson> findByCourse_CourseIdOrderByOrderIndex(Integer courseId);

  long countByCourse_CourseId(Integer courseId);

  @Query(
      """
      SELECT l.course.courseId AS courseId, COUNT(l) AS total FROM Lesson l
      WHERE l.course.courseId IN :ids AND l.isPublished = true
      GROUP BY l.course.courseId
      """)
  List<CourseCount> countPublishedByCourses(@Param("ids") List<Integer> ids);
}
