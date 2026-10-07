package com.example.course_management.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "quiz_answers")
@Getter
@Setter
public class QuizAnswer {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer answerId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "attempt_id", nullable = false)
  private QuizAttempt attempt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "quiz_question_id", nullable = false)
  private QuizQuestion quizQuestion;

  @Column(nullable = false)
  private Integer selectedIndex;
}
