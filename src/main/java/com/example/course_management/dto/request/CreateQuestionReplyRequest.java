package com.example.course_management.dto.request;

import jakarta.validation.constraints.*;

public record CreateQuestionReplyRequest(
    @NotBlank @Size(max = 5000) String body,
    @NotBlank @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}") String clientRequestId) {}
