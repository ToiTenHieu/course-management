# Course Management — Học để tiến xa

Nền tảng quản lý khóa học trực tuyến phục vụ đồ án/demo. Backend Spring Boot, Java 21, PostgreSQL; giao diện HTML/CSS/JavaScript có bố cục chung, hỗ trợ màn hình nhỏ. Thanh toán chuyển khoản được admin đối chiếu và duyệt thủ công.

## Chức năng

- Khách: tìm khóa từ trang chủ, khám phá chủ đề, xem kết quả học tập/chương trình/học phí trước khi đăng nhập; giữ khóa đã chọn qua đăng nhập.
- Học viên: dashboard tiếp tục học, tìm/lọc khóa đã đăng ký, đăng ký và thanh toán, học từng bài với chế độ tập trung, lưu ghi chú riêng và vị trí học trên máy chủ, theo dõi tiến độ, đánh giá, thông báo và hồ sơ.
- Giảng viên: dashboard riêng và checklist chuẩn bị khóa, sửa thông tin/mục tiêu của khóa phụ trách, thêm/sửa/xóa bài học, xuất bản/ẩn bài học. Học phí/phân công và xuất bản khóa do admin quản lý.
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

## Cấu hình và database

`application.properties` đọc `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `PORT`, `BANK_NAME`, `BANK_ACCOUNT`, `BANK_HOLDER` từ môi trường. `.env.example` chỉ minh họa biến; Spring Boot không tự đọc file `.env`. Hãy export biến hoặc dùng file cấu hình bên ngoài. Không đưa thông tin đăng nhập thật vào repo.

Flyway quản lý schema qua `src/main/resources/db/migration`, Hibernate chỉ validate. Database mới được tạo bảng tự động. Database cũ được baseline ở phiên bản 0, giữ dữ liệu và thêm metadata/chỉ mục. Nếu dữ liệu cũ đã có đăng ký/tiến độ/đánh giá trùng, migration sẽ dừng ở unique index; cần rà soát dữ liệu trùng trước khi chạy tiếp. Không xóa tự động dữ liệu cũ.

## Kiểm tra

```powershell
.\mvnw.cmd -B -ntp verify
node --check src/main/resources/static/js/api.js
node --check src/main/resources/static/js/app.js
```

Bộ backend có 48 kiểm thử (47 ca nghiệp vụ/API và một ca khởi động), kiểm tra trên H2 ở chế độ PostgreSQL và PostgreSQL 18 với migration thật. Bao gồm quyền khám phá công khai, phiên khách, chỉnh sửa metadata của giảng viên, ghi chú riêng/khôi phục bài, danh mục, bộ lọc, phân trang, thống kê và giới hạn số truy vấn. Database kiểm thử được xóa dữ liệu trước mỗi ca; chỉ dùng database chuyên biệt `course_management_test`, tuyệt đối không trỏ test vào database demo hoặc dữ liệu cần giữ.

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

## Phạm vi bản nền

Xem [đánh giá trải nghiệm theo từng vai trò và lộ trình sản phẩm](docs/PRODUCT_EXPERIENCE.md). Ưu tiên tiếp theo là bài tập có phản hồi, hỏi đáp theo bài, soạn nội dung/video và phân trang các danh sách còn lại. Khi triển khai rộng cần thêm email xác minh/quên mật khẩu, thanh toán qua cổng, lưu trữ file, audit log và quan sát vận hành. Bản hiện tại dùng chuyển khoản đối chiếu thủ công.
