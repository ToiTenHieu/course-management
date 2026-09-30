package com.example.course_management.repository;

import com.example.course_management.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Integer> {
    List<Enrollment> findByStudent_UserId(Integer studentId);
    Optional<Enrollment> findByStudent_UserIdAndCourse_CourseId(Integer studentId, Integer courseId);
    boolean existsByStudent_UserIdAndCourse_CourseId(Integer studentId, Integer courseId);
    @Query("SELECT e.course.courseId, e.course.title, COUNT(e) FROM Enrollment e " +
            "GROUP BY e.course.courseId, e.course.title ORDER BY COUNT(e) DESC")
    List<Object[]> findTopCourses(org.springframework.data.domain.Pageable pageable);

    long countByCourse_CourseId(Integer courseId);
}