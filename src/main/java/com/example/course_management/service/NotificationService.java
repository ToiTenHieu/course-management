package com.example.course_management.service;

import com.example.course_management.dto.request.CreateNotificationRequest;
import com.example.course_management.dto.response.NotificationResponse;
import com.example.course_management.security.CustomUserDetails;
import java.util.List;

public interface NotificationService {
    List<NotificationResponse> getMyNotifications(CustomUserDetails actor);
    NotificationResponse create(CreateNotificationRequest request);
    NotificationResponse markAsRead(Integer notificationId, CustomUserDetails actor);
    void delete(Integer notificationId);
}