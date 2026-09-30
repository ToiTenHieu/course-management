package com.example.course_management.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateEnrollmentRequest {
    @NotNull(message = "courseId không được để trống")
    private Integer courseId;
}