package com.example.course_management.repository;

import com.example.course_management.entity.Payment;
import com.example.course_management.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    List<Payment> findByStudent_UserIdOrderByCreatedAtDesc(Integer studentId);

    @Query("SELECT p FROM Payment p WHERE (:status IS NULL OR p.status = :status) ORDER BY p.createdAt DESC")
    List<Payment> search(@Param("status") PaymentStatus status);

    boolean existsByStudent_UserIdAndCourse_CourseIdAndStatusIn(
            Integer studentId, Integer courseId, List<PaymentStatus> statuses);
}