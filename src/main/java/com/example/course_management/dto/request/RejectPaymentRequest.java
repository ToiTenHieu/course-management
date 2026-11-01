package com.example.course_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectPaymentRequest(
    @NotBlank(message = "Nhập lý do từ chối, tối đa 1000 ký tự")
    @Size(max = 1000, message = "Nhập lý do từ chối, tối đa 1000 ký tự") String reason) {}
