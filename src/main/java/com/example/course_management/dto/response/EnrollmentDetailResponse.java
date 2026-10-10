package com.example.course_management.dto.response;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EnrollmentDetailResponse {
  private Integer enrollmentId;
  private Integer courseId;
  private String courseTitle;
  private String status;
  private BigDecimal progressPercentage;
  private Integer lastLessonId;
  private List<LessonProgressItem> lessons;

  @Getter
  @Builder
  public static class LessonProgressItem {
    private Integer lessonId;
    private String title;
    private Integer orderIndex;
    private Integer chapterId;
    private String chapterTitle;
    private Boolean isCompleted;
  }
}
