package com.example.course_management.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
  @NotBlank
  @Size(min = 3, max = 40)
  @Pattern(
      regexp = "[a-zA-Z0-9_.-]+",
      message = "Tên đăng nhập chỉ gồm chữ, số, dấu chấm, gạch dưới hoặc gạch ngang")
  private String username;

  @NotBlank
  @Size(min = 8, max = 64)
  private String password;

  @NotBlank
  @Email
  @Size(max = 100, message = "Email tối đa 100 ký tự")
  private String email;

  @NotBlank
  @Size(max = 100)
  private String fullName;
}
