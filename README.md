# Course Management — Học để tiến xa

Nền tảng quản lý khóa học trực tuyến phục vụ đồ án/demo. Backend Spring Boot, Java 21, PostgreSQL; giao diện HTML/CSS/JavaScript có bố cục chung, hỗ trợ màn hình nhỏ. Thanh toán chuyển khoản được admin đối chiếu và duyệt thủ công.

## Chức năng

- Khách: tìm khóa từ trang chủ, khám phá chủ đề, xem kết quả học tập/chương trình/học phí trước khi đăng nhập; giữ khóa đã chọn qua đăng nhập.
- Học viên: dashboard tiếp tục học, tìm/lọc khóa đã đăng ký, đăng ký và thanh toán, học từng bài với chế độ tập trung, lưu ghi chú riêng và vị trí học trên máy chủ, theo dõi tiến độ, đánh giá, thông báo và hồ sơ.
- Hỏi đáp theo bài: học viên đã đăng ký đặt câu hỏi và đọc trao đổi trong khóa; giảng viên phụ trách/admin trả lời, cập nhật phản hồi, ẩn/hiện câu hỏi. Thông báo dẫn tới đúng bài và câu hỏi, kể cả câu hỏi nằm ngoài trang đầu.
- Quiz theo bài: giảng viên soạn 1–20 câu trắc nghiệm bốn lựa chọn, đáp án và giải thích; lưu nháp/xuất bản, đặt mức đạt. Học viên nhận điểm và phản hồi từng câu, xem lịch sử phân trang; giảng viên xem thống kê câu hay sai của phiên bản hiện tại. Mỗi lần sửa giữ đề cũ để lịch sử luôn đúng.
- Giảng viên: dashboard riêng và checklist chuẩn bị khóa, sửa thông tin/mục tiêu của khóa phụ trách, thêm/sửa/xóa bài học, xuất bản/ẩn bài học. Học phí/phân công và xuất bản khóa do admin quản lý.
- Chi tiết thao tác: lỗi ngay dưới ô nhập, hiện/ẩn mật khẩu, số ký tự nội dung, trạng thái đang lưu; cảnh báo khi đóng hộp soạn có thay đổi chưa lưu. Giảng viên xem trước bài trước khi lưu. Menu điện thoại có nền che, hỗ trợ Escape và giữ focus trong điều hướng.
- Admin: quản lý người dùng và khóa học, xuất bản/lưu trữ, xác nhận/từ chối thanh toán, tạo thông báo và xem báo cáo.
- Quyền học được kiểm tra ở backend; danh sách chương trình của người chưa đăng ký chỉ trả metadata, không trả nội dung hoặc URL tài liệu.
- Danh mục khóa học phân trang ở máy chủ, tìm theo tên/mô tả/giảng viên, lọc chủ đề, học phí và trạng thái; bộ lọc và trang hiện tại được lưu trong URL.

## Chạy bản demo

Cần JDK 21 trở lên và PostgreSQL đang chạy. Maven Wrapper tải Maven/dependency trong lần chạy đầu. Tạo database demo trong PostgreSQL:

```sql
CREATE DATABASE course_management_demo;
```

PowerShell:

```powershell
$env:DB_USERNAME = 'postgres'
$env:DB_PASSWORD = 'mat-khau-postgresql-tren-may-ban'
.\mvnw.cmd -B -ntp verify
.\scripts\start-demo.ps1
```

Mở http://127.0.0.1:8080. Script khởi động Java nền, ghi log/PID vào `target/local-demo`. Dừng bằng `scripts/stop-demo.ps1`. Khi thay source, dừng demo, build lại rồi khởi động. Script có tùy chọn `-SkipBuild` nếu đã đóng gói trước đó. Trên máy đang làm việc, cấu hình database cục bộ có thể được đọc từ `target/local-demo/application-local.properties`; file này nằm trong thư mục bị Git bỏ qua.

Linux/macOS hoặc chạy foreground:

```bash
export DB_PASSWORD='mat-khau-postgresql-tren-may-ban'
bash ./mvnw -B -ntp verify
java -jar target/course_management-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo
```

Tài khoản demo có cùng mật khẩu `Demo123!`:

| Vai trò | Tên đăng nhập |
|---|---|
| Admin | admin_demo |
| Giảng viên | teacher_demo |
| Học viên | student_demo |

Profile `demo` tạo tài khoản và nội dung mẫu. Profile mặc định không tạo tài khoản mẫu. Dữ liệu mẫu được giữ qua các lần khởi động; không tự reset database.

Demo có quiz **Java: chuẩn bị môi trường** ở bài **Bắt đầu và chuẩn bị môi trường** của khóa Java. Chỉ tạo khi bài mẫu chưa có quiz, giữ nguyên đề giảng viên đã sửa. Đăng nhập giảng viên để soạn tại trang khóa → Quiz; học viên đã đăng ký làm bài ngay trong phòng học.

Profile demo còn tạo bộ tình huống theo vai trò: 36 khóa mới (xuất bản/nháp/lưu trữ), 26 học viên `student_scenario_1`–`student_scenario_26`, giảng viên `teacher_scenario`, chương trình dài, 12 lượt đăng ký cho `student_demo`, thanh toán chờ/xác nhận/từ chối, tiến độ/ghi chú, 26 đánh giá, câu hỏi và bài làm quiz. Mật khẩu mẫu vẫn là `Demo123!`; bốn học viên mẫu cuối bị khóa để kiểm tra bộ lọc. Dữ liệu được tạo qua nghiệp vụ, lưu PostgreSQL. Flyway V8 ghi nhận bộ mẫu đã hoàn tất; khởi động lại không tạo trùng, không khôi phục dữ liệu đã xóa hoặc ghi đè phần đã sửa. Profile thông thường không chạy bộ mẫu.

## Cấu hình và database

`application.properties` đọc `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `PORT`, `BANK_NAME`, `BANK_ACCOUNT`, `BANK_HOLDER` từ môi trường. `.env.example` chỉ minh họa biến; Spring Boot không tự đọc file `.env`. Hãy export biến hoặc dùng file cấu hình bên ngoài. Không đưa thông tin đăng nhập thật vào repo.

Flyway quản lý schema qua `src/main/resources/db/migration`, Hibernate chỉ validate. Database mới được tạo bảng tự động. Database cũ được baseline ở phiên bản 0, giữ dữ liệu và thêm metadata/chỉ mục. Nếu dữ liệu cũ đã có đăng ký/tiến độ/đánh giá trùng, migration sẽ dừng ở unique index; cần rà soát dữ liệu trùng trước khi chạy tiếp. Không xóa tự động dữ liệu cũ.

## Kiểm tra

```powershell
.\mvnw.cmd -B -ntp verify
node --check src/main/resources/static/js/api.js
node --check src/main/resources/static/js/app.js
node --check src/main/resources/static/js/experience.js
node --check src/main/resources/static/js/questions.js
node --check src/main/resources/static/js/quiz.js
```

Bộ backend có 74 kiểm thử (73 ca nghiệp vụ/API và một ca khởi động), kiểm tra trên H2 và PostgreSQL với migration thật. Bao gồm quiz/phiên bản/chấm điểm/lịch sử riêng/nộp đồng thời, hỏi đáp/ẩn câu hỏi, phân trang, thông báo và rollback, khám phá công khai, phiên khách, chỉnh sửa metadata của giảng viên, ghi chú riêng/khôi phục bài, danh mục, bộ lọc, thống kê và giới hạn số truy vấn. Database kiểm thử được xóa dữ liệu trước mỗi ca; chỉ dùng database chuyên biệt `course_management_test`, tuyệt đối không trỏ test vào database demo hoặc dữ liệu cần giữ.

Kiểm thử giao diện với Chromium: đăng nhập lỗi/thành công, đăng ký và học đến 100%, học khóa trả phí sau khi admin duyệt, chống thực thi HTML trong hồ sơ, màn hình điện thoại và soạn bài trên khóa nháp của giảng viên.

```powershell
npm ci
npx playwright install chromium
# Khi demo đã chạy ở localhost:8080:
$env:E2E_BASE_URL = 'http://127.0.0.1:8080'
npm run test:e2e
```

Nếu không đặt `E2E_BASE_URL`, Playwright tự chạy JAR đã build với profile demo; cần cấu hình database bằng biến môi trường. E2E tạo tài khoản/khóa học mẫu mới mỗi lần chạy, không xóa dữ liệu demo. Báo cáo HTML ở `output/e2e-report/index.html`; ca lỗi có screenshot và trace ở `output/e2e-results`.

GitHub Actions chạy backend trên cả H2 và PostgreSQL, kiểm tra cú pháp JavaScript, rồi chạy sáu hành trình trên Chromium và lưu báo cáo. Xem [kế hoạch kiểm thử](docs/KE_HOACH_KIEM_THU.md) để đối chiếu lỗi, test và tiêu chí nghiệm thu.

## Quy tắc nghiệp vụ

- Khóa có phí phải qua thanh toán được xác nhận. Xác nhận thanh toán, cấp quyền học và tạo thông báo nằm trong một transaction.
- Khóa dữ liệu theo học viên giúp yêu cầu đồng thời không tạo nhiều thanh toán/đăng ký; database có unique index cho enrollment, lesson progress và review.
- Chỉ bài published của khóa học được tính vào tiến độ. Khi thay đổi chương trình, tiến độ/trạng thái hoàn thành được tính lại. Hoàn thành lặp lại không tạo thêm bản ghi.
- Khóa tài khoản, đổi quyền hoặc đổi mật khẩu làm phiên cũ hết hiệu lực ở request tiếp theo. Đăng nhập đổi session ID; thao tác ghi dùng CSRF token.
- Nội dung người dùng được escape trước khi render HTML; liên kết bài học chỉ chấp nhận HTTP/HTTPS, thông báo chỉ liên kết tới trang nội bộ.
- Xóa dữ liệu đang được tham chiếu trả lỗi 409. Dùng khóa tài khoản/lưu trữ khóa học để giữ lịch sử.

## API danh mục khóa học

Khách dùng `GET /api/discovery/catalog` với `search`, `category`, `freeOnly`, `sort`, `page`, `size`; `GET /api/discovery/categories`; và `GET /api/discovery/courses/{id}`. Các API này luôn chỉ trả khóa PUBLISHED cùng metadata chương trình, không trả nội dung/URL bài học. `GET /api/auth/session` trả hồ sơ phiên hiện tại hoặc `null` cho khách.

`GET /api/courses/catalog` yêu cầu đăng nhập, trả `data` gồm `content`, `page`, `size`, `totalElements`, `totalPages`. API đánh số trang từ **0**; URL giao diện đánh số từ **1**.

| Tham số | Mặc định | Ý nghĩa |
|---|---|---|
| `page`, `size` | `0`, `9` | Trang và số khóa/trang; `size` từ 1 đến 100 |
| `search` | rỗng | Tìm không phân biệt hoa/thường theo tên, mô tả hoặc tên giảng viên; tối đa 255 ký tự; `%`, `_` được tìm như ký tự thường |
| `teacherId` | rỗng | Lọc khóa do một giảng viên phụ trách |
| `category` | rỗng | Lọc đúng chủ đề; tối đa 255 ký tự |
| `freeOnly` | `false` | Chỉ khóa miễn phí |
| `status` | rỗng | `DRAFT`, `PUBLISHED`, `ARCHIVED`, trong phạm vi quyền xem |
| `sort` | `new` | `new` (ID giảm dần), `price` (học phí tăng), `title` (tên tăng theo collation database); ID giảm dần làm tiêu chí phụ |

Ví dụ: `/api/courses/catalog?page=0&size=9&freeOnly=true&sort=price`.

`GET /api/courses/categories?teacherId=...` trả chủ đề của các khóa có quyền xem; không bị giới hạn bởi trang hiện tại. Học viên chỉ thấy khóa published; giảng viên còn thấy khóa nháp/lưu trữ của mình; admin thấy tất cả. Quyền xem áp dụng trước khi tính tổng và phân trang.

`GET /api/courses` vẫn trả list cho các màn hình/API hiện có. Danh mục mới tải giảng viên cùng khóa học và dùng ba truy vấn thống kê theo nhóm, tối đa năm truy vấn cho một trang có dữ liệu. Flyway V3 bổ sung chỉ mục phục vụ bộ lọc và thống kê theo khóa học.

Các danh sách nghiệp vụ dùng `/api/lists`, phân trang từ 0, size 1–100. Quyền và bộ lọc được áp dụng tại database trước khi tính tổng; thứ tự có ID làm tiêu chí phụ để ổn định khi thời gian trùng nhau. V9 thêm chỉ mục cho các danh sách này; API list cũ còn được giữ để tương thích.

- `GET users`: admin, `search`, `role`, `status=active|inactive`.
- `GET payments`: admin xem toàn bộ, học viên chỉ xem của mình; `search`, `status=PENDING|CONFIRMED|REJECTED`, `sort=new|old`.
- `GET enrollments`: học viên, `search`, `status=ENROLLED|COMPLETED|DROPPED`, `sort=new|progress|title`; trả metadata khóa trong cùng trang bằng truy vấn thống kê theo nhóm.
- `GET notifications`: thông báo của phiên hiện tại, `status=all|unread|read`. `PUT notifications/read-all` đánh dấu đã đọc cho chính người đó, yêu cầu CSRF.
- `GET summary`: tổng toàn bộ dữ liệu theo phạm vi vai trò, không phụ thuộc trang đang mở.
- `GET course-state/{id}`: đăng ký và thanh toán đang chờ của học viên cho đúng khóa đó.
- `GET reviews?courseId=...`: đánh giá theo quyền xem khóa; `GET own-review/{id}`: đánh giá của chính học viên, kể cả khi nằm ngoài trang đầu.
- `GET student-report/{id}` và `GET teacher-report/{id}`: admin; tổng toàn bộ và danh sách khóa phân trang, không truy vấn lặp theo từng khóa.

## Phạm vi bản nền

Xem [đánh giá trải nghiệm theo từng vai trò và lộ trình sản phẩm](docs/PRODUCT_EXPERIENCE.md). Các danh sách nghiệp vụ đã phân trang ở máy chủ; chương trình/điều hướng bài vẫn tải metadata đầy đủ và phân trang trên giao diện. Quiz hiện hỗ trợ một đáp án đúng trong bốn lựa chọn; bài tập tự luận, lịch thi và tiêu chí cấp chứng nhận thuộc giai đoạn sau. Khi triển khai rộng cần thêm email xác minh/quên mật khẩu, thanh toán qua cổng, audit log và quan sát vận hành. Bản hiện tại dùng chuyển khoản đối chiếu thủ công.

## Nội dung, video và tài liệu bài học

Bài cũ giữ định dạng `TEXT`. Giảng viên chọn `MARKDOWN` để soạn tiêu đề (`#`–`###`), danh sách, chữ đậm, mã inline/fenced, liên kết và ảnh có mô tả. HTML luôn được escape. Preview và phòng học dùng cùng bộ hiển thị. `videoUrl` hỗ trợ YouTube qua youtube-nocookie, MP4/WebM bằng player gốc; nguồn khác hiện liên kết ngoài. Không tự phát video hoặc tải URL từ backend.

Lưu bài nháp trước để tải file từ trình soạn. Mỗi bài tối đa 20 file, 5 MB/file, nhận PDF, TXT UTF-8, PNG/JPG/WebP và kiểm tra chữ ký/định dạng. File được lưu trong PostgreSQL qua V10; sao lưu database bao gồm tài liệu. Giới hạn này phù hợp demo, cần cân nhắc object storage khi có nhiều nội dung. Tải/xóa file có hiệu lực ngay; xóa ảnh đã chèn làm liên kết ảnh không còn tồn tại.

- `GET/POST /api/lessons/{id}/resources`: danh sách metadata hoặc upload multipart `file`; upload chỉ dành cho admin/giảng viên phụ trách và yêu cầu CSRF.
- `GET /api/lesson-resources/{id}/content`: chỉ người quản lý hoặc học viên còn đăng ký và bài published. PDF/TXT tải xuống, ảnh được hiển thị inline; response không cache và có nosniff/sandbox.
- `DELETE /api/lesson-resources/{id}`: người quản lý khóa, yêu cầu CSRF. Xóa bài xóa tài liệu của bài qua khóa ngoại.
- `node --test tests/unit/*.test.mjs`: kiểm tra bộ hiển thị và URL không an toàn.

## API quiz theo bài

- `GET /api/lessons/{id}/quiz`: giảng viên phụ trách/admin đọc đề mới nhất gồm đáp án/giải thích; học viên đã đăng ký chỉ nhận đề xuất bản và các lựa chọn, đáp án/giải thích được bỏ khỏi JSON. Chưa có đề xuất bản trả `data: null`.
- `PUT /api/lessons/{id}/quiz`: người quản lý khóa lưu `{ expectedRevision, title, passPercentage, published, questions }`. Đề đầu dùng `expectedRevision: 0`; mỗi câu có `prompt`, `options` gồm đúng bốn chuỗi khác nhau, `correctIndex` từ 0–3, `explanation`. Giới hạn 20 câu, prompt/giải thích 5000 ký tự, mỗi lựa chọn 1000 ký tự, tên đề 255 ký tự, mức đạt 1–100. Mỗi lần lưu tạo phiên bản mới; sửa đồng thời với revision cũ trả 409.
- `POST /api/lessons/{id}/quiz/attempts`: học viên gửi `{ quizVersionId, submissionKey, answers }`; `submissionKey` là UUID, `answers` là danh sách chỉ số lựa chọn theo thứ tự câu. Backend tự chấm; không nhận điểm do client gửi. Gửi lại cùng UUID/nội dung trả lại kết quả đã lưu; tái sử dụng UUID cho nội dung khác trả 409. Nộp đề đã bị thay bằng phiên bản mới trả 409.
- `GET /api/lessons/{id}/quiz/attempts?page=0&size=5`: lịch sử của chính học viên, mới nhất trước; size 1–50. Lịch sử gồm mọi phiên bản, kể cả khi đề hiện tại đang nháp.
- `GET /api/quiz-attempts/{id}`: chủ bài làm hoặc người quản lý khóa xem điểm/giải thích theo đúng phiên bản đã làm; học viên khác bị chặn.
- `GET /api/lessons/{id}/quiz/statistics`: người quản lý khóa xem tổng lần làm, học viên duy nhất, số lần đạt, điểm trung bình và số trả lời sai mỗi câu của phiên bản hiện tại. Làm lại được tính là lần làm mới; gửi lại cùng UUID không tăng thống kê.

Chỉ học viên có đăng ký còn hiệu lực và bài xuất bản được làm/đọc lịch sử. Quiz đạt không tự hoàn thành bài hoặc thay tiến độ đọc; điểm phần trăm hiển thị bỏ phần thập phân, so mức đạt bằng tỷ lệ chính xác. Phản hồi được trả sau khi nộp; làm lại không giới hạn, phù hợp luyện tập. Phiên bản và bài làm lưu trên máy chủ; lần nộp đang chờ kết quả lưu trong sessionStorage để thử lại qua tải trang. Flyway V7 thêm bốn bảng; xóa bài học sẽ xóa quiz/lịch sử của bài như tiến độ và hỏi đáp. Không dùng thao tác xóa bài để lưu trữ nội dung cần giữ lịch sử.

## API hỏi đáp theo bài

- `GET /api/lessons/{lessonId}/questions?page=0&size=10`: phân trang từ 0, size 1–50, mới nhất trước. Học viên phải có đăng ký còn hiệu lực và bài đã xuất bản; người quản lý khóa xem được bài nháp. Câu hỏi bị ẩn không được tính vào tổng/trang của học viên.
- `POST /api/lessons/{lessonId}/questions`: học viên gửi `{ "body": "Câu hỏi" }`, 1–5000 ký tự, không chấp nhận chỉ khoảng trắng; tạo thông báo cho giảng viên trong cùng transaction.
- `GET /api/questions/{id}`: đọc một câu hỏi theo quyền, hỗ trợ liên kết thông báo. Câu hỏi bị ẩn trả 404 cho học viên.
- `PUT /api/questions/{id}/answer`: giảng viên phụ trách/admin gửi `{ "body": "Phản hồi" }`; lưu phản hồi hiện tại, người trả lời và thời điểm, đồng thời thông báo cho tác giả. Cập nhật thay phản hồi cũ; chưa có lịch sử phiên bản hay trao đổi nhiều tầng.
- `PUT /api/questions/{id}/visibility`: giảng viên phụ trách/admin gửi `{ "hidden": true }` để ẩn hoặc `false` để hiện lại. Phải hiện câu hỏi trước khi trả lời.

Mọi thao tác ghi yêu cầu CSRF. Flyway V6 thêm bảng hỏi đáp, giữ dữ liệu hiện có. Xóa bài học sẽ xóa hỏi đáp của bài; ẩn bài giữ hỏi đáp nhưng chặn học viên đọc. Nội dung được hiển thị như văn bản, không thực thi HTML.

### Bản nháp và lịch sử bài học

Trình soạn tự lưu sau khoảng 0,9 giây, tách theo giảng viên/quản trị viên và bài học. Mở lại sẽ khôi phục bản nháp trên máy chủ; nút **Lưu thay đổi** mới cập nhật bài học. Bản nháp cho bài mới dùng khóa `0` trong mỗi khóa học. Trạng thái lưu và nút thử lại hiển thị khi mất kết nối; hãy chờ báo đã lưu trước khi rời trang.

API `GET/PUT/DELETE /api/courses/{id}/lesson-drafts/{key}` kiểm tra quyền quản lý và phiên bản bản nháp. `GET /api/lessons/{id}/content-versions` trả 30 phiên bản gần nhất. Đưa phiên bản cũ vào bản nháp để xem/chỉnh rồi lưu lại. Flyway V11 giữ lịch sử nội dung trước mỗi lần sửa; lịch sử không sao lưu tệp đã bị xóa.

Giao diện gửi `expectedRevision` và `draftRevision` để tránh ghi đè từ tab khác; lưu nội dung, thêm lịch sử và tiêu thụ bản nháp cùng transaction. Client API cũ vẫn được hỗ trợ khi không gửi hai trường tùy chọn này. Xung đột trả HTTP 409 và giữ bản nháp/nội dung đang soạn.

### Báo cáo học viên theo khóa

Giảng viên phụ trách và quản trị viên có tab **Học viên** trong chi tiết khóa học. Tab hiển thị tổng toàn khóa, trạng thái/tiến độ, số bài đã hoàn thành đang xuất bản, số lượt nộp quiz, điểm trung bình/cao nhất, hoạt động gần nhất và liên kết tới câu hỏi chờ phản hồi đầu tiên. Ghi chú riêng và email không được đưa vào báo cáo.

`GET /api/courses/{id}/students?search=&status=&sort=name&page=0&size=10` trả summary toàn khóa và trang học viên. Tìm theo tên hoặc tài khoản; trạng thái gồm ENROLLED/COMPLETED/DROPPED; thứ tự name/progress; trang bắt đầu từ 0, kích thước tối đa 100. Giao diện hiển thị 10 học viên/trang, giữ bộ lọc trong phiên trang. Điểm quiz bao gồm mọi lượt nộp còn lưu trong khóa, kể cả phiên bản cũ. Chỉ câu hỏi chưa trả lời và chưa bị ẩn được tính vào hàng chờ. Các truy vấn tổng hợp lấy theo nhóm học viên của trang, không truy vấn riêng từng dòng. Flyway V12 bổ sung index cho báo cáo.
