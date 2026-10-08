package com.example.course_management.service;

import com.example.course_management.dto.response.PageResponse;
import com.example.course_management.entity.EnrollmentStatus;
import com.example.course_management.exception.*;
import com.example.course_management.repository.CourseRepository;
import com.example.course_management.security.CustomUserDetails;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CourseStudentReportService {
  public record Summary(long students, long enrolled, long completed, long dropped,
      long publishedLessons, long pendingQuestions) {}
  public record Student(int enrollmentId, int studentId, String name, String username,
      String status, BigDecimal progressPercentage, long completedLessons, long quizAttempts,
      BigDecimal averageScore, Integer bestScore, LocalDateTime lastActivity,
      long pendingQuestions, Integer pendingQuestionId, Integer pendingLessonId) {}
  public record Report(Summary summary, PageResponse<Student> students) {}
  private record Enrollment(int id, int studentId, String name, String username, String status, BigDecimal progress) {}
  private record Activity(long count, BigDecimal average, Integer best, LocalDateTime at) {}
  private record Pending(long count, Integer questionId, Integer lessonId, LocalDateTime at) {}
  private final JdbcTemplate jdbc;
  private final NamedParameterJdbcTemplate named;
  private final CourseRepository courses;
  private final ContentPolicy policy;

  public CourseStudentReportService(JdbcTemplate jdbc, CourseRepository courses, ContentPolicy policy) {
    this.jdbc = jdbc; this.named = new NamedParameterJdbcTemplate(jdbc); this.courses = courses; this.policy = policy;
  }

  public Report get(int courseId, String search, EnrollmentStatus status, String sort,
      int page, int size, CustomUserDetails actor) {
    var course = courses.findById(courseId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học"));
    policy.manager(course, actor);
    if (page < 0 || page >= 1000000 || size < 1 || size > 100)
      throw new BadRequestException("Trang hoặc kích thước trang không hợp lệ");
    if (search.length() > 255) throw new BadRequestException("Từ khóa tối đa 255 ký tự");
    if (!List.of("name", "progress").contains(sort)) throw new BadRequestException("Thứ tự không hợp lệ");
    var summary = jdbc.queryForObject("""
        SELECT COUNT(*) AS students,
          COALESCE(SUM(CASE WHEN status='ENROLLED' THEN 1 ELSE 0 END),0) AS enrolled,
          COALESCE(SUM(CASE WHEN status='COMPLETED' THEN 1 ELSE 0 END),0) AS completed,
          COALESCE(SUM(CASE WHEN status='DROPPED' THEN 1 ELSE 0 END),0) AS dropped
        FROM enrollments WHERE course_id=?
        """, (r,n) -> new long[]{r.getLong(1),r.getLong(2),r.getLong(3),r.getLong(4)}, courseId);
    long published = jdbc.queryForObject("SELECT COUNT(*) FROM lessons WHERE course_id=? AND is_published=TRUE",Long.class,courseId);
    long pendingTotal = jdbc.queryForObject("SELECT COUNT(*) FROM lesson_questions q JOIN lessons l ON l.lesson_id=q.lesson_id WHERE l.course_id=? AND q.is_hidden=FALSE AND (q.answer IS NULL OR TRIM(q.answer)='')",Long.class,courseId);
    var parameters = new HashMap<String,Object>();
    parameters.put("courseId", courseId);
    String pattern = "%" + search.strip().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_") + "%";
    parameters.put("search", pattern);
    String where = " WHERE e.course_id=:courseId AND (LOWER(u.full_name) LIKE :search ESCAPE '!' OR LOWER(u.username) LIKE :search ESCAPE '!')";
    if (status != null) { where += " AND e.status=:status"; parameters.put("status",status.name()); }
    long total = named.queryForObject("SELECT COUNT(*) FROM enrollments e JOIN users u ON u.user_id=e.student_id"+where,parameters,Long.class);
    parameters.put("size",size); parameters.put("offset",(long)page*size);
    String order = sort.equals("progress") ? "e.progress_percentage DESC, LOWER(u.full_name), e.enrollment_id" : "LOWER(u.full_name), e.enrollment_id";
    var enrollments = named.query("SELECT e.enrollment_id,e.student_id,u.full_name,u.username,e.status,e.progress_percentage FROM enrollments e JOIN users u ON u.user_id=e.student_id"+where+" ORDER BY "+order+" LIMIT :size OFFSET :offset",
        parameters,(r,n) -> new Enrollment(r.getInt(1),r.getInt(2),r.getString(3),r.getString(4),r.getString(5),r.getBigDecimal(6)));
    var lessonActivity = new HashMap<Integer,Activity>();
    var quizActivity = new HashMap<Integer,Activity>();
    var questions = new HashMap<Integer,Pending>();
    if (!enrollments.isEmpty()) {
      parameters.put("enrollmentIds",enrollments.stream().map(Enrollment::id).toList());
      parameters.put("studentIds",enrollments.stream().map(Enrollment::studentId).toList());
      named.query("""
          SELECT p.enrollment_id, SUM(CASE WHEN p.is_completed=TRUE AND l.is_published=TRUE THEN 1 ELSE 0 END), MAX(p.last_accessed_at)
          FROM lesson_progress p JOIN lessons l ON l.lesson_id=p.lesson_id
          WHERE p.enrollment_id IN (:enrollmentIds) AND l.course_id=:courseId GROUP BY p.enrollment_id
          """, parameters, (org.springframework.jdbc.core.RowCallbackHandler)r -> lessonActivity.put(r.getInt(1),new Activity(r.getLong(2),null,null,time(r,3))));
      named.query("""
          SELECT a.student_id,COUNT(*),AVG(CAST(a.score AS DECIMAL(10,2))),MAX(a.score),MAX(a.submitted_at)
          FROM quiz_attempts a JOIN lesson_quiz_versions v ON v.quiz_version_id=a.quiz_version_id JOIN lessons l ON l.lesson_id=v.lesson_id
          WHERE l.course_id=:courseId AND a.student_id IN (:studentIds) GROUP BY a.student_id
          """, parameters, (org.springframework.jdbc.core.RowCallbackHandler)r -> quizActivity.put(r.getInt(1),new Activity(r.getLong(2),r.getBigDecimal(3),(Integer)r.getObject(4),time(r,5))));
      named.query("""
          SELECT grouped.student_id,grouped.pending_count,grouped.first_id,first_question.lesson_id,grouped.last_question
          FROM (
            SELECT q.student_id,MAX(q.created_at) AS last_question,
              SUM(CASE WHEN q.answer IS NULL OR TRIM(q.answer)='' THEN 1 ELSE 0 END) AS pending_count,
              MIN(CASE WHEN q.answer IS NULL OR TRIM(q.answer)='' THEN q.question_id ELSE NULL END) AS first_id
            FROM lesson_questions q JOIN lessons l ON l.lesson_id=q.lesson_id
            WHERE l.course_id=:courseId AND q.is_hidden=FALSE AND q.student_id IN (:studentIds) GROUP BY q.student_id
          ) grouped LEFT JOIN lesson_questions first_question ON first_question.question_id=grouped.first_id
          """, parameters, (org.springframework.jdbc.core.RowCallbackHandler)r -> questions.put(r.getInt(1),new Pending(r.getLong(2),(Integer)r.getObject(3),(Integer)r.getObject(4),time(r,5))));
    }
    var rows = enrollments.stream().map(e -> {
      var p = lessonActivity.getOrDefault(e.id(),new Activity(0,null,null,null));
      var q = quizActivity.getOrDefault(e.studentId(),new Activity(0,null,null,null));
      var pending = questions.getOrDefault(e.studentId(),new Pending(0,null,null,null));
      var last = java.util.stream.Stream.of(p.at(),q.at(),pending.at()).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);
      return new Student(e.id(),e.studentId(),e.name(),e.username(),e.status(),e.progress(),p.count(),q.count(),q.average(),q.best(),last,pending.count(),pending.questionId(),pending.lessonId());
    }).toList();
    return new Report(new Summary(summary[0],summary[1],summary[2],summary[3],published,pendingTotal),
        new PageResponse<>(rows,page,size,total,(int)((total+size-1)/size)));
  }

  private LocalDateTime time(java.sql.ResultSet r,int column) throws java.sql.SQLException {
    var value = r.getTimestamp(column); return value == null ? null : value.toLocalDateTime();
  }
}
