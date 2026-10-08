package com.example.course_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateNotificationRequest {
  @NotNull(message = "userId không được để trống")
  private Integer userId;

  @NotBlank(message = "Nội dung không được để trống")
  @Size(max = 5000, message = "Thông báo tối đa 5000 ký tự")
  private String message;

  @Size(max = 50, message = "Loại thông báo tối đa 50 ký tự")
  private String type;
  @Size(max = 500, message = "Liên kết thông báo tối đa 500 ký tự")
  private String targetUrl;
}
