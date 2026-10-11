package com.example.course_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectPaymentRequest(@NotBlank @Size(max=1000) String reason) {}
