package com.example.course_management.config;

import com.example.course_management.dto.request.SaveSettingsRequest;
import com.example.course_management.dto.request.SaveQuizRequest;
import com.example.course_management.exception.BadRequestException;
import com.example.course_management.exception.ConflictException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;

@Service
public class LearningSettings {
  private final JdbcTemplate jdbc;
  private final long uploadCeilingBytes;

  public LearningSettings(JdbcTemplate jdbc,
      @Value("${spring.servlet.multipart.max-file-size}") DataSize maxFileSize,
      @Value("${spring.servlet.multipart.max-request-size}") DataSize maxRequestSize) {
    this.jdbc = jdbc;
    this.uploadCeilingBytes = Math.min(maxFileSize.toBytes(), maxRequestSize.toBytes());
  }

  // Read the persisted row on every operation so changes apply without a restart.
  public SaveSettingsRequest current() {
    return jdbc.queryForObject("SELECT * FROM application_settings WHERE id = 1", (row, number) ->
        new SaveSettingsRequest(row.getInt("revision"), row.getLong("max_file_bytes"),
            row.getInt("max_resources_per_lesson"), row.getInt("max_quiz_questions"),
            row.getInt("default_pass_percentage"), row.getString("default_category"),
            row.getString("default_level"), row.getString("bank_name"),
            row.getString("bank_account"), row.getString("bank_holder")));
  }

  public long maxFileBytes() { return Math.min(current().maxFileBytes(), uploadCeilingBytes); }
  public int maxResourcesPerLesson() { return current().maxResourcesPerLesson(); }
  public int maxQuizQuestions() { return current().maxQuizQuestions(); }
  public long uploadCeilingBytes() { return uploadCeilingBytes; }

  public Map<String, Object> publicSettings() {
    var value = current();
    return Map.of("maxFileBytes", Math.min(value.maxFileBytes(), uploadCeilingBytes),
        "maxResourcesPerLesson", value.maxResourcesPerLesson(),
        "maxQuizQuestions", value.maxQuizQuestions(),
        "defaultPassPercentage", value.defaultPassPercentage(),
        "quizOptionCount", SaveQuizRequest.OPTION_COUNT,
        "defaultCategory", value.defaultCategory(), "defaultLevel", value.defaultLevel());
  }

  @Transactional
  public SaveSettingsRequest save(SaveSettingsRequest value) {
    if (value.maxFileBytes() > uploadCeilingBytes)
      throw new BadRequestException("Dung lượng file vượt giới hạn máy chủ: " + uploadCeilingBytes + " byte");
    int changed = jdbc.update("""
        UPDATE application_settings SET revision = revision + 1, max_file_bytes = ?,
            max_resources_per_lesson = ?, max_quiz_questions = ?, default_pass_percentage = ?,
            default_category = ?, default_level = ?, bank_name = ?, bank_account = ?, bank_holder = ?
        WHERE id = 1 AND revision = ?
        """, value.maxFileBytes(), value.maxResourcesPerLesson(), value.maxQuizQuestions(),
        value.defaultPassPercentage(), value.defaultCategory().trim(), value.defaultLevel().trim(),
        value.bankName().trim(), value.bankAccount().trim(), value.bankHolder().trim(), value.revision());
    if (changed != 1)
      throw new ConflictException("Cài đặt đã được thay đổi ở nơi khác. Tải lại trang trước khi lưu.");
    return current();
  }
}
