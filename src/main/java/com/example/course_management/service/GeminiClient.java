package com.example.course_management.service;

import com.example.course_management.config.AiSettings;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Calls Gemini generateContent over HTTPS; the API key is sent as a header and never logged. */
@Component
public class GeminiClient implements AiClient {
  private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);
  private final AiSettings settings;
  private final JsonMapper mapper;
  private final HttpClient http;

  public GeminiClient(AiSettings settings, JsonMapper mapper) {
    this.settings = settings;
    this.mapper = mapper;
    this.http = HttpClient.newBuilder().connectTimeout(settings.timeout()).build();
  }

  @Override
  public String generate(String system, List<Turn> turns, Map<String, Object> jsonSchema) {
    if (!settings.enabled()) throw new AiUnavailableException("Trợ lý AI chưa được bật");
    var config = new LinkedHashMap<String, Object>();
    config.put("temperature", jsonSchema == null ? 0.4 : 0.2);
    config.put("maxOutputTokens", 8192);
    if (jsonSchema != null) {
      config.put("responseMimeType", "application/json");
      config.put("responseSchema", jsonSchema);
    }
    var body = Map.of(
        "systemInstruction", Map.of("parts", List.of(Map.of("text", system))),
        "contents", turns.stream().map(t -> Map.of("role", t.role(), "parts", List.of(Map.of("text", t.text())))).toList(),
        "generationConfig", config);
    HttpResponse<String> response;
    try {
      var request = HttpRequest.newBuilder(URI.create(settings.baseUrl() + "/models/" + settings.model() + ":generateContent"))
          .timeout(settings.timeout())
          .header("Content-Type", "application/json")
          .header("x-goog-api-key", settings.apiKey())
          .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body), StandardCharsets.UTF_8))
          .build();
      response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    } catch (IOException ex) {
      log.warn("Gemini request failed: {}", ex.getClass().getSimpleName());
      throw new AiUnavailableException("Không kết nối được dịch vụ AI. Vui lòng thử lại sau");
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new AiUnavailableException("Yêu cầu AI đã bị hủy");
    }
    if (response.statusCode() == 429)
      throw new AiUnavailableException("AI đang quá tải hoặc đã hết lượt miễn phí. Vui lòng thử lại sau ít phút");
    if (response.statusCode() != 200) {
      log.warn("Gemini returned HTTP {}", response.statusCode());
      throw new AiUnavailableException("Dịch vụ AI tạm thời không khả dụng");
    }
    JsonNode root;
    try {
      root = mapper.readTree(response.body());
    } catch (RuntimeException ex) {
      throw new AiUnavailableException("Dịch vụ AI trả về dữ liệu không hợp lệ");
    }
    if (!root.path("promptFeedback").path("blockReason").isMissingNode())
      throw new AiUnavailableException("AI từ chối xử lý nội dung này");
    var candidate = root.path("candidates").path(0);
    String finish = candidate.path("finishReason").asString("");
    if (Set.of("SAFETY", "PROHIBITED_CONTENT", "BLOCKLIST", "SPII").contains(finish))
      throw new AiUnavailableException("AI từ chối xử lý nội dung này");
    var text = new StringBuilder();
    // Thinking models may return thought summaries as separate parts; only the answer is kept.
    for (var part : candidate.path("content").path("parts"))
      if (!part.path("thought").asBoolean(false)) text.append(part.path("text").asString(""));
    if (text.isEmpty()) throw new AiUnavailableException("AI chưa trả lời. Vui lòng thử lại");
    return text.toString();
  }
}
