package com.example.course_management.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "enrollments",
    uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "course_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Enrollment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "enrollment_id")
  private Integer enrollmentId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "student_id", nullable = false)
  private User student;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id", nullable = false)
  private Course course;

  @Column(name = "enrollment_date", nullable = false)
  private LocalDateTime enrollmentDate = LocalDateTime.now();

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private EnrollmentStatus status = EnrollmentStatus.ENROLLED;

  @Column(name = "completion_date")
  private LocalDateTime completionDate;

  @Column(name = "progress_percentage", precision = 5, scale = 2)
  private BigDecimal progressPercentage = BigDecimal.ZERO;
}
