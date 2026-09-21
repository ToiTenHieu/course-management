package com.example.course_management.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UpdateStatusRequest {
    @NotNull(message = "isActive không được để trống")
    private Boolean isActive;
}