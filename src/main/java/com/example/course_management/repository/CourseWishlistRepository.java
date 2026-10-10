package com.example.course_management.repository;

import com.example.course_management.entity.CourseStatus;
import com.example.course_management.entity.CourseWishlist;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface CourseWishlistRepository extends JpaRepository<CourseWishlist, Integer> {
  boolean existsByStudent_UserIdAndCourse_CourseId(Integer studentId, Integer courseId);

  @Modifying
  @Query("DELETE FROM CourseWishlist w WHERE w.student.userId = :studentId AND w.course.courseId = :courseId")
  void remove(@Param("studentId") Integer studentId, @Param("courseId") Integer courseId);

  @Query("""
      SELECT w.course.courseId FROM CourseWishlist w
      WHERE w.student.userId = :studentId AND w.course.courseId IN :ids AND w.course.status = :status
      """)
  List<Integer> savedIds(@Param("studentId") Integer studentId, @Param("ids") List<Integer> ids,
      @Param("status") CourseStatus status);

  @EntityGraph(attributePaths = {"course", "course.teacher"})
  @Query("""
      SELECT w FROM CourseWishlist w WHERE w.student.userId = :studentId AND w.course.status = :status
      AND (:search = '' OR LOWER(w.course.title) LIKE :search ESCAPE '\\'
        OR LOWER(w.course.description) LIKE :search ESCAPE '\\'
        OR LOWER(w.course.teacher.fullName) LIKE :search ESCAPE '\\')
      ORDER BY w.savedAt DESC, w.wishlistId DESC
      """)
  Page<CourseWishlist> findVisible(@Param("studentId") Integer studentId, @Param("status") CourseStatus status,
      @Param("search") String search, Pageable pageable);
}
