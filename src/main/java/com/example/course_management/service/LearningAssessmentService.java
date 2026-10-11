package com.example.course_management.service;

import com.example.course_management.exception.*;
import com.example.course_management.repository.UserRepository;
import com.example.course_management.security.CustomUserDetails;
import jakarta.validation.constraints.*;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LearningAssessmentService {
  public static final int VERSION = 1;
  public record Submit(@Min(1) int version, @NotBlank String goal,
      @Min(1) @Max(20) int hoursPerWeek,
      @NotNull @Size(min=8,max=8) List<@NotNull @Min(-1) @Max(3) Integer> answers) {}
  public record Question(String id, String topic, String text, List<String> options) {}
  public record Goal(String id, String title, List<Question> questions) {}
  public record Definition(int version, List<Goal> goals) {}
  public record Review(String topic, String question, int selected, int correct, String explanation) {}
  public record CourseLink(int courseId, String title, String level, int durationHours) {}
  public record Step(String title, String description, int estimatedHours, int weeks, List<CourseLink> courses) {}
  public record Result(int version, String goal, String goalTitle, int hoursPerWeek,
      int score, int total, int foundationScore, int subjectScore, String level,
      String summary, LocalDateTime completedAt, List<Review> reviews, List<Step> steps) {}
  private record Item(String topic, String text, List<String> options, int correct, String explanation) {}
  private final JdbcTemplate jdbc;
  private final UserRepository users;
  private final Clock clock;
  public LearningAssessmentService(JdbcTemplate jdbc, UserRepository users, Clock clock) {
    this.jdbc=jdbc; this.users=users; this.clock=clock;
  }
  private static Item q(String topic,String text,int correct,String explanation,String... options) {
    return new Item(topic,text,List.of(options),correct,explanation);
  }
  // Version 1 is immutable: saved answers must always be graded against the original bank.
  private static final List<Item> CORE = List.of(
      q("Biến và dữ liệu","Để giữ giá trị 7,5 mà không mất phần thập phân, nên chọn kiểu nào?",1,"Kiểu số thực lưu được phần thập phân.","Số nguyên","Số thực","Boolean","Không cần kiểu dữ liệu"),
      q("Điều kiện","Điểm hợp lệ nằm từ 0 đến 10. Điều kiện nào kiểm tra đúng?",2,"Cần đồng thời thỏa mãn cả cận dưới và cận trên.","điểm >= 0 HOẶC điểm <= 10","điểm > 10","điểm >= 0 VÀ điểm <= 10","điểm < 0"),
      q("Kiểm thử","Khi tính trung bình một danh sách, trường hợp nào cần kiểm tra để tránh chia cho 0?",0,"Danh sách rỗng có số phần tử bằng 0; cần xử lý trước khi chia.","Danh sách rỗng","Danh sách có hai phần tử","Danh sách chỉ có số dương","Danh sách đã sắp xếp"),
      q("Git","Trước khi tạo commit, lệnh nào đưa tệp đã sửa vào staging?",3,"git add chọn các thay đổi sẽ có trong commit kế tiếp.","git log","git status","git clone","git add")
  );
  private static final Map<String,List<Item>> SUBJECTS = Map.of(
      "WEB",List.of(
          q("HTML","Thẻ nào phù hợp cho một liên kết tới trang khóa học?",0,"Thẻ a với thuộc tính href tạo liên kết điều hướng.","<a href=\"/courses.html\">Khóa học</a>","<div href=\"/courses.html\">Khóa học</div>","<span>Khóa học</span>","<p src=\"/courses.html\">Khóa học</p>"),
          q("CSS","Muốn bố cục thay đổi theo chiều rộng màn hình, nên dùng gì?",2,"Media query áp dụng quy tắc CSS theo điều kiện màn hình.","Chỉ tăng cỡ chữ","Thẻ title","Media query","Đổi tên tệp HTML"),
          q("JavaScript","Lệnh nào lắng nghe thao tác nhấn một nút?",1,"addEventListener đăng ký hàm xử lý sự kiện click.","button.toString()","button.addEventListener('click', handler)","button.length","button.sort()"),
          q("Form và API","Kiểm tra dữ liệu ở trình duyệt đã đủ để bảo vệ API chưa?",3,"Máy chủ phải kiểm tra đầu vào vì người dùng có thể gửi request trực tiếp.","Đủ nếu ô nhập required","Đủ nếu dùng HTTPS","Đủ nếu ẩn nút gửi","Chưa, máy chủ vẫn phải kiểm tra")
      ),
      "JAVA",List.of(
          q("Java","Công cụ nào biên dịch tệp .java thành bytecode?",1,"javac biên dịch mã nguồn Java; java chạy chương trình đã biên dịch.","git","javac","npm","SELECT"),
          q("Java","Để so sánh nội dung hai chuỗi Java, nên dùng gì?",0,"equals so sánh nội dung chuỗi; == so sánh tham chiếu.","equals()","== trong mọi trường hợp","=","compareMemory()"),
          q("Hướng đối tượng","Lớp (class) thường dùng để làm gì?",3,"Lớp mô hình hóa dữ liệu và hành vi của một loại đối tượng.","Thay thế mọi vòng lặp","Chỉ lưu một số nguyên","Chỉ viết truy vấn SQL","Mô hình hóa thuộc tính và hành vi"),
          q("API và transaction","Hai thao tác lưu cần cùng thành công hoặc cùng hoàn tác. Nên dùng gì?",2,"Transaction giúp giữ tính nguyên tử cho nhóm thao tác dữ liệu.","Hai nút bấm","CSS","Transaction","Chỉ ghi log")
      ),
      "DATA",List.of(
          q("SQL","Câu lệnh nào lấy dữ liệu từ bảng courses?",2,"SELECT đọc các cột từ bảng được chỉ định bởi FROM.","DELETE FROM courses","DROP TABLE courses","SELECT * FROM courses","UPDATE courses"),
          q("Lọc dữ liệu","Mệnh đề nào lọc các hàng trước khi tổng hợp?",0,"WHERE lọc hàng trước GROUP BY; HAVING lọc các nhóm sau tổng hợp.","WHERE","ORDER BY","HAVING","LIMIT"),
          q("Nối bảng","Muốn giữ mọi học viên kể cả người chưa đăng ký, nên nối students với enrollments bằng gì?",3,"LEFT JOIN giữ mọi hàng của bảng bên trái, dù không có hàng khớp bên phải.","INNER JOIN","Chỉ ORDER BY","Chỉ DISTINCT","LEFT JOIN"),
          q("Tổng hợp","Muốn đếm lượt đăng ký theo course_id, câu nào phù hợp?",1,"GROUP BY tạo nhóm theo khóa học và COUNT(*) đếm hàng trong mỗi nhóm.","SELECT course_id FROM enrollments","SELECT course_id, COUNT(*) FROM enrollments GROUP BY course_id","DELETE FROM enrollments","SELECT COUNT(*) FROM enrollments ORDER BY course_id")
      )
  );
  private static final Map<String,String> TITLES = Map.of("WEB","Phát triển web","JAVA","Java & Backend","DATA","SQL & phân tích dữ liệu");
  private List<Item> bank(String goal) {
    if (!SUBJECTS.containsKey(goal)) throw new BadRequestException("Hướng học không hợp lệ");
    var items=new ArrayList<>(CORE); items.addAll(SUBJECTS.get(goal)); return items;
  }
  @Transactional(readOnly=true)
  public Definition definition() {
    return new Definition(VERSION,List.of("WEB","JAVA","DATA").stream().map(id -> {
      var items=bank(id); var questions=new ArrayList<Question>();
      for(int i=0;i<items.size();i++) {
        var item=items.get(i); questions.add(new Question(id+"-"+i,item.topic(),item.text(),item.options()));
      }
      return new Goal(id,TITLES.get(id),questions);
    }).toList());
  }
  public Result submit(Submit request,CustomUserDetails actor) {
    if(request.version()!=VERSION) throw new ConflictException("Đề kiểm tra đã thay đổi. Tải lại trang để làm đề mới.");
    bank(request.goal());
    if(request.hoursPerWeek()<1||request.hoursPerWeek()>20||request.answers()==null||request.answers().size()!=8
        ||request.answers().stream().anyMatch(a->a==null||a < -1||a > 3))
      throw new BadRequestException("Cần trả lời đủ 8 câu và chọn 1–20 giờ học mỗi tuần");
    int id=actor.getUser().getUserId();
    users.findLockedById(id).orElseThrow(()->new ForbiddenException("Tài khoản không tồn tại"));
    var now=com.example.course_management.time.ApplicationTime.now(clock).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    String answers=String.join(",",request.answers().stream().map(String::valueOf).toList());
    if(jdbc.update("UPDATE learning_assessments SET version=?,goal=?,hours_per_week=?,answers=?,completed_at=? WHERE student_id=?",
        VERSION,request.goal(),request.hoursPerWeek(),answers,now,id)==0)
      jdbc.update("INSERT INTO learning_assessments(student_id,version,goal,hours_per_week,answers,completed_at) VALUES(?,?,?,?,?,?)",
          id,VERSION,request.goal(),request.hoursPerWeek(),answers,now);
    return result(request,now);
  }
  @Transactional(readOnly=true)
  public Result latest(CustomUserDetails actor) {
    var rows=jdbc.query("SELECT version,goal,hours_per_week,answers,completed_at FROM learning_assessments WHERE student_id=?",
        (r,n)->result(new Submit(r.getInt(1),r.getString(2),r.getInt(3),
            Arrays.stream(r.getString(4).split(",")).map(Integer::valueOf).toList()),r.getTimestamp(5).toLocalDateTime()),actor.getUser().getUserId());
    return rows.isEmpty()?null:rows.getFirst();
  }
  private Result result(Submit request,LocalDateTime at) {
    var items=bank(request.goal()); var reviews=new ArrayList<Review>(); int core=0,subject=0;
    for(int i=0;i<items.size();i++) {
      var item=items.get(i); int answer=request.answers().get(i);
      if(answer==item.correct()) {if(i<4) core++; else subject++;}
      reviews.add(new Review(item.topic(),item.text(),answer,item.correct(),item.explanation()));
    }
    int score=core+subject;
    String level=score>=7?"Sẵn sàng thực hành":score>=4?"Đang xây nền tảng":"Bắt đầu từ nền tảng";
    var steps=new ArrayList<Step>();
    if(core<3) steps.add(step("Củng cố tư duy lập trình","Ôn biến, điều kiện, xử lý dữ liệu rỗng và Git. Viết ví dụ nhỏ và tự kiểm thử trước khi học tiếp.",12,request.hoursPerWeek(),"git"));
    String goal=request.goal();
    if(subject<3) steps.add(switch(goal) {
      case "WEB" -> step("Nền tảng HTML, CSS và JavaScript","Tạo trang có cấu trúc rõ ràng, bố cục responsive và xử lý sự kiện/form.",20,request.hoursPerWeek(),"html","javascript");
      case "JAVA" -> step("Java căn bản và hướng đối tượng","Chạy chương trình Java, làm việc với chuỗi, lớp, hàm và danh sách.",20,request.hoursPerWeek(),"java");
      default -> step("SQL căn bản","Luyện SELECT, WHERE, JOIN và GROUP BY trên bộ dữ liệu nhỏ; kiểm tra số hàng và giá trị thiếu.",20,request.hoursPerWeek(),"sql");
    });
    steps.add(switch(goal) {
      case "WEB" -> step("Dự án web thực hành","Xây trang chọn khóa học và form đăng ký, kiểm tra bàn phím và màn hình điện thoại. Nếu còn vướng, quay lại phần nền tảng.",16,request.hoursPerWeek(),"website");
      case "JAVA" -> step("Dự án Java quản lý học viên","Dùng lớp và danh sách để tính điểm, tìm học viên và từ chối dữ liệu sai. Hoàn thành bước này trước khi chuyển sang API.",16,request.hoursPerWeek(),"java");
      default -> step("Báo cáo dữ liệu bằng SQL","Viết báo cáo lượt đăng ký và học phí, đối chiếu JOIN để tránh đếm trùng; giải thích kết quả bằng dữ liệu mẫu.",16,request.hoursPerWeek(),"sql");
    });
    steps.add(switch(goal) {
      case "WEB" -> step("Hoàn thiện trải nghiệm và quy trình nhóm","Cải thiện luồng tác vụ, thông báo lỗi và khả năng tiếp cận; quản lý thay đổi bằng Git.",12,request.hoursPerWeek(),"thiết kế giao diện","git");
      case "JAVA" -> step("Tiến tới Spring Boot API","Sau khi làm vững dự án Java, học DTO, validation, repository và transaction; xây API có kiểm thử.",24,request.hoursPerWeek(),"spring boot");
      default -> step("Dự án phân tích độc lập","Chọn một câu hỏi kinh doanh, làm sạch dữ liệu, viết truy vấn và trình bày báo cáo có kiểm tra kết quả.",12,request.hoursPerWeek(),"sql");
    });
    String summary=core<3?"Ưu tiên củng cố kiến thức chung trước khi đi sâu vào hướng học đã chọn.":subject<3?"Bạn đã có nền tảng chung; hãy bổ sung kiến thức chuyên môn rồi làm dự án.":"Bạn có thể bắt đầu bằng dự án thực hành và ôn lại các câu chưa đúng khi cần.";
    return new Result(request.version(),goal,TITLES.get(goal),request.hoursPerWeek(),score,8,core,subject,level,summary,at,reviews,steps);
  }
  private Step step(String title,String description,int hours,int weekly,String... keywords) {
    // Only public metadata from currently published courses; no lesson content or enrollment changes.
    String filters=String.join(" OR ",Arrays.stream(keywords).map(k -> k.equals("java")
        ? "(LOWER(title) LIKE ? AND LOWER(title) NOT LIKE '%javascript%')" : "LOWER(title) LIKE ?").toList());
    Object[] values=Arrays.stream(keywords).map(k->"%"+k+"%").toArray();
    var courses=jdbc.query("SELECT course_id,title,level,duration_hours FROM courses WHERE status='PUBLISHED' AND ("+filters+") ORDER BY course_id LIMIT 3",
        (r,n)->new CourseLink(r.getInt(1),r.getString(2),r.getString(3),r.getInt(4)),values);
    return new Step(title,description,hours,(hours+weekly-1)/weekly,courses);
  }
}
