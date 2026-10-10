# Đánh giá toàn bộ dự án — 10/10/2026

> Cập nhật sau đánh giá: sáu phát hiện bên dưới đã được khắc phục, cùng bảo vệ admin cuối cùng, giới hạn đăng nhập/đăng ký và bổ sung CI/E2E. Xem [biên bản sửa lỗi](SUA_LOI_DANH_GIA_2026_10_10.md). Báo cáo này giữ nguyên bằng chứng của trạng thái trước khi sửa.

## Kết luận

Dự án có mức hoàn thiện tốt cho đồ án và trình diễn LMS: ba vai trò rõ ràng, luồng học và thanh toán chạy xuyên suốt, nội dung phong phú, dữ liệu được lưu trên máy chủ và nhiều tình huống xung đột/rollback đã có kiểm thử. Toàn bộ kiểm thử hiện có đạt trong lần đánh giá này. Tuy nhiên, vẫn có lỗi ở đầu vào mật khẩu, thứ tự chương trình và trạng thái sẵn sàng của demo; cùng các điểm cần cải thiện về hiệu năng, múi giờ và tự động hóa kiểm thử trước khi vận hành thực tế.

Đây là đánh giá trạng thái hiện tại, không phải chứng nhận không còn lỗi hoặc kiểm toán bảo mật. Các phát hiện bên dưới chưa được sửa trong đợt commit này.

## Phạm vi và phương pháp

- Xem cấu trúc, cấu hình, controller, chính sách quyền, entity/repository, các service nghiệp vụ chính, migration, frontend, tài liệu và CI; xem cả mã đã commit và toàn bộ thay đổi đang chờ commit.
- Chạy lại kiểm thử hiện có, đóng gói ứng dụng và kiểm chứng thêm hai trường hợp đầu vào/API trên demo PostgreSQL tạm.
- Quy mô tại thời điểm đánh giá: 172 tệp Java chính (8.329 dòng), 7 tệp Java kiểm thử (3.309 dòng), 17 module JavaScript (4.187 dòng), 19 trang HTML và 20 migration SQL.
- Hai database PostgreSQL riêng được tạo cho backend và E2E rồi xóa sau kiểm tra. Không chạy bộ kiểm thử có thao tác xóa fixture trên database demo đang sử dụng.
- Không thực hiện benchmark tải lớn, thử SMTP thật, kiểm tra đa trình duyệt toàn bộ hoặc khôi phục bản sao lưu. Việc rà soát toàn dự án không đồng nghĩa đã kiểm tra mọi tổ hợp đầu vào và mọi dòng mã.

## Kết quả kiểm thử thực chạy

| Kiểm tra | Kết quả | Giới hạn |
| --- | --- | --- |
| `mvnw.cmd -B -ntp verify` | 138/138 đạt; đóng gói thành công | Lần chạy mặc định dùng H2 |
| Toàn bộ suite với datasource PostgreSQL | 138/138 đạt | 118 ca BusinessFlow, 7 ca PasswordRecovery và 1 ca khởi động dùng PostgreSQL thật; 7 ca cấu hình dùng H2, 2 ca migration dùng H2 riêng, 3 ca recovery configuration dùng mock |
| `node --test tests/unit/*.test.mjs` | 19/19 đạt | Bao phủ API, renderer, QR, phản hồi và wishlist |
| `python -m unittest discover -s tests/unit -p 'test_*.py'` | 6/6 đạt | Kiểm tra bộ nhập nội dung demo |
| `node --check` toàn bộ module JS | 17/17 tệp hợp lệ | Kiểm tra cú pháp, không thay thế kiểm thử hành vi |
| `npm run test:e2e` trên demo PostgreSQL tạm | 6/6 Chromium đạt, 51 giây | Sáu hành trình hiện có; có kiểm tra mobile, HTML literal và lỗi JavaScript chưa xử lý |
| Kiểm chứng thứ tự chương trình qua API | Xác nhận lỗi | Thứ tự yêu cầu/lưu `[234,235]`, thứ tự nhóm trả về `[235,234]` |
| Đăng ký với mật khẩu 25 ký tự `ắ` | Xác nhận lỗi | 75 byte UTF-8; HTTP 401, log chứa lỗi BCrypt quá 72 byte |

Log và báo cáo cục bộ bị Git bỏ qua: `target/project-review-verify.log`, `target/project-review/{h2-reports,postgres-reports,postgres-tests.log,e2e.log,demo-server.log,curriculum-facts.json,review-facts.json}` và `output/e2e-report/`. Số kiểm thử đạt không bao gồm việc khắc phục các trường hợp bổ sung đã phát hiện lỗi.

## Phát hiện cần xử lý

P2: ảnh hưởng chức năng/độ ổn định trong điều kiện cụ thể; P3: lỗi nhất quán hoặc trải nghiệm có phạm vi hẹp.

### 1. [P2] Giới hạn mật khẩu giữa các luồng chưa thống nhất

`RegisterRequest` và `CreateUserRequest` chỉ giới hạn 8–64 ký tự; `ChangePasswordRequest` cũng chỉ giới hạn số ký tự. `UserServiceImpl.createUser()` và `changePassword()` gọi BCrypt trực tiếp. Trong khi đó, recovery kiểm tra thêm giới hạn 72 byte UTF-8.

Đã gửi đăng ký bằng mật khẩu 25 ký tự `ắ` (75 byte), hợp lệ theo giới hạn ký tự. API trả HTTP 401; log máy chủ xác nhận `IllegalArgumentException: password cannot be more than 72 bytes`. Lỗi ở thao tác băm mật khẩu bị đường xử lý lỗi của request chưa đăng nhập biến thành phản hồi xác thực, nên người dùng không nhận được hướng dẫn đúng.

Nên dùng chung quy tắc 72 byte tại backend cho đăng ký, tạo tài khoản và đổi mật khẩu, trả HTTP 400 với thông báo cụ thể; bổ sung kiểm tra tương ứng trên giao diện. Thêm ca hồi quy Unicode cho các luồng này. Nguồn: `dto/request/RegisterRequest.java:19`, `dto/request/CreateUserRequest.java:18`, `dto/request/ChangePasswordRequest.java:15`, `service/impl/UserServiceImpl.java:55` và `:140`, `config/SecurityConfig.java`.

### 2. [P2] Danh sách bài nộp đọc cả dữ liệu tệp đính kèm

`AssignmentService.select()` dùng `SELECT s.*`, bao gồm cột `file_content BYTEA`. Câu truy vấn này được dùng trong danh sách, đọc bài của mình và phản hồi chấm điểm, dù mapper chỉ cần metadata/nội dung văn bản. Tệp chỉ nên được đọc tại endpoint tải tệp.

Với trang API tối đa 50 bài và tệp 5 MiB theo cấu hình mặc định, có thể phải truyền khoảng 250 MiB dữ liệu tệp từ database trong một lần tải danh sách, chưa tính overhead. Đây là suy luận từ truy vấn và giới hạn dung lượng, chưa phải kết quả benchmark hoặc đo heap. Giao diện mặc định tải 5 bài/trang nên phạm vi thường gặp nhỏ hơn nhưng vẫn đọc tệp không cần thiết.

Nên liệt kê các cột cần thiết, tách truy vấn tải tệp và đo lại bằng dữ liệu có đính kèm. Nguồn: `service/AssignmentService.java:94` và `:143`; `db/migration/V18__chapters_assignments_and_reporting.sql`; giá trị mặc định tại V13.

### 3. [P2] Điều kiện “demo sẵn sàng” có thể đạt trước khi seed xong

`scripts/start-demo.ps1` coi `/api/auth/config` trả `demo=true` là sẵn sàng. Endpoint này có thể phục vụ khi web server đã mở cổng, còn các `CommandLineRunner` đang tạo dữ liệu.

Trong lần chạy trên database mới, công cụ kiểm chứng đã nhận được API config và thử đăng nhập `admin_demo`, nhưng nhận 401. Thử lại sau khi dữ liệu được tạo thì thành công. Sáu E2E hiện có vẫn đạt; tình huống này cho thấy điều kiện sẵn sàng chưa bảo đảm tài khoản/danh mục demo đã tồn tại.

Nên chỉ báo sẵn sàng sau khi các runner hoàn tất, chẳng hạn cờ được đặt ở `ApplicationReadyEvent` hoặc một readiness endpoint phù hợp. Không dùng thời gian chờ cố định làm điều kiện đảm bảo. Nguồn: `scripts/start-demo.ps1:23`, `config/DemoData.java:29`, `config/DemoScenarioRunner.java:23`, `controller/AuthController.java:47`.

### 4. [P2, có điều kiện] Các nguồn thời gian chưa dùng chung múi giờ

Weekly goal, audit và recovery dùng `Asia/Ho_Chi_Minh`; enrollment, payment, progress, entity và thời gian chấm bài dùng `LocalDateTime.now()` theo JVM, còn thời gian nộp bài mặc định dùng `CURRENT_TIMESTAMP` của database. Các cột lưu `TIMESTAMP` không mang offset.

Nếu JVM/database chạy ở UTC hoặc múi giờ khác, cùng một hoạt động có thể được ghi bằng hai giờ địa phương khác nhau. Bộ lọc ngày, lịch sử admin và mục tiêu tuần có nguy cơ không khớp ở ranh giới ngày/tuần. Chưa tái hiện trên môi trường đa múi giờ trong đợt này; đây là phát hiện từ mã và điều kiện triển khai.

Nên thống nhất cách lưu thời điểm và quy ước hiển thị, dùng `Clock` có thể cấu hình/tiêm vào để kiểm tra ranh giới ngày và tuần. Nếu vẫn lưu giờ địa phương thì phải cấu hình cùng múi giờ cho JVM và database. Nguồn: `service/WeeklyGoalService.java:27` và `:62`, `service/AuditLogService.java:30`, `service/impl/PaymentServiceImpl.java`, `service/impl/EnrollmentServiceImpl.java`, `service/AssignmentService.java:107`, migration V18.

### 5. [P3] API chương trình không giữ nhất quán thứ tự nhóm chưa chia chương

`CurriculumService.save()` cho phép nhóm chưa chia chương ở đầu/giữa và gán `orderIndex` theo đúng vị trí trong request. `response()` luôn đưa nhóm đó về cuối. Đã tái hiện trên khóa thử riêng: request và danh sách bài lưu theo `[234,235]`, còn nhóm trả về theo `[235,234]`.

Giao diện hiện tại giữ nhóm chưa chia chương ở cuối nên lỗi chủ yếu ảnh hưởng client gọi API trực tiếp hoặc tích hợp mới. Nên quy định rõ nhóm này phải ở cuối và từ chối/chuẩn hóa trước khi ghi, hoặc giữ cùng một thứ tự trong toàn bộ các response. Nguồn: `service/CurriculumService.java:44`, `:54`, `:77`; bằng chứng `target/project-review/curriculum-facts.json`.

### 6. [P3] Trang chi tiết dành cho khách chưa hiển thị tên chương

Tài liệu chức năng nói danh sách công khai và phòng học đều hiện tên chương từng bài. API public đã có `chapterTitle`, màn hình sau đăng nhập và phòng học dùng trường này, nhưng template trang chi tiết cho khách chỉ hiển thị tiêu đề bài và thông báo khóa nội dung.

Nên bổ sung nhãn chương ở giao diện khách và thêm hành trình khách xem khóa có chương. Đây là phát hiện từ template, chưa chạy riêng một hành trình UI cho trường hợp này. Nguồn: `src/main/resources/static/js/app.js:469`, đối chiếu `:1129` và `:1489`; `docs/CHUONG_BAI_TAP_VA_BAO_CAO.md:7`.

## Đánh giá theo các phần của dự án

| Phần | Điểm mạnh | Việc còn cần làm |
| --- | --- | --- |
| Sản phẩm | Luồng khách → đăng ký/thanh toán → học → tiến độ/đánh giá; có công cụ giảng viên và quản trị | Các tính năng mới cần E2E bền vững; khôi phục SMTP thật chưa được kiểm tra |
| Kiến trúc backend | Phân tầng controller/service/repository/DTO, có `ContentPolicy`, transaction và migration | Quy tắc thời gian/validation chưa tập trung; service mới pha JPA và JDBC cần kiểm soát truy vấn/khóa |
| Quyền truy cập | Method security, kiểm tra chủ khóa/lượt đăng ký, CSRF, thu hồi phiên bằng auth version | Login/register chưa có limiter riêng như recovery; cần quy tắc tránh khóa/xóa admin hoạt động cuối cùng trước khi vận hành |
| Học tập và nội dung | Markdown escape HTML, quiz có phiên bản, hỏi đáp nhiều lượt, ghi chú riêng, bản nháp có revision | Tiến độ là tự đánh dấu; quiz/bài tập không tự quyết định hoàn thành theo thiết kế hiện có |
| Dữ liệu | 20 migration, FK/unique/index, chống gửi trùng và giữ bản đề khi nộp | Tệp trong database làm tăng dung lượng backup; xóa bài có thể xóa bài nộp/quiz/hỏi đáp theo cascade, cần quy trình lưu trữ |
| Thanh toán/báo cáo | Duyệt thủ công, mã đối chiếu riêng, báo cáo tổng hợp tránh nhân doanh thu, CSV có BOM/escape | Chưa có đối soát ngân hàng tự động; báo cáo hoàn thành là trạng thái hiện tại của nhóm đăng ký, không phải số hoàn thành trong khoảng |
| Frontend | Module riêng cho tính năng mới, trạng thái pending/retry, cảnh báo bản soạn, hỗ trợ mobile | `app.js` có 2.199 dòng, nhiều template HTML lớn; nên tách theo trang để việc sửa và kiểm thử dễ hơn |
| Kiểm thử | Có negative case, quyền riêng tư, CSRF, idempotency, cạnh tranh ghi và rollback; chạy được PostgreSQL thật | Một lớp BusinessFlow chứa 118 ca; nên chia theo miền nghiệp vụ và thêm kiểm thử ranh giới thời gian/Unicode |
| CI/vận hành | Có GitHub Actions: H2, PostgreSQL, Node và Chromium; cấu hình bí mật qua biến môi trường | CI chưa chạy 6 ca Python và chưa kiểm tra cú pháp toàn bộ các module JS mới; chưa có bằng chứng tải lớn, giám sát hay diễn tập restore |
| Tài liệu | README và tài liệu theo tính năng, có hướng dẫn demo và biên bản kiểm thử | Đồng bộ mô tả chương công khai với giao diện, cập nhật số Node test ở phần README cũ (vẫn ghi 15 thay vì 19) |

Các mục cải thiện về limiter, admin cuối cùng, lưu trữ và vận hành là khuyến nghị theo phạm vi triển khai; không được tính là lỗi đã khai thác/tái hiện trong lần này. Session hiện lưu tại ứng dụng, do đó triển khai nhiều instance hoặc muốn giữ phiên qua restart cần thiết kế thêm.

## Thứ tự ưu tiên tiếp theo

1. Sửa validation mật khẩu Unicode và phản hồi lỗi; chốt readiness của demo và hợp đồng thứ tự curriculum, kèm ca hồi quy.
2. Bỏ BLOB khỏi truy vấn danh sách/chấm bài; thống nhất thời gian và kiểm tra ở múi giờ UTC, sát ranh giới tuần.
3. Bổ sung E2E wishlist, chương, nộp/chấm bài, weekly goal, audit và recovery; đưa Python cùng toàn bộ kiểm tra cú pháp JS vào CI.
4. Tách frontend theo trang và test backend theo miền; kiểm tra backup/restore, dung lượng tệp, nhật ký và kiểm soát đăng nhập trước khi triển khai cho người dùng thật.

Trạng thái phù hợp hiện tại: đồ án/demo với bộ kiểm thử nền tảng tốt. Quyết định vận hành thực tế cần dựa thêm vào việc xử lý các phát hiện và kiểm tra môi trường triển khai cụ thể.
