package com.example.course_management.dto.request;

import com.example.course_management.entity.Role;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUserRequest {
  @NotBlank(message = "Username không được để trống")
  @Size(min = 3, max = 40, message = "Tên đăng nhập từ 3 đến 40 ký tự")
  @Pattern(regexp = "[a-zA-Z0-9_.-]+", message = "Tên đăng nhập chỉ gồm chữ, số, dấu chấm, gạch dưới hoặc gạch ngang")
  private String username;

  @NotBlank(message = "Password không được để trống")
  @Size(min = 8, max = 64, message = "Password phải từ 8 đến 64 ký tự")
  private String password;

  @NotBlank(message = "Email không được để trống")
  @Email(message = "Email không hợp lệ")
  @Size(max = 100)
  private String email;

  @NotBlank(message = "Họ tên không được để trống")
  @Size(max = 100)
  private String fullName;

  @NotNull(message = "Role không được để trống")
  private Role role;
}
