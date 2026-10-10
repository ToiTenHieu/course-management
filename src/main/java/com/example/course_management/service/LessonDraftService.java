package com.example.course_management.service;

import com.example.course_management.dto.request.SaveLessonDraftRequest;
import com.example.course_management.entity.Lesson;
import com.example.course_management.exception.*;
import com.example.course_management.repository.*;
import com.example.course_management.security.CustomUserDetails;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LessonDraftService {
  public record Content(String title, Integer orderIndex, String contentUrl, String textContent,
      String contentFormat, String videoUrl) {}
  public record Draft(int revision, Integer baseRevision, Content content, LocalDateTime updatedAt) {}
  public record Version(int versionId, int revision, String editorName, Content content, LocalDateTime createdAt) {}
  private final JdbcTemplate jdbc;
  private final CourseRepository courses;
  private final LessonRepository lessons;
  private final ContentPolicy policy;

  public LessonDraftService(JdbcTemplate jdbc, CourseRepository courses,
      LessonRepository lessons, ContentPolicy policy) {
    this.jdbc = jdbc; this.courses = courses; this.lessons = lessons; this.policy = policy;
  }

  public Draft get(int courseId, int key, CustomUserDetails actor) {
    manager(courseId, key, actor);
    return find(courseId, key, actor);
  }

  public Draft save(int courseId, int key, SaveLessonDraftRequest r, CustomUserDetails actor) {
    manager(courseId, key, actor);
    if (key != 0 && r.getBaseRevision() == null) throw new BadRequestException("Thiếu phiên bản bài học gốc");
    var current = find(courseId, key, actor);
    int revision = current == null ? 0 : current.revision();
    if (revision != r.getExpectedRevision()) throw conflict();
    int owner = actor.getUser().getUserId();
    if (current == null) {
      jdbc.update("INSERT INTO lesson_drafts(course_id,owner_id,lesson_id,lesson_key,revision,base_revision,title,order_index,content_url,text_content,content_format,video_url,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
          courseId, owner, key == 0 ? null : key, key, 1, r.getBaseRevision(), r.getTitle(), r.getOrderIndex(), r.getContentUrl(), r.getTextContent(), r.getContentFormat(), r.getVideoUrl(),com.example.course_management.time.ApplicationTime.now());
    } else {
      jdbc.update("UPDATE lesson_drafts SET is_active=TRUE,revision=?,base_revision=?,title=?,order_index=?,content_url=?,text_content=?,content_format=?,video_url=?,updated_at=? WHERE course_id=? AND owner_id=? AND lesson_key=?",
          revision + 1, r.getBaseRevision(), r.getTitle(), r.getOrderIndex(), r.getContentUrl(), r.getTextContent(), r.getContentFormat(), r.getVideoUrl(), com.example.course_management.time.ApplicationTime.now(),courseId, owner, key);
    }
    return find(courseId, key, actor);
  }

  public void delete(int courseId, int key, int revision, CustomUserDetails actor) {
    manager(courseId, key, actor);
    consume(courseId, key, revision, actor);
  }

  // Called while the course lock is held by the publishing transaction.
  public void consume(int courseId, int key, Integer revision, CustomUserDetails actor) {
    if (revision == null) return;
    var current = find(courseId, key, actor);
    if (current == null || current.content() == null || current.revision() != revision) throw conflict();
    jdbc.update("UPDATE lesson_drafts SET is_active=FALSE,revision=revision+1,base_revision=NULL,title=NULL,order_index=NULL,content_url=NULL,text_content=NULL,content_format=NULL,video_url=NULL,updated_at=? WHERE course_id=? AND owner_id=? AND lesson_key=?",
        com.example.course_management.time.ApplicationTime.now(),courseId, actor.getUser().getUserId(), key);
  }

  public List<Version> history(int id, CustomUserDetails actor) {
    var courseId = lessons.findCourseId(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học"));
    manager(courseId, id, actor);
    return jdbc.query("SELECT v.*, u.full_name FROM lesson_content_versions v JOIN users u ON u.user_id=v.editor_id WHERE v.lesson_id=? ORDER BY v.revision DESC LIMIT 30",
        (r,n) -> new Version(r.getInt("version_id"),r.getInt("revision"),r.getString("full_name"),content(r),r.getTimestamp("created_at").toLocalDateTime()), id);
  }

  public void snapshot(Lesson l, CustomUserDetails actor) {
    jdbc.update("INSERT INTO lesson_content_versions(lesson_id,revision,editor_id,title,order_index,content_url,text_content,content_format,video_url,created_at) VALUES (?,?,?,?,?,?,?,?,?,?)",
        l.getLessonId(), l.getContentRevision(), actor.getUser().getUserId(), l.getTitle(), l.getOrderIndex(), l.getContentUrl(), l.getTextContent(), l.getContentFormat(), l.getVideoUrl(),com.example.course_management.time.ApplicationTime.now());
  }

  private Draft find(int courseId, int key, CustomUserDetails actor) {
    var rows = jdbc.query("SELECT * FROM lesson_drafts WHERE course_id=? AND owner_id=? AND lesson_key=?",
        (r,n) -> new Draft(r.getInt("revision"), (Integer)r.getObject("base_revision"), r.getBoolean("is_active") ? content(r) : null,r.getTimestamp("updated_at").toLocalDateTime()), courseId, actor.getUser().getUserId(), key);
    return rows.isEmpty() ? null : rows.getFirst();
  }

  private Content content(java.sql.ResultSet r) throws java.sql.SQLException {
    return new Content(r.getString("title"),(Integer)r.getObject("order_index"),r.getString("content_url"),r.getString("text_content"),r.getString("content_format"),r.getString("video_url"));
  }

  private void manager(int courseId, int key, CustomUserDetails actor) {
    if (key < 0) throw new BadRequestException("Mã bài học không hợp lệ");
    var c = courses.findLockedById(courseId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học"));
    policy.manager(c, actor);
    if (key != 0 && !lessons.findCourseId(key).filter(course -> course == courseId).isPresent())
      throw new ResourceNotFoundException("Không tìm thấy bài học trong khóa");
  }

  private ConflictException conflict() {
    return new ConflictException("Bản nháp đã thay đổi ở tab khác. Mở lại trình soạn để lấy bản mới; nội dung hiện tại vẫn được giữ.");
  }
}
