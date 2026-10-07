package com.example.course_management.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EnrollmentResponse {
  private Integer enrollmentId;
  private Integer courseId;
  private String courseTitle;
  private LocalDateTime enrollmentDate;
  private String status;
  private BigDecimal progressPercentage;
}
