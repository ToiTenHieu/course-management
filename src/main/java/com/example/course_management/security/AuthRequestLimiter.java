package com.example.course_management.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Bounded per-process limits; keys use the peer address, not untrusted forwarding headers. */
@Component
public class AuthRequestLimiter {
  private record Window(Instant expiresAt, int count) {}
  private final Map<String, Window> windows = new HashMap<>();
  private final Clock clock;

  public AuthRequestLimiter(Clock clock) {
    this.clock = clock;
  }

  public synchronized void check(String operation, String address) {
    Instant now = clock.instant();
    windows.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    String key = operation + ":" + address;
    Window current = windows.get(key);
    int maximum = operation.equals("register") ? 20 : 30;
    Duration duration = Duration.ofMinutes(operation.equals("register") ? 15 : 1);
    if ((current == null && windows.size() >= 10000)
        || (current != null && current.count() >= maximum))
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
          "Bạn đã gửi nhiều yêu cầu. Vui lòng thử lại sau " + duration.toMinutes() + " phút.");
    if (operation.equals("register"))
      windows.put(key, new Window(current == null ? now.plus(duration) : current.expiresAt(),
          current == null ? 1 : current.count() + 1));
  }

  public synchronized void recordLoginFailure(String address) {
    String key = "login:" + address;
    Instant now = clock.instant();
    Window current = windows.get(key);
    if (current != null && !current.expiresAt().isAfter(now)) current = null;
    // A fresh entry can only follow check(), which enforces the bounded map size.
    if (current == null && windows.size() >= 10000) return;
    windows.put(key, new Window(current == null ? now.plusSeconds(60) : current.expiresAt(),
        current == null ? 1 : current.count() + 1));
  }
}
