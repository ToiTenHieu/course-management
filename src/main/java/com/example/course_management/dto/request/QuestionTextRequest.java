package com.example.course_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QuestionTextRequest(@NotBlank @Size(max = 5000) String body) {}
