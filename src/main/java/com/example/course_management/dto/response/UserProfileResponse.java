package com.example.course_management.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserProfileResponse {
  private Integer userId;
  private String username;
  private String email;
  private String fullName;
  private String role;
  private Boolean isActive;
}
