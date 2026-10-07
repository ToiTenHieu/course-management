package com.example.course_management.repository;

import com.example.course_management.entity.Enrollment;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Integer> {
  List<Enrollment> findByCourse_CourseId(Integer courseId);

  List<Enrollment> findByStudent_UserId(Integer studentId);

  Optional<Enrollment> findByStudent_UserIdAndCourse_CourseId(Integer studentId, Integer courseId);

  boolean existsByStudent_UserIdAndCourse_CourseId(Integer studentId, Integer courseId);

  @Query(
      "SELECT e.course.courseId, e.course.title, COUNT(e) FROM Enrollment e "
          + "GROUP BY e.course.courseId, e.course.title ORDER BY COUNT(e) DESC")
  List<Object[]> findTopCourses(org.springframework.data.domain.Pageable pageable);

  long countByCourse_CourseId(Integer courseId);

  @Query(
      """
      SELECT e.course.courseId AS courseId, COUNT(e) AS total FROM Enrollment e
      WHERE e.course.courseId IN :ids GROUP BY e.course.courseId
      """)
  List<CourseCount> countByCourses(
      @org.springframework.data.repository.query.Param("ids") List<Integer> ids);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT x FROM Enrollment x WHERE x.enrollmentId = :id")
  Optional<Enrollment> findLockedById(
      @org.springframework.data.repository.query.Param("id") Integer id);
}
