package com.example.course_management.controller;

import com.example.course_management.dto.response.ApiResponse;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.WeeklyGoalService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/learning-goal") @PreAuthorize("hasRole('STUDENT')")
public class WeeklyGoalController {
  private final WeeklyGoalService service;
  public WeeklyGoalController(WeeklyGoalService service) {this.service=service;}
  @GetMapping public ApiResponse<WeeklyGoalService.Goal> get(@AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("OK",service.get(actor));
  }
  @PutMapping public ApiResponse<WeeklyGoalService.Goal> save(@Valid @RequestBody WeeklyGoalService.Save request,@AuthenticationPrincipal CustomUserDetails actor) {
    return ApiResponse.success("Đã lưu mục tiêu tuần",service.save(request,actor));
  }
}
