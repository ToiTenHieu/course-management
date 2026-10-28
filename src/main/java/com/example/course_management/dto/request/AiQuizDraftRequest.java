package com.example.course_management.dto.request;

import jakarta.validation.constraints.*;

public record AiQuizDraftRequest(@NotNull @Min(1) @Max(10) Integer count) {}
