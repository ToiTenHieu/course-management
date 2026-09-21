package com.example.course_management.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter @Builder
public class UserResponse {
    private Integer userId;
    private String username;
    private String email;
    private String fullName;
    private String role;
    private Boolean isActive;
    private LocalDateTime createdAt;
}