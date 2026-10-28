package com.example.course_management.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record AiTutorRequest(
    @NotBlank(message = "Hãy nhập câu hỏi cho trợ giảng AI")
    @Size(max = 2000, message = "Câu hỏi tối đa 2000 ký tự") String question,
    @Size(max = 20, message = "Lịch sử trò chuyện tối đa 20 lượt") List<@NotNull @Valid Turn> history) {
  /** Only the latest turns are sent to the model. */
  public static final int MAX_HISTORY = 6;
  public record Turn(
      @NotNull(message = "Thiếu người gửi trong lịch sử trò chuyện") Boolean fromAssistant,
      @NotBlank(message = "Nội dung lịch sử trò chuyện không được để trống")
      @Size(max = 4000, message = "Mỗi lượt trò chuyện tối đa 4000 ký tự") String text) {}
}
