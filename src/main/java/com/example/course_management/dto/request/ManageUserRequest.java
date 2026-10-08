package com.example.course_management.dto.request;

import com.example.course_management.entity.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ManageUserRequest extends UpdateUserRequest {
  @NotNull(message = "Vui lòng chọn vai trò")
  private Role role;
}
