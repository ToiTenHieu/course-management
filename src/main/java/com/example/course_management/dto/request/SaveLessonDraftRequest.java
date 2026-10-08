package com.example.course_management.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaveLessonDraftRequest {
  @NotNull @Min(0) private Integer expectedRevision;
  @Min(0) private Integer baseRevision;
  @Size(max = 255) private String title;
  private Integer orderIndex;
  @Size(max = 500) private String contentUrl;
  @Size(max = 100000) private String textContent;
  @Pattern(regexp = "TEXT|MARKDOWN") private String contentFormat;
  @Size(max = 500) private String videoUrl;
}
