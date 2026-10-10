package com.example.course_management.entity;

import com.example.course_management.time.ApplicationTime;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "lesson_quiz_versions",
    uniqueConstraints = @UniqueConstraint(columnNames = {"lesson_id", "revision"}))
@Getter
@Setter
public class LessonQuizVersion {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer quizVersionId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lesson_id", nullable = false)
  private Lesson lesson;

  @Column(nullable = false)
  private Integer revision;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false)
  private Integer passPercentage;

  @Column(nullable = false)
  private Boolean isPublished = false;

  @Column(nullable = false)
  private LocalDateTime createdAt = ApplicationTime.now();

  @OneToMany(mappedBy = "quizVersion", cascade = CascadeType.ALL)
  @OrderBy("orderIndex ASC")
  private List<QuizQuestion> questions = new ArrayList<>();
}
