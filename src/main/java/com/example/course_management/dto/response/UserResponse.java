package com.example.course_management.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {
  private Integer userId;
  private String username;
  private String email;
  private String fullName;
  private String role;
  private Boolean isActive;
  private LocalDateTime createdAt;
}
