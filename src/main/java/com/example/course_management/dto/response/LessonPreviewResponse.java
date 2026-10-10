package com.example.course_management.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LessonPreviewResponse {
  private Integer lessonId;
  private String title;
  private Integer orderIndex;
  private Integer chapterId;
  private String chapterTitle;
  private String preview;
  private Boolean hasVideoOrDocument;
}
