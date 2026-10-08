package com.example.course_management.config;

import com.example.course_management.dto.request.SaveQuizRequest;
import com.example.course_management.entity.*;
import com.example.course_management.repository.*;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.LessonQuizService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Small, non-commercial demo courses; videos remain hosted by their YouTube creator. */
@Component
public class DemoYouTubeSeeder {
  private static final String KEY = "youtube-demo-courses-v1";
  private final JdbcTemplate jdbc;
  private final UserRepository users;
  private final CourseRepository courses;
  private final LessonRepository lessons;
  private final LessonQuizService quizzes;

  public DemoYouTubeSeeder(JdbcTemplate jdbc, UserRepository users, CourseRepository courses,
      LessonRepository lessons, LessonQuizService quizzes) {
    this.jdbc = jdbc;
    this.users = users;
    this.courses = courses;
    this.lessons = lessons;
    this.quizzes = quizzes;
  }

  @Transactional
  public void seed() {
    if (jdbc.queryForObject("SELECT COUNT(*) FROM demo_seed_runs WHERE dataset_key = ?",
        Integer.class, KEY) > 0) return;
    var teacher = users.findByUsername("teacher_demo").orElse(null);
    if (teacher == null) return;

    create(teacher, "HTML nhập môn · Demo YouTube", "kUMe1FH4CHE", "Dave Gray",
        "Learn HTML – Full Tutorial for Beginners",
        "Tạo trang HTML có cấu trúc rõ ràng\nThêm văn bản, liên kết và hình ảnh\nTạo biểu mẫu và trang giới thiệu cá nhân",
        List.of(
            new Chapter("Chuẩn bị và tạo trang HTML đầu tiên", 0, "00:00–29:02",
                "Nhận biết cấu trúc tài liệu HTML và phần head.",
                "Tạo index.html với doctype, html, head, body và tiêu đề trang. Mở bằng trình duyệt."),
            new Chapter("Văn bản và danh sách", 1742, "29:02–59:55",
                "Dùng tiêu đề, đoạn văn và danh sách để trình bày nội dung.",
                "Viết phần giới thiệu bản thân với một tiêu đề, hai đoạn văn và danh sách ba sở thích."),
            new Chapter("Liên kết và hình ảnh", 3595, "59:55–02:00:58",
                "Thêm liên kết và ảnh có mô tả thay thế.",
                "Thêm một liên kết đến website bạn thích và một ảnh có thuộc tính alt vào trang cá nhân."),
            new Chapter("Cấu trúc ngữ nghĩa và bảng", 7258, "02:00:58–02:40:42",
                "Chia trang bằng các thẻ ngữ nghĩa và trình bày dữ liệu dạng bảng.",
                "Dùng header, main, footer cho trang cá nhân; thêm bảng lịch học có tiêu đề cột."),
            new Chapter("Biểu mẫu và dự án nhỏ", 9642, "Từ 02:40:42 đến hết video",
                "Tạo biểu mẫu và ghép các phần thành một trang hoàn chỉnh.",
                "Hoàn thiện trang giới thiệu có biểu mẫu tên, email, lời nhắn. Kiểm tra label và các liên kết. Biểu mẫu chỉ cần giao diện, chưa cần gửi dữ liệu.")),
        List.of(
            new SaveQuizRequest.Question("Thuộc tính nào mô tả nội dung hình ảnh?",
                List.of("href", "alt", "class", "id"), 1, "alt cung cấp mô tả thay thế cho hình ảnh."),
            new SaveQuizRequest.Question("Thẻ nào biểu thị nội dung chính của trang?",
                List.of("main", "footer", "head", "title"), 0, "main chứa nội dung chính của trang HTML.")));

    create(teacher, "JavaScript nhập môn · Demo YouTube", "PkZNo7MFNFg", "Beau Carnes",
        "Learn JavaScript – Full Course for Beginners",
        "Sử dụng biến, chuỗi và mảng\nViết hàm và điều kiện\nThực hành đối tượng và vòng lặp",
        List.of(
            new Chapter("Làm quen, biến và chuỗi", 0, "00:00–40:44",
                "Chạy JavaScript và sử dụng biến, số, chuỗi.",
                "Mở Console của trình duyệt. Khai báo tên và tuổi; in một lời chào và tính tuổi sau một năm."),
            new Chapter("Mảng và thao tác dữ liệu", 2444, "40:44–51:41",
                "Tạo mảng và thêm, sửa, xóa phần tử.",
                "Tạo mảng ba môn học. Thêm một môn bằng push, xóa môn cuối bằng pop và in kết quả."),
            new Chapter("Hàm và giá trị trả về", 3101, "51:41–01:08:41",
                "Tách thao tác thành hàm có tham số và kết quả trả về.",
                "Viết hàm tinhTong(a, b) trả về tổng. Gọi hàm với số dương, số âm và số 0."),
            new Chapter("Điều kiện và lựa chọn", 4121, "01:08:41–01:49:11",
                "Dùng biểu thức Boolean, if/else và switch.",
                "Viết hàm xếp loại điểm: từ 8 là Tốt, từ 5 là Đạt, còn lại là Cần ôn. Kiểm tra điểm 4, 5 và 8."),
            new Chapter("Đối tượng, vòng lặp và ôn tập", 6551, "Từ 01:49:11; phần ES6 từ 02:36:57 là đọc thêm",
                "Mô tả dữ liệu bằng đối tượng và duyệt danh sách bằng vòng lặp.",
                "Tạo danh sách ba học viên gồm tên và điểm. Duyệt danh sách để in tên và xếp loại bằng hàm của bài trước; tính điểm trung bình.")),
        List.of(
            new SaveQuizRequest.Question("Lệnh nào thêm một phần tử vào cuối mảng?",
                List.of("pop()", "shift()", "push()", "return"), 2, "push() thêm phần tử vào cuối mảng."),
            new SaveQuizRequest.Question("Từ khóa nào trả kết quả từ một hàm?",
                List.of("if", "return", "for", "const"), 1, "return trả kết quả và kết thúc lần gọi hàm.")));

    jdbc.update("INSERT INTO demo_seed_runs (dataset_key) VALUES (?)", KEY);
  }

  private void create(User teacher, String title, String videoId, String author, String sourceTitle,
      String outcomes, List<Chapter> chapters, List<SaveQuizRequest.Question> questions) {
    var course = new Course();
    course.setTeacher(teacher);
    course.setTitle(title);
    course.setCategory("Lập trình");
    course.setLevel("Cơ bản");
    course.setPrice(BigDecimal.ZERO);
    course.setDurationHours(4);
    course.setStatus(CourseStatus.PUBLISHED);
    course.setDescription("Khóa học demo miễn phí cho đồ án, không mang tính thương mại. "
        + "Video tiếng Anh từ freeCodeCamp.org, do " + author + " hướng dẫn; "
        + "chia thành 5 bài với hướng dẫn thực hành bằng tiếng Việt. Video thuộc tác giả/kênh gốc và được phát trực tiếp từ YouTube.");
    course.setLearningOutcomes(outcomes);
    courses.saveAndFlush(course);
    Lesson last = null;
    for (int i = 0; i < chapters.size(); i++) {
      var chapter = chapters.get(i);
      var lesson = new Lesson();
      lesson.setCourse(course);
      lesson.setTitle(chapter.title());
      lesson.setOrderIndex(i + 1);
      lesson.setIsPublished(true);
      lesson.setContentFormat("MARKDOWN");
      lesson.setVideoUrl("https://www.youtube.com/watch?v=" + videoId + "&t=" + chapter.start() + "s");
      lesson.setTextContent("# Mục tiêu\n" + chapter.goal()
          + "\n\n# Cách học\nXem phần **" + chapter.range() + "** của video. "
          + "Các bài trong khóa dùng chung video dài; trình phát mở tại mốc của bài hiện tại. "
          + "Bạn chủ động dừng khi hết phần được hướng dẫn. Video tiếng Anh; có thể bật phụ đề nếu video hỗ trợ."
          + "\n\n# Thực hành\n" + chapter.exercise()
          + "\n\nGhi lại điều chưa rõ trong ghi chú hoặc mục hỏi đáp. Sau khi thực hành, chọn **Đánh dấu hoàn thành** để cập nhật tiến độ."
          + "\n\n# Nguồn video\n[" + sourceTitle + "](https://www.youtube.com/watch?v=" + videoId + ")"
          + " · Kênh **freeCodeCamp.org** · Tác giả **" + author + "**."
          + "\n\nVideo được nhúng tạm cho đồ án/demo phi thương mại. Quyền sở hữu thuộc tác giả/kênh gốc.");
      last = lessons.saveAndFlush(lesson);
    }
    quizzes.save(last.getLessonId(), new SaveQuizRequest(0, "Ôn tập " + title, 50, true, questions),
        new CustomUserDetails(teacher));
  }

  private record Chapter(String title, int start, String range, String goal, String exercise) {}
}
