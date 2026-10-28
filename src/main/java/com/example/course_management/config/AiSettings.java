package com.example.course_management.config;

import java.net.URI;
import java.time.Duration;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** AI features are off unless AI_PROVIDER=gemini and a key is supplied through the environment. */
@Component
public class AiSettings {
  private final String provider, apiKey, model, baseUrl;
  private final Duration timeout;
  private final int requestsPerMinute;

  public AiSettings(@Value("${app.ai.provider:disabled}") String provider,
      @Value("${app.ai.gemini.api-key:}") String apiKey,
      @Value("${app.ai.gemini.model:gemini-3.5-flash-lite}") String model,
      @Value("${app.ai.gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
      @Value("${app.ai.timeout-seconds:30}") int timeoutSeconds,
      @Value("${app.ai.requests-per-minute:10}") int requestsPerMinute) {
    if (!Set.of("disabled", "gemini").contains(provider))
      throw new IllegalArgumentException("AI_PROVIDER phải là disabled hoặc gemini");
    if (provider.equals("gemini") && apiKey.isBlank())
      throw new IllegalArgumentException("AI_PROVIDER=gemini cần biến GEMINI_API_KEY");
    if (!model.matches("[a-zA-Z0-9._-]+"))
      throw new IllegalArgumentException("GEMINI_MODEL không hợp lệ");
    var uri = URI.create(baseUrl);
    if (!"https".equals(uri.getScheme()) || uri.getHost() == null)
      throw new IllegalArgumentException("Địa chỉ Gemini API phải dùng HTTPS");
    if (timeoutSeconds < 1 || timeoutSeconds > 120 || requestsPerMinute < 1 || requestsPerMinute > 120)
      throw new IllegalArgumentException("Thời gian chờ AI cần 1–120 giây, giới hạn 1–120 lượt mỗi phút");
    this.provider = provider;
    this.apiKey = apiKey.strip();
    this.model = model;
    this.baseUrl = baseUrl.replaceAll("/$", "");
    this.timeout = Duration.ofSeconds(timeoutSeconds);
    this.requestsPerMinute = requestsPerMinute;
  }

  public boolean enabled() { return !provider.equals("disabled"); }
  public String apiKey() { return apiKey; }
  public String model() { return model; }
  public String baseUrl() { return baseUrl; }
  public Duration timeout() { return timeout; }
  public int requestsPerMinute() { return requestsPerMinute; }
}
