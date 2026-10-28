package com.example.course_management.dto.request;

import jakarta.validation.constraints.*;

public record AiQuizDraftRequest(
    @NotNull(message = "Hãy chọn số câu AI soạn")
    @Min(value = 1, message = "Số câu cần từ 1 đến 10")
    @Max(value = 10, message = "Số câu cần từ 1 đến 10") Integer count) {}
