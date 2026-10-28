package com.example.course_management.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record AiTutorRequest(
    @NotBlank @Size(max = 2000) String question,
    @Size(max = 20) List<@NotNull @Valid Turn> history) {
  /** Only the latest turns are sent to the model. */
  public static final int MAX_HISTORY = 6;
  public record Turn(@NotNull Boolean fromAssistant, @NotBlank @Size(max = 4000) String text) {}
}
