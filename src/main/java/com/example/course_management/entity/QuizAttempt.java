package com.example.course_management.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "quiz_attempts",
    uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "submission_key"}))
@Getter
@Setter
public class QuizAttempt {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer attemptId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "quiz_version_id", nullable = false)
  private LessonQuizVersion quizVersion;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "student_id", nullable = false)
  private User student;

  @Column(nullable = false, length = 36)
  private String submissionKey;

  @Column(nullable = false)
  private Integer correctCount;

  @Column(nullable = false)
  private Integer questionCount;

  @Column(nullable = false)
  private Integer score;

  @Column(nullable = false)
  private Boolean passed;

  @Column(nullable = false)
  private LocalDateTime submittedAt = LocalDateTime.now();

  @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL)
  private List<QuizAnswer> answers = new ArrayList<>();
}
