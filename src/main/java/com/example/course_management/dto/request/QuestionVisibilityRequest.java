package com.example.course_management.dto.request;

import jakarta.validation.constraints.NotNull;

public record QuestionVisibilityRequest(@NotNull Boolean hidden) {}
