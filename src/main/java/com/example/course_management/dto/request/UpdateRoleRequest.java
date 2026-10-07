package com.example.course_management.dto.request;

import com.example.course_management.entity.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRoleRequest {
  @NotNull(message = "Role không được để trống")
  private Role role;
}
