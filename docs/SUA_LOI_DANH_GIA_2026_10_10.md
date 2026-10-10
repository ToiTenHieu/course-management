# Khắc phục sau đánh giá dự án — 10/10/2026

Đợt sửa này xử lý sáu phát hiện trong [báo cáo đánh giá](DANH_GIA_TOAN_BO_DU_AN_2026_10_10.md), bổ sung bảo vệ tài khoản quản trị, giới hạn yêu cầu xác thực và kiểm thử hồi quy. Giữ cấu trúc ứng dụng hiện có và dữ liệu cũ; Flyway V21 chỉ thêm hàng khóa quản lý tài khoản.

## Các thay đổi

| Phát hiện | Khắc phục | Kiểm chứng |
| --- | --- | --- |
| Mật khẩu Unicode vượt 72 byte khiến BCrypt lỗi | `PasswordPolicy` dùng chung cho đăng ký, admin tạo/đổi, tự đổi và reset. Mật khẩu mới 8–64 ký tự, không chỉ khoảng trắng, tối đa 72 byte UTF-8. Login quá 72 byte trả 401 chung. Giao diện báo lỗi cùng quy tắc. Internal error dispatch được phép đi vào trình xử lý lỗi, không bị biến thành 401 | API 25 ký tự `ắ` trả 400 đúng thông báo, 24 ký tự đăng ký/đăng nhập được; admin đổi bị từ chối giữ hash cũ; Node kiểm tra biên; Chromium kiểm tra lỗi biểu mẫu và đăng ký |
| Danh sách/chấm bài đọc tệp BYTEA không cần thiết | Truy vấn metadata liệt kê các cột cần cho DTO, không chọn `file_content`; endpoint tải tệp giữ truy vấn riêng và kiểm tra quyền | Suite bài tập/quyền/idempotency; Chromium nộp TXT UTF-8, tải lại đúng byte, giảng viên chấm và học viên xem nhận xét |
| Demo báo sẵn sàng trước khi seed hoàn tất | Readiness chỉ bật ở `ApplicationReadyEvent`. API trả 503 khi chưa sẵn sàng, 200 sau hoàn tất runner. Script demo và Playwright đợi endpoint mới | Unit kiểm tra hai trạng thái; toàn bộ E2E bắt đầu trên database mới sau readiness và đăng nhập demo ngay được |
| Thời điểm phụ thuộc múi giờ JVM/database/browser | Ghi giờ Việt Nam qua `ApplicationTime` cho entity, service và INSERT/UPDATE JDBC, gồm nộp bài/tài liệu/bản nháp/seed. `Clock` tiêm vào mục tiêu tuần. Frontend dùng chung parser/formatter/ngày bộ lọc Việt Nam | Clock UTC cố định tại Chủ nhật 16:59:59Z và 17:00:00Z kiểm tra đổi tuần; entity với JVM UTC; Node biên ngày; demo Chromium với JVM và database UTC kiểm tra thời điểm bài nộp |
| Thứ tự nhóm chưa chia chương khác thứ tự lưu | Chuẩn hóa nhóm không chương về cuối trước khi gán thứ tự bài | API request nhóm không chương ở đầu trả thứ tự thống nhất với danh sách bài lưu; Chromium xác nhận cùng hợp đồng |
| Khách không thấy tên chương | Template công khai hiển thị `chapterTitle` đã escape | Chromium khách xem nhãn chương và giữ khóa quan tâm qua đăng nhập/tải lại |

## Bảo vệ và tự động hóa bổ sung

- Chặn khóa/xóa admin hoạt động cuối cùng bằng HTTP 409. Hàng singleton `account_management_guard` được khóa trong transaction trước khi đếm admin và ghi thay đổi; bảo vệ hai admin đồng thời khóa tài khoản. Ca bị chặn không sinh lịch sử quản trị. Admin đã khóa không được tính là admin hoạt động.
- Giới hạn đăng ký 20 yêu cầu/15 phút và đăng nhập 30 lần xác thực thất bại/phút, bộ đếm riêng theo peer address thực. Đăng nhập thành công không tăng bộ đếm; cửa sổ cố định, hết hạn tự dọn, tối đa 10.000 khóa. Trả HTTP 429 khi vượt giới hạn. Không đọc địa chỉ từ header forwarded không tin cậy.
- CI kiểm tra cú pháp toàn bộ 19 module JavaScript, 22 Node test và 6 Python test; tiếp tục chạy H2/package, PostgreSQL thật và Chromium.
- Thêm bốn hành trình Chromium cho Unicode, chương/khóa quan tâm, bài tập/mục tiêu/audit, recovery/phiên cũ/token dùng một lần. Các browser context quản trị và học viên độc lập; bắt lỗi JavaScript trên trang học viên.
- README cập nhật số kiểm thử, readiness, giới hạn mật khẩu, giờ Việt Nam và bảo vệ admin.

## Kết quả kiểm thử

| Kiểm tra thực chạy | Kết quả |
| --- | --- |
| Toàn bộ kiểm thử mặc định (H2) | 146 đạt; 0 lỗi, 0 bỏ qua |
| Đóng gói ứng dụng | BUILD SUCCESS; JAR chạy và readiness trả 200 |
| Toàn bộ suite với datasource PostgreSQL | 146 đạt; 130 ca chạy PostgreSQL thật, 16 ca cấu hình/migration/unit giữ H2 hoặc mock |
| Node unit | 22/22 đạt |
| Python unit | 6/6 đạt |
| Cú pháp JavaScript | 19/19 module hợp lệ |
| Chromium trên demo PostgreSQL mới | 10/10 đạt, 50,2 giây; JVM/database UTC, trình duyệt UTC và context phụ America/Los_Angeles |
| HTTP kiểm chứng Unicode | 25 ký tự `ắ`/75 byte trả 400, thông báo đúng |
| HTTP kiểm chứng giới hạn xác thực | Đăng nhập thất bại trả 401 rồi 429; đăng ký vượt quota trả 429, bộ đếm độc lập |
| `git diff --check` | Đạt |

Bước `verify` đầu tiên đã qua đủ 146 ca nhưng Windows chặn rename JAR do demo đang giữ tệp. Sau khi dừng đúng tiến trình demo, chạy đóng gói không lặp kiểm thử đã thành công; demo được khởi động lại và sẵn sàng tại `http://127.0.0.1:8080`.

Lần E2E đầu đạt 9/10, ca recovery dừng vì selector kiểm thử khớp cả ô nhập và nút hiện mật khẩu. Đổi sang khớp nhãn chính xác rồi chạy lại đủ mười hành trình trên database mới đạt 10/10. Không đổi hành vi sản phẩm để vượt lỗi selector.

Backend và E2E dùng database PostgreSQL tạm riêng; chỉ database do công cụ kiểm thử tạo được xóa khi hoàn tất. Database demo hiện có giữ dữ liệu và chỉ áp dụng migration V21 khi khởi động bản mới.

Bằng chứng cục bộ (Git bỏ qua): `target/review-fixes-verify.log`, `target/review-fixes-package.log`, `target/review-fixes/{h2-reports,postgres-reports,postgres-tests.log,e2e.log}`, `target/review-fixes-e2e/{e2e.log,demo-server.log,review-facts.json}`, `target/review-fixes-{node,python}.log` và `output/e2e-report/`.

## Phạm vi còn lại

Bộ đếm xác thực và session đang nằm trong một instance. SMTP thật, tải lớn, đa trình duyệt và diễn tập backup/restore vẫn cần môi trường triển khai để kiểm tra. Đợt sửa không chuyển tự động dữ liệu thời điểm cũ: các cột cũ không có offset nên không đủ thông tin để xác định giờ ban đầu. Không thực hiện tái cấu trúc toàn bộ `app.js` hoặc chia lại lớp BusinessFlow chỉ để đổi bố cục mã trong đợt sửa chức năng này.

Tệp bài nộp vẫn được lưu trong database theo thiết kế hiện có; metadata không đọc tệp nữa nhưng dung lượng backup và quy trình lưu trữ/xóa nội dung vẫn cần theo dõi khi vận hành.
