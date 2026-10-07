package com.example.course_management.dto.request;

import jakarta.validation.constraints.*;
import java.util.List;

public record SubmitQuizRequest(
    @NotNull @Min(1) Integer quizVersionId,
    @NotBlank
        @Pattern(
            regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
        String submissionKey,
    @NotNull @Size(min = 1, max = 20) List<@NotNull @Min(0) @Max(3) Integer> answers) {}
