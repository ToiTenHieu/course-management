package com.example.course_management.service;

import com.example.course_management.dto.request.ReviewRequest;
import com.example.course_management.dto.response.ReviewResponse;
import com.example.course_management.security.CustomUserDetails;
import java.util.List;

public interface ReviewService {
    List<ReviewResponse> getReviews(Integer courseId);
    ReviewResponse create(Integer courseId, ReviewRequest request, CustomUserDetails actor);
    ReviewResponse update(Integer reviewId, ReviewRequest request, CustomUserDetails actor);
    void delete(Integer reviewId, CustomUserDetails actor);
}