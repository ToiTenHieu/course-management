package com.example.course_management.entity;

import com.example.course_management.time.ApplicationTime;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer userId;

  @Column(unique = true, nullable = false)
  private String username;

  @Column(nullable = false)
  private String passwordHash;

  @Column(unique = true, nullable = false)
  private String email;

  @Column(nullable = false)
  private String fullName;

  @Column(columnDefinition = "TEXT")
  private String biography;

  @Column(length = 2000)
  private String expertise;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role; // ADMIN, TEACHER, STUDENT

  @Column(nullable = false)
  private Boolean isActive = true;

  @Column(nullable = false)
  private long authVersion = 0;

  private LocalDateTime createdAt = ApplicationTime.now();
  private LocalDateTime updatedAt = ApplicationTime.now();
}
