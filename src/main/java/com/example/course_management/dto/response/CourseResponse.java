package com.example.course_management.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
@Setter
public class CourseResponse {
  private String category;
  private String level;
  private String learningOutcomes;
  private String prerequisites;
  private String targetAudience;
  private Long lessonCount;
  private Long enrollmentCount;
  private Double averageRating;
  private Integer courseId;
  private String title;
  private String description;
  private Integer teacherId;
  private String teacherName;
  private BigDecimal price;
  private Integer durationHours;
  private String status;
  private LocalDateTime createdAt;
  private List<LessonSummaryResponse> lessons;
}
