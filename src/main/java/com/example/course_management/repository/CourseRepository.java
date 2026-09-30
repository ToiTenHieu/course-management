package com.example.course_management.repository;

import com.example.course_management.entity.Course;
import com.example.course_management.entity.CourseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Integer> {

    @Query("""
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
            @Param("search") String search
    );
    List<Course> findByTeacher_UserId(Integer teacherId);
}