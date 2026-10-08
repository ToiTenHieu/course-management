package com.example.course_management.service;

import com.example.course_management.exception.*;
import com.example.course_management.repository.*;
import com.example.course_management.security.CustomUserDetails;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class LessonResourceService {
  public record Resource(Integer resourceId, String name, String mediaType, int size) {}
  public record Download(Resource resource, byte[] content) {}
  private final JdbcTemplate jdbc;
  private final LessonService lessons;
  private final LessonRepository repository;
  private final CourseRepository courses;
  private final ContentPolicy policy;
  private final com.example.course_management.config.LearningSettings settings;

  public LessonResourceService(JdbcTemplate jdbc, LessonService lessons,
      LessonRepository repository, CourseRepository courses, ContentPolicy policy,
      com.example.course_management.config.LearningSettings settings) {
    this.jdbc = jdbc; this.lessons = lessons; this.repository = repository;
    this.courses = courses; this.policy = policy;
    this.settings = settings;
  }

  public List<Resource> list(Integer id, CustomUserDetails actor) {
    lessons.getLessonById(id, actor);
    return jdbc.query("SELECT resource_id, name, media_type, file_size FROM lesson_resources WHERE lesson_id=? ORDER BY resource_id",
        (r, n) -> new Resource(r.getInt(1), r.getString(2), r.getString(3), r.getInt(4)), id);
  }

  public List<Resource> upload(Integer id, MultipartFile file, CustomUserDetails actor) {
    manager(id, actor);
    if (file.isEmpty() || file.getSize() > settings.maxFileBytes())
      throw new BadRequestException("Chọn tài liệu từ 1 byte đến " + settings.maxFileBytes() + " byte");
    if (jdbc.queryForObject("SELECT COUNT(*) FROM lesson_resources WHERE lesson_id=?", Long.class, id) >= settings.maxResourcesPerLesson())
      throw new BadRequestException("Mỗi bài tối đa " + settings.maxResourcesPerLesson() + " tài liệu");
    byte[] bytes;
    try { bytes = file.getBytes(); }
    catch (java.io.IOException ex) { throw new BadRequestException("Không đọc được tài liệu, hãy thử lại"); }
    String original = file.getOriginalFilename() == null ? "tai-lieu" : file.getOriginalFilename();
    String name = original.replace('\\', '/');
    name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").trim();
    if (name.isBlank() || name.length() > 160) throw new BadRequestException("Tên tài liệu từ 1 đến 160 ký tự");
    String type = mediaType(name, bytes);
    jdbc.update("INSERT INTO lesson_resources(lesson_id,name,media_type,file_size,content) VALUES (?,?,?,?,?)",
        id, name, type, bytes.length, bytes);
    return list(id, actor);
  }

  public Download download(Integer id, CustomUserDetails actor) {
    Integer lessonId = resourceLesson(id);
    lessons.getLessonById(lessonId, actor);
    return jdbc.queryForObject("SELECT resource_id,name,media_type,file_size,content FROM lesson_resources WHERE resource_id=?",
        (r, n) -> new Download(new Resource(r.getInt(1), r.getString(2), r.getString(3), r.getInt(4)), r.getBytes(5)), id);
  }

  public void delete(Integer id, CustomUserDetails actor) {
    manager(resourceLesson(id), actor);
    jdbc.update("DELETE FROM lesson_resources WHERE resource_id=?", id);
  }

  private Integer resourceLesson(Integer id) {
    var ids = jdbc.queryForList("SELECT lesson_id FROM lesson_resources WHERE resource_id=?", Integer.class, id);
    if (ids.isEmpty()) throw new ResourceNotFoundException("Không tìm thấy tài liệu");
    return ids.getFirst();
  }

  private void manager(Integer id, CustomUserDetails actor) {
    var courseId = repository.findCourseId(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học"));
    var course = courses.findLockedById(courseId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học"));
    policy.manager(course, actor);
  }

  private String mediaType(String name, byte[] b) {
    String lower = name.toLowerCase(java.util.Locale.ROOT);
    if (lower.endsWith(".pdf") && starts(b, 0x25, 0x50, 0x44, 0x46, 0x2d)) return "application/pdf";
    if (lower.endsWith(".png") && starts(b, 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)) return "image/png";
    if ((lower.endsWith(".jpg") || lower.endsWith(".jpeg")) && starts(b, 0xff, 0xd8, 0xff)) return "image/jpeg";
    if (lower.endsWith(".webp") && b.length >= 12 && new String(b, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
        && new String(b, 8, 4, StandardCharsets.US_ASCII).equals("WEBP")) return "image/webp";
    if (lower.endsWith(".txt")) {
      try {
        var text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(b));
        if (text.chars().noneMatch(c -> c < 32 && c != 9 && c != 10 && c != 13)) return "text/plain";
      } catch (CharacterCodingException ignored) { }
    }
    throw new BadRequestException("Chỉ nhận PDF, TXT UTF-8, PNG, JPG hoặc WebP đúng định dạng");
  }

  private boolean starts(byte[] bytes, int... signature) {
    if (bytes.length < signature.length) return false;
    for (int i = 0; i < signature.length; i++) if ((bytes[i] & 255) != signature[i]) return false;
    return true;
  }
}
