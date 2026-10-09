package com.example.course_management.dto.request;

import jakarta.validation.constraints.*;

public record SaveSettingsRequest(
    @NotNull @Min(0) Integer revision,
    @NotNull @Min(1) Long maxFileBytes,
    @NotNull @Min(1) Integer maxResourcesPerLesson,
    @NotNull @Min(1) Integer maxQuizQuestions,
    @NotNull @Min(1) @Max(100) Integer defaultPassPercentage,
    @NotBlank @Size(max = 100) String defaultCategory,
    @NotBlank @Size(max = 100) String defaultLevel,
    @NotBlank @Size(max = 255) String bankName,
    @NotBlank @Size(max = 255) String bankAccount,
    @NotBlank @Size(max = 255) String bankHolder,
    @Pattern(regexp = "[0-9]{6}|", message = "Mã BIN ngân hàng phải gồm 6 chữ số hoặc để trống") String bankBin) {}
