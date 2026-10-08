package com.example.course_management.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lessons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Lesson {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "lesson_id")
  private Integer lessonId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id", nullable = false)
  private Course course;

  @Column(name = "title", nullable = false, length = 255)
  private String title;

  @Column(name = "content_url", length = 500)
  private String contentUrl;

  @Column(name = "text_content", columnDefinition = "TEXT")
  private String textContent;

  @Column(name = "content_format", nullable = false, length = 20)
  private String contentFormat = "TEXT";

  @Column(name = "content_revision", nullable = false)
  private Integer contentRevision = 0;

  @Column(name = "video_url", length = 500)
  private String videoUrl;

  @Column(name = "order_index", nullable = false)
  private Integer orderIndex;

  @Column(name = "is_published", nullable = false)
  private Boolean isPublished = false;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();
}
