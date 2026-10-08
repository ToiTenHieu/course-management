package com.example.course_management.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Course {

  private String category;
  private String level;

  @Column(columnDefinition = "TEXT")
  private String learningOutcomes;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "course_id")
  private Integer courseId;

  @Column(name = "title", nullable = false, length = 255)
  private String title;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "teacher_id", nullable = false)
  private User teacher;

  @Column(name = "price", precision = 10, scale = 2)
  private BigDecimal price = BigDecimal.ZERO;

  @Column(name = "duration_hours")
  private Integer durationHours;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private CourseStatus status = CourseStatus.DRAFT;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();
}
