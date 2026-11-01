package com.example.course_management.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
  @NotBlank(message = "Tên đăng nhập không được để trống")
  @Size(min = 3, max = 40, message = "Tên đăng nhập từ 3 đến 40 ký tự")
  @Pattern(
      regexp = "[a-zA-Z0-9_.-]+",
      message = "Tên đăng nhập chỉ gồm chữ, số, dấu chấm, gạch dưới hoặc gạch ngang")
  private String username;

  @NotBlank(message = "Mật khẩu không được để trống")
  @Size(min = 8, max = 64, message = "Mật khẩu phải từ 8 đến 64 ký tự")
  private String password;

  @NotBlank(message = "Email không được để trống")
  @Email(message = "Email không hợp lệ")
  @Size(max = 100, message = "Email tối đa 100 ký tự")
  private String email;

  @NotBlank(message = "Họ tên không được để trống")
  @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
  private String fullName;
}
