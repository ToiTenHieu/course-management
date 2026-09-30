package com.example.course_management.controller;

import com.example.course_management.dto.request.CreateNotificationRequest;
import com.example.course_management.dto.response.ApiResponse;
import com.example.course_management.dto.response.NotificationResponse;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // Endpoint 33 — AUTH
    @GetMapping
    public ApiResponse<List<NotificationResponse>> getMine(@AuthenticationPrincipal CustomUserDetails actor) {
        return ApiResponse.success("OK", notificationService.getMyNotifications(actor));
    }

    // Endpoint 34 — AUTH (chỉ chủ thông báo, kiểm tra trong Service)
    @PutMapping("/{notificationId}/read")
    public ApiResponse<NotificationResponse> markRead(
            @PathVariable Integer notificationId,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ApiResponse.success("Đã đánh dấu đã đọc", notificationService.markAsRead(notificationId, actor));
    }

    // Endpoint 35 — ADMIN
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<NotificationResponse> create(@Valid @RequestBody CreateNotificationRequest request) {
        return ApiResponse.success("Tạo thông báo thành công", notificationService.create(request));
    }

    // Endpoint 36 — ADMIN
    @DeleteMapping("/{notificationId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Integer notificationId) {
        notificationService.delete(notificationId);
        return ApiResponse.success("Xóa thông báo thành công", null);
    }
}