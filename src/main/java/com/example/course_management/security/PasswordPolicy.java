package com.example.course_management.security;

import com.example.course_management.exception.BadRequestException;
import java.nio.charset.StandardCharsets;

public final class PasswordPolicy {
  public static final String MESSAGE =
      "Mật khẩu phải từ 8 đến 64 ký tự và tối đa 72 byte UTF-8";

  private PasswordPolicy() {}

  public static boolean fitsBcrypt(String password) {
    return password != null && password.getBytes(StandardCharsets.UTF_8).length <= 72;
  }

  public static void validateNewPassword(String password) {
    if (password == null || password.isBlank() || password.length() < 8
        || password.length() > 64 || !fitsBcrypt(password)) {
      throw new BadRequestException(MESSAGE);
    }
  }
}
