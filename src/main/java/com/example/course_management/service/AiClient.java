package com.example.course_management.service;

import java.util.List;
import java.util.Map;

/** Text generation port; the Gemini implementation is replaced by a mock in tests. */
public interface AiClient {
  /** One conversation turn; role is "user" or "model". */
  record Turn(String role, String text) {}

  /** Thrown when the provider fails, times out, rate-limits or blocks the request. */
  class AiUnavailableException extends RuntimeException {
    public AiUnavailableException(String message) { super(message); }
  }

  /**
   * Returns the model's text. With a non-null schema the provider is asked for JSON matching it,
   * but callers must still validate the result.
   */
  String generate(String system, List<Turn> turns, Map<String, Object> jsonSchema);
}
