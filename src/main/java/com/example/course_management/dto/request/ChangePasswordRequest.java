package com.example.course_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {
  private String oldPassword; // bắt buộc nếu tự đổi mật khẩu của chính mình

  @NotBlank(message = "Mật khẩu mới không được để trống")
  @Size(min = 8, max = 64, message = "Mật khẩu mới phải từ 8 đến 64 ký tự")
  private String newPassword;
}
