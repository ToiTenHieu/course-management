package com.example.course_management.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SaveLessonNoteRequest(@NotNull @Size(max = 10000) String note) {}
