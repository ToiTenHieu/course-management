package com.example.course_management.repository;

import com.example.course_management.entity.Review;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository
    extends JpaRepository<Review, Integer>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<Review> {
  @Override
  @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"student", "course"})
  org.springframework.data.domain.Page<Review> findAll(
      org.springframework.data.jpa.domain.Specification<Review> filter,
      org.springframework.data.domain.Pageable pageable);

  java.util.Optional<Review> findByCourse_CourseIdAndStudent_UserId(
      Integer courseId, Integer studentId);

  List<Review> findByCourse_CourseIdOrderByCreatedAtDesc(Integer courseId);

  boolean existsByCourse_CourseIdAndStudent_UserId(Integer courseId, Integer studentId);

  @Query("SELECT AVG(r.rating) FROM Review r WHERE r.course.courseId = :courseId")
  Double averageRating(@Param("courseId") Integer courseId);

  @Query(
      """
      SELECT r.course.courseId AS courseId, AVG(r.rating) AS averageRating FROM Review r
      WHERE r.course.courseId IN :ids GROUP BY r.course.courseId
      """)
  List<CourseRating> averageRatingByCourses(@Param("ids") List<Integer> ids);
}
