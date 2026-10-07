package com.example.course_management.dto.request;

import jakarta.validation.constraints.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCourseRequest {
  @NotBlank(message = "Tiêu đề không được để trống")
  @Size(max = 255)
  private String title;

  @Size(max = 10000)
  private String description;

  @NotNull(message = "teacherId không được để trống")
  private Integer teacherId;

  @DecimalMin("0.00")
  @Digits(integer = 8, fraction = 2)
  private BigDecimal price;

  @Positive
  @Max(10000)
  private Integer durationHours;

  @Size(max = 100)
  private String category;

  @Size(max = 100)
  private String level;

  @Size(max = 10000)
  private String learningOutcomes;
}
