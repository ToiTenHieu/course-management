package com.example.course_management.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Builder
public class PaymentResponse {
    private Integer paymentId;
    private Integer courseId;
    private String courseTitle;
    private Integer studentId;
    private String studentName;
    private BigDecimal amount;
    private String status;
    private String transferNote;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
}