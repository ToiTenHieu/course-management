package com.example.course_management.config;

import com.example.course_management.entity.*;
import com.example.course_management.repository.*;
import java.math.BigDecimal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
public class DemoData implements CommandLineRunner {
  private final UserRepository users;
  private final CourseRepository courses;
  private final LessonRepository lessons;
  private final PasswordEncoder encoder;

  public DemoData(
      UserRepository users,
      CourseRepository courses,
      LessonRepository lessons,
      PasswordEncoder encoder) {
    this.users = users;
    this.courses = courses;
    this.lessons = lessons;
    this.encoder = encoder;
  }

  @Override
  @Transactional
  public void run(String... args) {
    account("admin_demo", "Quản trị viên Demo", Role.ADMIN);
    var teacher = account("teacher_demo", "Nguyễn Minh Anh", Role.TEACHER);
    account("student_demo", "Học viên Demo", Role.STUDENT);
    if (courses.count() > 1) return;
    String[][] samples = {
      {"Java từ nền tảng đến ứng dụng", "Lập trình", "Cơ bản", "0", "12"},
      {"Thiết kế giao diện với tư duy sản phẩm", "Thiết kế", "Cơ bản", "249000", "8"},
      {"SQL: từ truy vấn đến phân tích dữ liệu", "Dữ liệu", "Trung cấp", "199000", "10"},
      {"Xây dựng website với HTML, CSS và JavaScript", "Lập trình", "Cơ bản", "0", "16"},
      {"Git và quy trình làm việc nhóm", "Công cụ", "Cơ bản", "99000", "6"},
      {"Spring Boot: xây dựng API thực tế", "Lập trình", "Nâng cao", "349000", "20"}
    };
    for (var sample : samples) {
      var c = new Course();
      c.setTitle(sample[0]);
      c.setCategory(sample[1]);
      c.setLevel(sample[2]);
      c.setTeacher(teacher);
      c.setPrice(new BigDecimal(sample[3]));
      c.setDurationHours(Integer.parseInt(sample[4]));
      c.setStatus(CourseStatus.PUBLISHED);
      c.setDescription(
          "Học từng bước qua ví dụ cụ thể, bài thực hành và dự án nhỏ. Khóa học giúp bạn xây dựng"
              + " nền tảng vững chắc và tự tin áp dụng kiến thức.");
      c.setLearningOutcomes(
          "Hiểu các khái niệm nền tảng\n"
              + "Áp dụng kiến thức vào bài thực hành\n"
              + "Hoàn thành một dự án nhỏ của riêng bạn");
      courses.save(c);
      String[] titles = {
        "Bắt đầu và chuẩn bị môi trường",
        "Các khái niệm cốt lõi",
        "Thực hành qua ví dụ",
        "Tổng kết và dự án nhỏ"
      };
      for (int i = 0; i < titles.length; i++) {
        var l = new Lesson();
        l.setCourse(c);
        l.setTitle(titles[i]);
        l.setOrderIndex(i + 1);
        l.setIsPublished(true);
        l.setTextContent(
            "Bài "
                + (i + 1)
                + ": "
                + titles[i]
                + "\n\n"
                + "Mục tiêu\n"
                + "Sau bài này, bạn có thể giải thích khái niệm vừa học và áp dụng vào một ví dụ"
                + " nhỏ.\n\n"
                + "Nội dung\n"
                + "Bắt đầu từ vấn đề thực tế, chia thành từng bước và kiểm tra kết quả ở mỗi bước."
                + " Ghi lại các câu hỏi để tự tìm hiểu thêm.\n\n"
                + "Thực hành\n"
                + "1. Tóm tắt ba điều bạn vừa học.\n"
                + "2. Tạo một ví dụ theo cách hiểu của bạn.\n"
                + "3. Kiểm tra ví dụ với một trường hợp khác.\n\n"
                + "Đây là nội dung mẫu của học viện để trình diễn luồng học tập.");
        lessons.save(l);
      }
    }
  }

  private User account(String name, String fullName, Role role) {
    return users
        .findByUsername(name)
        .orElseGet(
            () -> {
              var u = new User();
              u.setUsername(name);
              u.setFullName(fullName);
              u.setEmail(name + "@example.invalid");
              u.setPasswordHash(encoder.encode("Demo123!"));
              u.setRole(role);
              return users.save(u);
            });
  }
}
