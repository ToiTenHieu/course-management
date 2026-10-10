package com.example.course_management.entity;

import com.example.course_management.time.ApplicationTime;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "question_replies")
@Getter
@Setter
public class QuestionReply {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer replyId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "question_id", nullable = false)
  private LessonQuestion question;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "author_id", nullable = false)
  private User author;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Role authorRole;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String body;

  @Column(nullable = false)
  private Boolean isHidden = false;

  @Column(length = 36)
  private String clientRequestId;

  @Column(nullable = false)
  private LocalDateTime createdAt = ApplicationTime.now();
}
