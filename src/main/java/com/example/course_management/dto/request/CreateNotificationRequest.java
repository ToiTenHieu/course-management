package com.example.course_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateNotificationRequest {
  @NotNull(message = "userId không được để trống")
  private Integer userId;

  @NotBlank(message = "Nội dung không được để trống")
  private String message;

  private String type;
  private String targetUrl;
}
