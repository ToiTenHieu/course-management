package com.example.course_management.config;

import com.example.course_management.dto.request.SaveQuizRequest;
import com.example.course_management.repository.*;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.LessonQuizService;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(1)
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
public class DemoQuizData implements CommandLineRunner {
  private final UserRepository users;
  private final CourseRepository courses;
  private final LessonRepository lessons;
  private final LessonQuizRepository quizzes;
  private final LessonQuizService service;

  public DemoQuizData(
      UserRepository users,
      CourseRepository courses,
      LessonRepository lessons,
      LessonQuizRepository quizzes,
      LessonQuizService service) {
    this.users = users;
    this.courses = courses;
    this.lessons = lessons;
    this.quizzes = quizzes;
    this.service = service;
  }

  @Override
  @Transactional
  public void run(String... args) {
    var teacher = users.findByUsername("teacher_demo").orElse(null);
    if (teacher == null) return;
    var course =
        courses.findByTeacher_UserId(teacher.getUserId()).stream()
            .filter(c -> "Java từ nền tảng đến ứng dụng".equals(c.getTitle()))
            .findFirst()
            .orElse(null);
    if (course == null) return;
    var lesson =
        lessons
            .findByCourse_CourseIdAndIsPublishedTrueOrderByOrderIndex(course.getCourseId())
            .stream()
            .filter(l -> "Bắt đầu và chuẩn bị môi trường".equals(l.getTitle()))
            .findFirst()
            .orElse(null);
    if (lesson == null
        || quizzes.findFirstByLesson_LessonIdOrderByRevisionDesc(lesson.getLessonId()).isPresent())
      return;
    service.save(
        lesson.getLessonId(),
        new SaveQuizRequest(
            0,
            "Java: chuẩn bị môi trường",
            70,
            true,
            List.of(
                new SaveQuizRequest.Question(
                    "Bạn cần cài bộ công cụ nào để biên dịch chương trình Java?",
                    List.of("JDK", "Chỉ trình duyệt", "Chỉ trình soạn văn bản", "Git"),
                    0,
                    "JDK cung cấp trình biên dịch javac và các công cụ phát triển Java."),
                new SaveQuizRequest.Question(
                    "Tệp mã nguồn Java thường có phần mở rộng nào?",
                    List.of(".class", ".java", ".exe", ".sql"),
                    1,
                    "Mã nguồn được lưu trong tệp .java. Trình biên dịch tạo tệp .class chứa"
                        + " bytecode."),
                new SaveQuizRequest.Question(
                    "Lệnh nào biên dịch tệp Hello.java trong terminal?",
                    List.of(
                        "java Hello.java.class",
                        "git Hello.java",
                        "javac Hello.java",
                        "java --compile-all"),
                    2,
                    "Chạy javac Hello.java để biên dịch. Sau khi thành công, có thể chạy lớp Hello"
                        + " bằng java Hello."))),
        new CustomUserDetails(teacher));
  }
}
