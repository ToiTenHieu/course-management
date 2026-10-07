package com.example.course_management.config;

import com.example.course_management.dto.request.*;
import com.example.course_management.entity.*;
import com.example.course_management.repository.*;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Repeatable role scenarios, recorded once in the demo database. */
@Component
public class DemoScenarioSeeder {
  private static final String KEY = "role-scenarios-v1";
  private final JdbcTemplate jdbc;
  private final UserRepository users;
  private final CourseRepository courses;
  private final LessonRepository lessons;
  private final EnrollmentRepository enrollments;
  private final NotificationRepository notifications;
  private final PasswordEncoder encoder;
  private final PaymentService payments;
  private final EnrollmentService learning;
  private final ReviewService reviews;
  private final LessonQuestionService questions;
  private final LessonQuizService quizzes;

  public DemoScenarioSeeder(JdbcTemplate jdbc, UserRepository users, CourseRepository courses,
      LessonRepository lessons, EnrollmentRepository enrollments, NotificationRepository notifications,
      PasswordEncoder encoder, PaymentService payments, EnrollmentService learning,
      ReviewService reviews, LessonQuestionService questions, LessonQuizService quizzes) {
    this.jdbc = jdbc; this.users = users; this.courses = courses; this.lessons = lessons;
    this.enrollments = enrollments; this.notifications = notifications; this.encoder = encoder;
    this.payments = payments; this.learning = learning; this.reviews = reviews;
    this.questions = questions; this.quizzes = quizzes;
  }

  @Transactional
  public void seed() {
    if (jdbc.queryForObject("SELECT COUNT(*) FROM demo_seed_runs WHERE dataset_key = ?",
        Integer.class, KEY) > 0) return;
    var admin = account("admin_demo", "Quản trị viên Demo", Role.ADMIN);
    var teacher = account("teacher_demo", "Nguyễn Minh Anh", Role.TEACHER);
    var otherTeacher = account("teacher_scenario", "Trần Hà Phương (mẫu)", Role.TEACHER);
    var student = account("student_demo", "Học viên Demo", Role.STUDENT);
    String[] topics = {"Python thực hành", "React và giao diện tương tác", "Phân tích dữ liệu",
        "Figma cho sản phẩm số", "Kiểm thử phần mềm", "Docker và triển khai",
        "Tiếng Anh giao tiếp", "Viết và trình bày ý tưởng", "Excel cho công việc",
        "Quản lý dự án", "Thiết kế hệ thống", "SQL nâng cao"};
    String[] categories = {"Lập trình", "Lập trình", "Dữ liệu", "Thiết kế", "Công cụ",
        "Công cụ", "Ngoại ngữ", "Kỹ năng", "Dữ liệu", "Kỹ năng", "Lập trình", "Dữ liệu"};
    var samples = new ArrayList<Course>();
    for (int i = 0; i < 36; i++) {
      var c = new Course();
      c.setTitle(topics[i % 12] + " · " + new String[]{"Nhập môn", "Ứng dụng", "Dự án"}[i / 12]);
      c.setCategory(categories[i % 12]);
      c.setLevel(i < 12 ? "Cơ bản" : i < 24 ? "Trung cấp" : "Nâng cao");
      c.setTeacher(i % 3 == 0 ? otherTeacher : teacher);
      c.setPrice(i % 4 == 3 ? BigDecimal.ZERO : BigDecimal.valueOf(99000 + (i % 5) * 50000));
      c.setDurationHours(6 + i % 12);
      c.setDescription("Dữ liệu mẫu để trải nghiệm Course Management. Học qua ví dụ, tự kiểm tra và một dự án nhỏ về " + topics[i % 12] + ".");
      c.setLearningOutcomes("Giải thích các khái niệm chính\nÁp dụng vào tình huống thực tế\nHoàn thành và tự đánh giá dự án");
      c.setStatus(i < 24 ? CourseStatus.PUBLISHED : i < 30 ? CourseStatus.DRAFT : CourseStatus.ARCHIVED);
      courses.saveAndFlush(c);
      for (int j = 0; j < (i == 3 ? 24 : 5); j++) {
        var l = new Lesson(); l.setCourse(c); l.setTitle("Bài " + (j + 1) + ": " + new String[]{"Làm quen và chuẩn bị", "Khái niệm và ví dụ", "Thực hành từng bước", "Kiểm tra và cải thiện", "Tổng kết dự án"}[j % 5]);
        l.setOrderIndex(j + 1); l.setIsPublished(j != 4 || c.getStatus() == CourseStatus.ARCHIVED);
        l.setTextContent("Mục tiêu\nÁp dụng kiến thức của " + topics[i % 12] + ".\n\nVí dụ\nChia bài toán thành từng bước và kiểm tra kết quả.\n\nThực hành\nViết một ví dụ của bạn, ghi chú điều chưa rõ và thảo luận với giảng viên.\n\nĐây là nội dung mẫu phục vụ kiểm tra các vai trò.");
        lessons.save(l);
      }
      samples.add(c);
    }
    for (int i = 0; i < 12; i++) {
      var c = samples.get(i); var e = enroll(student, c, admin);
      var visible = lessons.findByCourse_CourseIdAndIsPublishedTrueOrderByOrderIndex(c.getCourseId());
      for (int j = 0; j < (i < 2 ? visible.size() : i % visible.size()); j++)
        learning.completeLesson(e.getEnrollmentId(), visible.get(j).getLessonId(), actor(student));
      learning.saveNote(e.getEnrollmentId(), visible.getFirst().getLessonId(), "Ghi chú mẫu: ôn lại ví dụ và đặt câu hỏi.", actor(student));
    }
    for (int i = 12; i < 24; i++) {
      var c = samples.get(i);
      if (c.getPrice().signum() == 0) continue;
      var p = payments.createPayment(c.getCourseId(), actor(student));
      if (i % 2 == 0) payments.rejectPayment(p.getPaymentId(), actor(admin));
    }
    var shared = samples.get(3);
    var lesson = lessons.findByCourse_CourseIdAndIsPublishedTrueOrderByOrderIndex(shared.getCourseId()).getFirst();
    var quiz = quizzes.save(lesson.getLessonId(), new SaveQuizRequest(0, "Tự kiểm tra kiến thức", 70, true,
        List.of(new SaveQuizRequest.Question("Cách nào giúp học hiệu quả?", List.of("Thực hành và kiểm tra", "Chỉ đọc tiêu đề", "Bỏ qua phản hồi", "Sao chép đáp án"), 0, "Thực hành và phản hồi giúp xác định điều cần cải thiện."))), actor(shared.getTeacher()));
    for (int i = 1; i <= 26; i++) {
      var u = account("student_scenario_" + i, "Học viên mẫu " + String.format("%02d", i), Role.STUDENT);
      enroll(u, shared, admin);
      var p = payments.createPayment(samples.get(0).getCourseId(), actor(u));
      if (i % 3 == 0) payments.confirmPayment(p.getPaymentId(), actor(admin));
      else if (i % 3 == 1) payments.rejectPayment(p.getPaymentId(), actor(admin));
      var review = new ReviewRequest(); review.setRating(3 + i % 3);
      review.setComment("Nhận xét mẫu " + i + ": ví dụ dễ theo dõi, cần thêm bài thực hành.");
      reviews.create(shared.getCourseId(), review, actor(u));
      var q = questions.ask(lesson.getLessonId(), "Câu hỏi mẫu " + i + ": em nên kiểm tra kết quả bài thực hành như thế nào?", actor(u));
      if (i % 3 == 0) questions.answer(q.questionId(), "Hãy thử một trường hợp đơn giản, sau đó kiểm tra các trường hợp biên.", actor(shared.getTeacher()));
      if (i % 7 == 0) questions.visibility(q.questionId(), true, actor(shared.getTeacher()));
      quizzes.submit(lesson.getLessonId(), new SubmitQuizRequest(quiz.quizVersionId(), java.util.UUID.randomUUID().toString(), List.of(i % 2)), actor(u));
      if (i > 22) { u.setIsActive(false); users.save(u); }
    }
    for (var u : List.of(admin, teacher, otherTeacher, student)) {
      for (int i = 1; i <= 18; i++) {
        var n = new Notification(); n.setUser(u); n.setMessage("Cập nhật mẫu " + i + ": xem lại nội dung và kế hoạch học tập.");
        n.setType("GENERAL"); n.setIsRead(i % 3 == 0); n.setTargetUrl("/courses.html"); notifications.save(n);
      }
    }
    jdbc.update("INSERT INTO demo_seed_runs (dataset_key) VALUES (?)", KEY);
  }

  private Enrollment enroll(User student, Course course, User admin) {
    if (course.getPrice().signum() == 0) learning.enroll(course.getCourseId(), actor(student));
    else {
      var p = payments.createPayment(course.getCourseId(), actor(student));
      payments.confirmPayment(p.getPaymentId(), actor(admin));
    }
    return enrollments.findByStudent_UserIdAndCourse_CourseId(student.getUserId(), course.getCourseId()).orElseThrow();
  }

  private User account(String username, String name, Role role) {
    return users.findByUsername(username).orElseGet(() -> {
      var u = new User(); u.setUsername(username); u.setFullName(name); u.setRole(role);
      u.setEmail(username + "@example.invalid"); u.setPasswordHash(encoder.encode("Demo123!"));
      return users.saveAndFlush(u);
    });
  }

  private CustomUserDetails actor(User u) { return new CustomUserDetails(u); }
}
