package com.example.course_management.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReviewResponse {
  private Integer reviewId;
  private Integer courseId;
  private Integer studentId;
  private String studentName;
  private Integer rating;
  private String comment;
  private LocalDateTime createdAt;
}
