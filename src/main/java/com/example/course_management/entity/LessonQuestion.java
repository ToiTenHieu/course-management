package com.example.course_management.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "lesson_questions")
@Getter
@Setter
public class LessonQuestion {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer questionId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lesson_id", nullable = false)
  private Lesson lesson;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "student_id", nullable = false)
  private User student;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String body;

  @Column(columnDefinition = "TEXT")
  private String answer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "answered_by")
  private User answeredBy;

  private LocalDateTime answeredAt;

  @Column(nullable = false)
  private Boolean isHidden = false;

  @Column(nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();
}
