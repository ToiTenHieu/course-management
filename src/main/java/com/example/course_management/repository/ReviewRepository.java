package com.example.course_management.repository;

import com.example.course_management.entity.Review;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Integer> {
  List<Review> findByCourse_CourseIdOrderByCreatedAtDesc(Integer courseId);

  boolean existsByCourse_CourseIdAndStudent_UserId(Integer courseId, Integer studentId);

  @Query("SELECT AVG(r.rating) FROM Review r WHERE r.course.courseId = :courseId")
  Double averageRating(@Param("courseId") Integer courseId);
}
