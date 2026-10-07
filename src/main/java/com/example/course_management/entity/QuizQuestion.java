package com.example.course_management.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "quiz_questions")
@Getter
@Setter
public class QuizQuestion {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer quizQuestionId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "quiz_version_id", nullable = false)
  private LessonQuizVersion quizVersion;

  @Column(nullable = false)
  private Integer orderIndex;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String prompt;

  @Column(name = "option_a", nullable = false, length = 1000)
  private String optionA;

  @Column(name = "option_b", nullable = false, length = 1000)
  private String optionB;

  @Column(name = "option_c", nullable = false, length = 1000)
  private String optionC;

  @Column(name = "option_d", nullable = false, length = 1000)
  private String optionD;

  @Column(nullable = false)
  private Integer correctIndex;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String explanation;
}
