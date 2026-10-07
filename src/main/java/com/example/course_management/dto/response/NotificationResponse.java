package com.example.course_management.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationResponse {
  private Integer notificationId;
  private String message;
  private String type;
  private String targetUrl;
  private Boolean isRead;
  private LocalDateTime createdAt;
}
