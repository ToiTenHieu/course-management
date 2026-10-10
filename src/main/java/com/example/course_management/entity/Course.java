package com.example.course_management.entity;

import com.example.course_management.time.ApplicationTime;
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

  @Column(name = "curriculum_revision", nullable = false)
  private Integer curriculumRevision = 0;

  private String category;
  private String level;

  @Column(columnDefinition = "TEXT")
  private String learningOutcomes;

  @Column(columnDefinition = "TEXT")
  private String prerequisites;

  @Column(columnDefinition = "TEXT")
  private String targetAudience;

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
  private LocalDateTime createdAt = ApplicationTime.now();

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = ApplicationTime.now();
}
