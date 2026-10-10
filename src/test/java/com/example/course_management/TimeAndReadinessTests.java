package com.example.course_management;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.course_management.config.ApplicationReadiness;
import com.example.course_management.controller.ReadinessController;
import com.example.course_management.entity.Role;
import com.example.course_management.entity.User;
import com.example.course_management.repository.UserRepository;
import com.example.course_management.security.AuthRequestLimiter;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.WeeklyGoalService;
import com.example.course_management.time.ApplicationTime;
import java.time.*;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

class TimeAndReadinessTests {
  @Test
  void readinessRejectsRequestsUntilApplicationReadyEvent() {
    var state = new ApplicationReadiness();
    var controller = new ReadinessController(state);
    assertEquals(503, controller.ready().getStatusCode().value());
    assertEquals(Boolean.FALSE, controller.ready().getBody().getData().get("ready"));
    state.markReady();
    assertEquals(200, controller.ready().getStatusCode().value());
    assertEquals(Boolean.TRUE, controller.ready().getBody().getData().get("ready"));
  }

  @Test
  void vietnamWeekChangesAtMondayMidnightEvenWithUtcClock() {
    var account = new User(); account.setUserId(1); account.setRole(Role.STUDENT);
    var actor = new CustomUserDetails(account);
    var jdbc = mock(JdbcTemplate.class);
    var sunday = Clock.fixed(Instant.parse("2026-10-11T16:59:59Z"), ZoneOffset.UTC);
    var monday = Clock.fixed(Instant.parse("2026-10-11T17:00:00Z"), ZoneOffset.UTC);
    var previous = new WeeklyGoalService(jdbc, mock(UserRepository.class), sunday);
    var next = new WeeklyGoalService(jdbc, mock(UserRepository.class), monday);
    assertEquals(LocalDate.of(2026, 10, 5), previous.get(actor).weekStart());
    assertEquals(LocalDate.of(2026, 10, 12), next.get(actor).weekStart());
    next.recordCompletion(1, 2);
    verify(jdbc).update(anyString(), eq(1), eq(2), eq(LocalDateTime.of(2026,10,12,0,0)), eq(1), eq(2));
  }

  @Test
  void entityTimesDoNotDependOnJvmTimezone() {
    TimeZone original = TimeZone.getDefault();
    try {
      TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
      var before = ApplicationTime.now();
      var account = new User();
      var after = ApplicationTime.now();
      assertFalse(account.getCreatedAt().isBefore(before));
      assertFalse(account.getCreatedAt().isAfter(after));
      assertEquals(LocalDateTime.of(2026,10,12,0,0),
          ApplicationTime.now(Clock.fixed(Instant.parse("2026-10-11T17:00:00Z"), ZoneOffset.UTC)));
    } finally {
      TimeZone.setDefault(original);
    }
  }

  @Test
  void authLimitsAreIndependentAndExpireWithoutExtendingTheWindow() {
    Clock clock = mock(Clock.class);
    Instant start = Instant.parse("2026-10-10T00:00:00Z");
    when(clock.instant()).thenReturn(start);
    var limiter = new AuthRequestLimiter(clock);
    // Successful logins are not counted; repeated legitimate traffic remains possible.
    for (int i=0;i<100;i++) limiter.check("login", "peer");
    for (int i=0;i<30;i++) { limiter.check("login", "peer"); limiter.recordLoginFailure("peer"); }
    assertEquals(429, assertThrows(ResponseStatusException.class,
        () -> limiter.check("login", "peer")).getStatusCode().value());
    limiter.check("login", "another-peer");
    for (int i=0;i<20;i++) limiter.check("register", "peer");
    assertThrows(ResponseStatusException.class, () -> limiter.check("register", "peer"));
    when(clock.instant()).thenReturn(start.plusSeconds(60));
    limiter.check("login", "peer");
    assertThrows(ResponseStatusException.class, () -> limiter.check("register", "peer"));
    when(clock.instant()).thenReturn(start.plusSeconds(900));
    limiter.check("register", "peer");
  }
}
