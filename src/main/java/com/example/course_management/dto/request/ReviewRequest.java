package com.example.course_management.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewRequest {
  @NotNull(message = "Điểm đánh giá không được để trống")
  @Min(value = 1, message = "Điểm tối thiểu là 1")
  @Max(value = 5, message = "Điểm tối đa là 5")
  private Integer rating;

  @jakarta.validation.constraints.Size(max = 5000, message = "Nhận xét tối đa 5000 ký tự")
  private String comment;
}
