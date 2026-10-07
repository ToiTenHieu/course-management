package com.example.course_management.dto.request;

import jakarta.validation.constraints.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateLessonRequest {
  @NotBlank(message = "Tiêu đề không được để trống")
  @Size(max = 255)
  private String title;

  @Size(max = 500)
  private String contentUrl;

  @Size(max = 100000)
  private String textContent;

  @NotNull(message = "orderIndex không được để trống")
  @Positive
  private Integer orderIndex;
}
