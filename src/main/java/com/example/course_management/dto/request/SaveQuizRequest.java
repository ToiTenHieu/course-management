package com.example.course_management.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record SaveQuizRequest(
    @NotNull @Min(0) Integer expectedRevision,
    @NotBlank @Size(max = 255) String title,
    @NotNull @Min(1) @Max(100) Integer passPercentage,
    @NotNull Boolean published,
    @NotNull @Size(min = 1) List<@NotNull @Valid Question> questions) {
  // The database stores option_a through option_d; changing this requires a schema change.
  public static final int OPTION_COUNT = 4;
  public record Question(
      @NotBlank @Size(max = 5000) String prompt,
      @NotNull @Size(min = OPTION_COUNT, max = OPTION_COUNT) List<@NotBlank @Size(max = 1000) String> options,
      @NotNull @Min(0) @Max(OPTION_COUNT - 1) Integer correctIndex,
      @NotBlank @Size(max = 5000) String explanation) {}
}
