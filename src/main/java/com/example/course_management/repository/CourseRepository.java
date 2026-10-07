package com.example.course_management.repository;

import com.example.course_management.entity.Course;
import com.example.course_management.entity.CourseStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository
    extends JpaRepository<Course, Integer>, JpaSpecificationExecutor<Course> {

  @Override
  @EntityGraph(attributePaths = "teacher")
  Page<Course> findAll(Specification<Course> specification, Pageable pageable);

  @Override
  @EntityGraph(attributePaths = "teacher")
  List<Course> findAll(Specification<Course> specification, Sort sort);

  @Query(
      """
      SELECT DISTINCT c.category FROM Course c
      WHERE (:admin = true OR c.status = :published OR c.teacher.userId = :managedTeacherId)
        AND (:teacherId IS NULL OR c.teacher.userId = :teacherId)
      ORDER BY c.category
      """)
  List<String> findVisibleCategories(
      @Param("admin") boolean admin,
      @Param("published") CourseStatus published,
      @Param("managedTeacherId") Integer managedTeacherId,
      @Param("teacherId") Integer teacherId);

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT c FROM Course c WHERE c.courseId=:id")
  java.util.Optional<Course> findLockedById(@Param("id") Integer id);

  List<Course> findByTeacher_UserId(Integer teacherId);
}
