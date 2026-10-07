package com.example.course_management.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record SaveQuizRequest(
    @NotNull @Min(0) Integer expectedRevision,
    @NotBlank @Size(max = 255) String title,
    @NotNull @Min(1) @Max(100) Integer passPercentage,
    @NotNull Boolean published,
    @NotNull @Size(min = 1, max = 20) List<@NotNull @Valid Question> questions) {
  public record Question(
      @NotBlank @Size(max = 5000) String prompt,
      @NotNull @Size(min = 4, max = 4) List<@NotBlank @Size(max = 1000) String> options,
      @NotNull @Min(0) @Max(3) Integer correctIndex,
      @NotBlank @Size(max = 5000) String explanation) {}
}
