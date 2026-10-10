package com.example.course_management.service;

import com.example.course_management.time.ApplicationTime;
import com.example.course_management.dto.response.PageResponse;
import com.example.course_management.entity.*;
import com.example.course_management.exception.*;
import com.example.course_management.repository.*;
import com.example.course_management.security.CustomUserDetails;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service @Transactional
public class AssignmentService {
  public record Definition(int assignmentId, int lessonId, String title, String instructions, boolean published, int revision) {}
  public record Save(@NotNull @Min(0) Integer expectedRevision, @NotBlank @Size(max=255) String title,
      @NotBlank @Size(max=10000) String instructions, boolean published) {}
  public record Grade(@NotNull @Min(0) Integer expectedRevision, @NotNull @Min(0) @Max(100) Integer score,
      @NotNull @Size(max=10000) String feedback) {}
  public record Submission(int submissionId, int studentId, String studentName, int assignmentRevision,
      String assignmentTitle, String assignmentInstructions, String answer, String fileName,
      LocalDateTime submittedAt, Integer score, String feedback, String gradedByName,
      LocalDateTime gradedAt, int gradeRevision) {}
  public record View(Definition assignment, Submission submission) {}
  public record Download(String name, String mediaType, byte[] content) {}
  private final JdbcTemplate jdbc;
  private final LessonRepository lessons;
  private final CourseRepository courses;
  private final ContentPolicy policy;
  private final LessonService lessonService;
  private final LessonResourceService resources;
  public AssignmentService(JdbcTemplate jdbc, LessonRepository lessons, CourseRepository courses,
      ContentPolicy policy, LessonService lessonService, LessonResourceService resources) {
    this.jdbc=jdbc; this.lessons=lessons; this.courses=courses; this.policy=policy;
    this.lessonService=lessonService; this.resources=resources;
  }
  public View view(int lessonId, CustomUserDetails actor) {
    lessonService.getLessonById(lessonId,actor);
    var l=lesson(lessonId); var definition=definition(lessonId);
    var own=definition==null ? null : own(definition.assignmentId(),actor.getUser().getUserId());
    if (definition!=null && !definition.published() && !policy.manages(l.getCourse(),actor)) definition=null;
    return new View(definition,own);
  }
  public Definition save(int lessonId, Save r, CustomUserDetails actor) {
    var l=lockedLesson(lessonId); policy.manager(l.getCourse(),actor);
    var old=definition(lessonId);
    if (r.expectedRevision()!=(old==null ? 0 : old.revision()))
      throw new ConflictException("Bài tập đã thay đổi. Tải lại trước khi lưu; nội dung đang soạn vẫn được giữ.");
    if (old==null) jdbc.update("INSERT INTO lesson_assignments(lesson_id,title,instructions,published) VALUES (?,?,?,?)",
        lessonId,r.title().strip(),r.instructions().strip(),r.published());
    else jdbc.update("UPDATE lesson_assignments SET title=?,instructions=?,published=?,revision=revision+1 WHERE assignment_id=?",
        r.title().strip(),r.instructions().strip(),r.published(),old.assignmentId());
    return definition(lessonId);
  }
  public Submission submit(int lessonId, int expectedRevision, String key, String answer, MultipartFile file, CustomUserDetails actor) {
    var l=lockedLesson(lessonId);
    if (actor.getUser().getRole()!=Role.STUDENT) throw new ForbiddenException("Chỉ học viên được nộp bài");
    lessonService.getLessonById(lessonId,actor);
    var d=definition(lessonId);
    if (d==null) throw new ResourceNotFoundException("Không tìm thấy bài tập");
    try { if (!UUID.fromString(key).toString().equals(key)) throw new IllegalArgumentException(); }
    catch (RuntimeException ex) { throw new BadRequestException("Mã lượt nộp không hợp lệ"); }
    if (answer==null || answer.length()>10000) throw new BadRequestException("Bài làm tối đa 10.000 ký tự");
    var uploaded=file==null || file.isEmpty() ? null : resources.validateFile(file);
    answer=answer.strip();
    if (answer.isEmpty() && uploaded==null) throw new BadRequestException("Nhập bài làm hoặc chọn tệp đính kèm");
    String hash;
    try {
      var digest=java.security.MessageDigest.getInstance("SHA-256");
      digest.update((expectedRevision+"\n"+answer+"\n"+(uploaded==null ? "" : uploaded.name())+"\n").getBytes(StandardCharsets.UTF_8));
      if (uploaded!=null) digest.update(uploaded.content());
      hash=HexFormat.of().formatHex(digest.digest());
    } catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    int studentId=actor.getUser().getUserId();
    var previous=jdbc.queryForList("SELECT submission_key,request_hash FROM assignment_submissions WHERE assignment_id=? AND student_id=?",d.assignmentId(),studentId);
    if (!previous.isEmpty()) {
      var row=previous.getFirst();
      if (key.equals(row.get("submission_key")) && hash.equals(row.get("request_hash"))) return own(d.assignmentId(),studentId);
      throw new ConflictException("Bạn đã nộp bài tập này. Xem bài đã nộp và nhận xét của giảng viên.");
    }
    if (!d.published()) throw new ResourceNotFoundException("Bài tập chưa xuất bản");
    if (d.revision()!=expectedRevision) throw new ConflictException("Đề bài đã thay đổi. Tải lại đề trước khi nộp.");
    jdbc.update("""
        INSERT INTO assignment_submissions(assignment_id,student_id,submission_key,request_hash,assignment_revision,
          assignment_title,assignment_instructions,answer,file_name,media_type,file_content,submitted_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
        """,d.assignmentId(),studentId,key,hash,d.revision(),d.title(),d.instructions(),answer,
        uploaded==null ? null : uploaded.name(),uploaded==null ? null : uploaded.mediaType(),uploaded==null ? null : uploaded.content(),com.example.course_management.time.ApplicationTime.now());
    return own(d.assignmentId(),studentId);
  }
  public PageResponse<Submission> list(int lessonId, boolean ungradedOnly, int page, int size, CustomUserDetails actor) {
    var l=lesson(lessonId); policy.manager(l.getCourse(),actor);
    if (page<0 || page>=1000000 || size<1 || size>50) throw new BadRequestException("Trang không hợp lệ");
    var d=definition(lessonId);
    if (d==null) return new PageResponse<>(List.of(),page,size,0,0);
    String where=" WHERE s.assignment_id=?"+(ungradedOnly ? " AND s.score IS NULL" : "");
    long total=jdbc.queryForObject("SELECT COUNT(*) FROM assignment_submissions s"+where,Long.class,d.assignmentId());
    var rows=jdbc.query(select()+where+" ORDER BY s.submission_id DESC LIMIT ? OFFSET ?",this::map,d.assignmentId(),size,(long)page*size);
    return new PageResponse<>(rows,page,size,total,(int)((total+size-1)/size));
  }
  public Submission grade(int submissionId, Grade r, CustomUserDetails actor) {
    var l=lockedLesson(submissionLesson(submissionId)); policy.manager(l.getCourse(),actor);
    int count=jdbc.update("UPDATE assignment_submissions SET score=?,feedback=?,graded_by=?,graded_at=?,grade_revision=grade_revision+1 WHERE submission_id=? AND grade_revision=?",
        r.score(),r.feedback().strip(),actor.getUser().getUserId(),ApplicationTime.now(),submissionId,r.expectedRevision());
    if (count==0) throw new ConflictException("Bài đã được chấm lại. Tải danh sách để xem điểm mới.");
    return jdbc.queryForObject(select()+" WHERE s.submission_id=?",this::map,submissionId);
  }
  public Download download(int id, CustomUserDetails actor) {
    var l=lesson(submissionLesson(id));
    var s=jdbc.queryForObject(select()+" WHERE s.submission_id=?",this::map,id);
    if (!policy.manages(l.getCourse(),actor)) {
      if (s.studentId()!=actor.getUser().getUserId()) throw new ForbiddenException("Bạn không được xem bài nộp này");
      lessonService.getLessonById(l.getLessonId(),actor);
    }
    var rows=jdbc.query("SELECT file_name,media_type,file_content FROM assignment_submissions WHERE submission_id=? AND file_content IS NOT NULL",
        (rs,n)->new Download(rs.getString(1),rs.getString(2),rs.getBytes(3)),id);
    if (rows.isEmpty()) throw new ResourceNotFoundException("Bài nộp không có tệp");
    return rows.getFirst();
  }
  private int submissionLesson(int id) {
    var ids=jdbc.queryForList("SELECT a.lesson_id FROM assignment_submissions s JOIN lesson_assignments a ON a.assignment_id=s.assignment_id WHERE s.submission_id=?",Integer.class,id);
    if (ids.isEmpty()) throw new ResourceNotFoundException("Không tìm thấy bài nộp"); return ids.getFirst();
  }
  private Lesson lesson(int id) { return lessons.findById(id).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy bài học")); }
  private Lesson lockedLesson(int id) {
    var courseId=lessons.findCourseId(id).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy bài học"));
    courses.findLockedById(courseId).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy khóa học"));
    return lesson(id);
  }
  private Definition definition(int id) {
    var rows=jdbc.query("SELECT assignment_id,lesson_id,title,instructions,published,revision FROM lesson_assignments WHERE lesson_id=?",
        (r,n)->new Definition(r.getInt(1),r.getInt(2),r.getString(3),r.getString(4),r.getBoolean(5),r.getInt(6)),id);
    return rows.isEmpty() ? null : rows.getFirst();
  }
  private Submission own(int id,int studentId) {
    var rows=jdbc.query(select()+" WHERE s.assignment_id=? AND s.student_id=?",this::map,id,studentId);
    return rows.isEmpty() ? null : rows.getFirst();
  }
  private String select() { return """
      SELECT s.submission_id,s.student_id,s.assignment_revision,s.assignment_title,s.assignment_instructions,
        s.answer,s.file_name,s.submitted_at,s.score,s.feedback,s.graded_at,s.grade_revision,
        u.full_name AS student_name,g.full_name AS grader_name FROM assignment_submissions s
      JOIN users u ON u.user_id=s.student_id LEFT JOIN users g ON g.user_id=s.graded_by
      """; }
  private Submission map(java.sql.ResultSet r,int n) throws java.sql.SQLException {
    var graded=r.getTimestamp("graded_at");
    return new Submission(r.getInt("submission_id"),r.getInt("student_id"),r.getString("student_name"),r.getInt("assignment_revision"),
        r.getString("assignment_title"),r.getString("assignment_instructions"),r.getString("answer"),r.getString("file_name"),
        r.getTimestamp("submitted_at").toLocalDateTime(),(Integer)r.getObject("score"),r.getString("feedback"),r.getString("grader_name"),
        graded==null ? null : graded.toLocalDateTime(),r.getInt("grade_revision"));
  }
}
