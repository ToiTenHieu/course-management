package com.example.course_management.dto.request;

import jakarta.validation.constraints.Size;

public record AiRecommendationRequest(@Size(max = 500, message = "Mục tiêu tối đa 500 ký tự") String goal) {}
