package com.example.course_management.repository;

import com.example.course_management.entity.Course;
import com.example.course_management.entity.CourseStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, Integer> {

  @Query(
      """
      SELECT c FROM Course c
      WHERE (:status IS NULL OR c.status = :status)
        AND (:teacherId IS NULL OR c.teacher.userId = :teacherId)
        AND (
            :search = ''
            OR LOWER(c.title) LIKE CONCAT('%', LOWER(:search), '%')
            OR LOWER(c.description) LIKE CONCAT('%', LOWER(:search), '%')
        )
      """)
  List<Course> search(
      @Param("status") CourseStatus status,
      @Param("teacherId") Integer teacherId,
      @Param("search") String search);

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT c FROM Course c WHERE c.courseId=:id")
  java.util.Optional<Course> findLockedById(@Param("id") Integer id);

  List<Course> findByTeacher_UserId(Integer teacherId);
}
