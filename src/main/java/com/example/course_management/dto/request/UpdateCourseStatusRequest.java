package com.example.course_management.dto.request;

import com.example.course_management.entity.CourseStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCourseStatusRequest {
  @NotNull(message = "status không được để trống")
  private CourseStatus status;
}
