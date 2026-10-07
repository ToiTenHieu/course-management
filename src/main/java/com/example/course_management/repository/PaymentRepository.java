package com.example.course_management.repository;

import com.example.course_management.entity.Payment;
import com.example.course_management.entity.PaymentStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {
  List<Payment> findByStudent_UserIdOrderByCreatedAtDesc(Integer studentId);

  @Query(
      "SELECT p FROM Payment p WHERE (:status IS NULL OR p.status = :status) ORDER BY p.createdAt"
          + " DESC")
  List<Payment> search(@Param("status") PaymentStatus status);

  boolean existsByStudent_UserIdAndCourse_CourseIdAndStatusIn(
      Integer studentId, Integer courseId, List<PaymentStatus> statuses);

  @Query("SELECT p.student.userId FROM Payment p WHERE p.paymentId = :id")
  Optional<Integer> findStudentIdByPaymentId(@Param("id") Integer id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT x FROM Payment x WHERE x.paymentId = :id")
  Optional<Payment> findLockedById(
      @org.springframework.data.repository.query.Param("id") Integer id);
}
