package com.example.course_management.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaveTeacherProfileRequest {
  @Size(max = 10000, message = "Giới thiệu tối đa 10000 ký tự")
  private String biography;

  @Size(max = 2000, message = "Chuyên môn tối đa 2000 ký tự")
  private String expertise;
}
