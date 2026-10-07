package com.example.course_management.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateLessonPublishRequest {
  @NotNull(message = "isPublished không được để trống")
  private Boolean isPublished;
}
