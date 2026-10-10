package com.example.course_management.service.impl;

import com.example.course_management.time.ApplicationTime;
import com.example.course_management.dto.request.CreateNotificationRequest;
import com.example.course_management.dto.response.NotificationResponse;
import com.example.course_management.entity.Notification;
import com.example.course_management.entity.User;
import com.example.course_management.exception.ForbiddenException;
import com.example.course_management.exception.ResourceNotFoundException;
import com.example.course_management.repository.NotificationRepository;
import com.example.course_management.repository.UserRepository;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.ContentPolicy;
import com.example.course_management.service.NotificationService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;
  private final ContentPolicy contentPolicy;

  public NotificationServiceImpl(
      NotificationRepository notificationRepository,
      UserRepository userRepository,
      ContentPolicy contentPolicy) {
    this.notificationRepository = notificationRepository;
    this.userRepository = userRepository;
    this.contentPolicy = contentPolicy;
  }

  @Override
  public List<NotificationResponse> getMyNotifications(CustomUserDetails actor) {
    return notificationRepository
        .findByUser_UserIdOrderByCreatedAtDesc(actor.getUser().getUserId())
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Override
  public NotificationResponse create(CreateNotificationRequest req) {
    contentPolicy.targetUrl(req.getTargetUrl());
    User target =
        userRepository
            .findById(req.getUserId())
            .orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy user id=" + req.getUserId()));

    Notification n = new Notification();
    n.setUser(target);
    n.setMessage(req.getMessage());
    n.setType(req.getType());
    n.setTargetUrl(req.getTargetUrl());
    n.setIsRead(false);
    n.setCreatedAt(ApplicationTime.now());
    return toResponse(notificationRepository.save(n));
  }

  @Override
  public NotificationResponse markAsRead(Integer notificationId, CustomUserDetails actor) {
    Notification n = findOrThrow(notificationId);
    if (!n.getUser().getUserId().equals(actor.getUser().getUserId())) {
      throw new ForbiddenException("Đây không phải thông báo của bạn");
    }
    n.setIsRead(true);
    return toResponse(notificationRepository.save(n));
  }

  @Override
  public void delete(Integer notificationId) {
    notificationRepository.delete(findOrThrow(notificationId));
  }

  private Notification findOrThrow(Integer id) {
    return notificationRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo id=" + id));
  }

  private NotificationResponse toResponse(Notification n) {
    return NotificationResponse.builder()
        .notificationId(n.getNotificationId())
        .message(n.getMessage())
        .type(n.getType())
        .targetUrl(n.getTargetUrl())
        .isRead(n.getIsRead())
        .createdAt(n.getCreatedAt())
        .build();
  }
}
