# Lấy lại mật khẩu

## Người dùng

Ở **Đăng nhập**, chọn **Quên mật khẩu?**, nhập email đã đăng ký rồi gửi. Hệ thống luôn trả cùng thông báo, kể cả email không tồn tại, tài khoản bị khóa hoặc email trùng sau khi chuẩn hóa chữ hoa/thường. Sau khi gửi, giao diện chờ 60 giây trước khi cho gửi lại.

Mở liên kết trong thư, nhập mật khẩu mới hai lần. Mật khẩu theo quy tắc hiện tại: 8–64 ký tự, tối đa 72 byte UTF-8 để không vượt giới hạn BCrypt. Lưu thành công không tự đăng nhập; hãy đăng nhập lại. Các phiên cũ hết hiệu lực và tài khoản nhận một thông báo về thay đổi mật khẩu.

Liên kết dùng một lần, có hiệu lực 15 phút. Thành công thu hồi mọi liên kết khác của cùng tài khoản. Yêu cầu gửi lại sau 60 giây có thể tạo thêm liên kết; liên kết cũ chưa hết hạn vẫn dùng được cho đến khi một lần đổi mật khẩu thành công. Thay mật khẩu/vai trò, khóa tài khoản hoặc đổi email cũng làm liên kết cũ mất hiệu lực.

Mã nằm trong fragment `#token=...` nên không được gửi trong URL tới máy chủ. Trang đọc mã vào bộ nhớ rồi xóa fragment khỏi thanh địa chỉ; không lưu mã trong localStorage/sessionStorage. Nếu tải lại trang đặt mật khẩu, mở lại liên kết từ thư. Khi lỗi mạng, mật khẩu đang nhập giữ trong trang; chỉ xóa khi thành công. Nếu phản hồi thành công bị mất, thử đăng nhập bằng mật khẩu mới trước khi yêu cầu thư khác.

## Thử trên demo

Profile **demo** mặc định dùng hộp thư trong bộ nhớ, không gửi email ra ngoài. Admin mở **Hộp thư thử nghiệm** trong menu, bấm làm mới, chọn **Mở liên kết đặt lại** hoặc sao chép liên kết. Người chưa đăng nhập, học viên và giảng viên không đọc được hộp thư. Thư có thể còn hiển thị sau khi dùng liên kết, nhưng liên kết không dùng lại được.

Hộp thư giữ tối đa 100 thư và loại thư hết hạn; khởi động lại máy chủ xóa hộp thư. Các token trong database vẫn theo thời hạn và quy tắc phiên; liên kết đã sao chép có thể vẫn dùng được sau khởi động lại. Không dùng tài khoản demo chung để thử đổi mật khẩu nếu còn cần giữ thông tin đăng nhập mẫu; hãy tạo tài khoản QA riêng.

## Cấu hình triển khai

Ngoài demo, tính năng mặc định **disabled** và trang đăng nhập không hiện liên kết quên mật khẩu. Bật `smtp` cần `JavaMailSender`, email gửi hợp lệ và địa chỉ gốc HTTPS được cấu hình; hệ thống không lấy địa chỉ từ header Host của yêu cầu.

Mẫu cấu hình: [application-recovery.example.properties](../config/application-recovery.example.properties). Sao chép sang tệp cục bộ ngoài Git, đặt các biến môi trường `PASSWORD_RESET_BASE_URL`, `PASSWORD_RESET_FROM`, `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, rồi thêm tệp bằng `--spring.config.additional-location=file:/đường/dẫn/application-recovery.properties` khi chạy JAR. Mẫu dùng STARTTLS bắt buộc, xác thực SMTP và timeout 5 giây. Địa chỉ gốc chỉ gồm scheme/host/port, không chứa tài khoản, query, fragment hoặc đường dẫn phụ. Demo HTTP chỉ chấp nhận localhost; hộp thư demo không được bật khi `app.demo.enabled=false`.

Không có SMTP thật được cấu hình/gửi thử trong lần nâng cấp này. Nhánh SMTP đã kiểm tra bằng mail sender giả. Muốn nhận email thật cần cấu hình tài khoản gửi hợp lệ.

## API, giới hạn và dữ liệu

- `POST /api/auth/forgot-password`: `{ "email": "..." }`, CSRF; trả 202 và thông báo thống nhất. Xử lý tài khoản/gửi thư qua hàng đợi nền hai worker, tối đa 100 tác vụ chờ. Hàng đợi đầy trả 503 cho mọi email.
- `POST /api/auth/reset-password`: `{ "token": "...", "newPassword": "..." }`, CSRF. Token hết hạn, đã dùng hoặc bị thu hồi trả 400 chung.
- `GET /api/demo/recovery-mailbox`: chỉ admin và chỉ ở chế độ hộp thư demo; `Cache-Control: no-store`.

Mỗi địa chỉ kết nối trực tiếp được phép 10 yêu cầu gửi và 30 lần đổi trong 15 phút; hai bộ đếm riêng, không tin header `X-Forwarded-For` do khách gửi. Mỗi tài khoản có khoảng nghỉ 60 giây khi cấp thư. Rate limit và hàng đợi nằm trong bộ nhớ một instance; chạy nhiều instance cần thêm giới hạn dùng chung tại gateway/proxy được tin cậy. Hàng đợi chưa gửi không bền qua khởi động lại: người dùng cần gửi lại nếu chưa nhận thư.

Flyway V20 chỉ thêm bảng `password_reset_tokens`, giữ dữ liệu cũ. Token ngẫu nhiên 32 byte; database chỉ lưu SHA-256, email snapshot, phiên xác thực và thời hạn. Dữ liệu hết hạn được dọn khi cấp token mới. Lỗi gửi thư thu hồi token vừa cấp. Đổi mật khẩu, tăng phiên, thu hồi liên kết và tạo thông báo trong cùng transaction; lỗi sẽ rollback. Khóa tài khoản bảo vệ hai lần đổi đồng thời: chỉ một lần thành công.

Nguồn tham khảo: [OWASP Forgot Password](https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html) về phản hồi thống nhất, mã ngẫu nhiên/dùng một lần và giới hạn yêu cầu; [Spring Boot Sending Email](https://docs.spring.io/spring-boot/reference/io/email.html) về `JavaMailSender`, cấu hình SMTP và timeout.
