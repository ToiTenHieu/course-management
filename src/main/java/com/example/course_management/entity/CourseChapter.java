package com.example.course_management.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "course_chapters")
@Getter @Setter @NoArgsConstructor
public class CourseChapter {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "chapter_id") private Integer chapterId;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "course_id", nullable = false)
  private Course course;
  @Column(nullable = false, length = 255) private String title;
  @Column(name = "order_index", nullable = false) private Integer orderIndex;
}
