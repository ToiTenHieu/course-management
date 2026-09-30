package com.example.course_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UpdateLessonRequest {
    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    private String contentUrl;
    private String textContent;

    @NotNull(message = "orderIndex không được để trống")
    private Integer orderIndex;
}