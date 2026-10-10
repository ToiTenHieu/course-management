package com.example.course_management.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LessonResponse {
  private Integer lessonId;
  private Integer courseId;
  private String title;
  private String contentUrl;
  private String textContent;
  private String contentFormat;
  private String videoUrl;
  private Integer orderIndex;
    private Integer chapterId;
    private String chapterTitle;
  private Boolean isPublished;
  private Integer contentRevision;
}
