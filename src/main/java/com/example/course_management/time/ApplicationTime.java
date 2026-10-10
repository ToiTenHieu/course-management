package com.example.course_management.time;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Existing TIMESTAMP columns contain Vietnam wall time, never the host's local time. */
public final class ApplicationTime {
  public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

  private ApplicationTime() {}

  public static LocalDateTime now() {
    return now(Clock.system(ZONE));
  }

  public static LocalDateTime now(Clock clock) {
    return LocalDateTime.now(clock.withZone(ZONE));
  }

  public static LocalDate today(Clock clock) {
    return LocalDate.now(clock.withZone(ZONE));
  }
}
