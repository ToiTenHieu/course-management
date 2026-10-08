# Dữ liệu và cài đặt trong database

## Chỉnh ngay trong ứng dụng

Đăng nhập admin → **Cài đặt** (`/settings.html`) để đổi:

- Dung lượng một tài liệu, số tài liệu mỗi bài, số câu mỗi quiz.
- Điểm đạt mặc định, chủ đề và trình độ điền sẵn khi tạo mới.
- Ngân hàng, số tài khoản và chủ tài khoản dùng trong hướng dẫn thanh toán.

Các giá trị nằm ở một bản ghi trong bảng `application_settings`, được Flyway V13 tạo với giá trị ban đầu. Backend đọc database khi xử lý yêu cầu, không cần build hoặc khởi động lại sau khi lưu. Tải lại các trang đang mở để giao diện nhận cài đặt mới. Mặc định áp dụng cho nội dung mới; bài, quiz và khóa đã lưu không bị ghi đè. Giảm giới hạn không xóa tài liệu hoặc câu hỏi cũ; lần thêm tài liệu/lưu quiz tiếp theo dùng giới hạn mới.

`GET /api/settings` và `PUT /api/settings` chỉ dành cho admin. Request lưu cần CSRF và `revision`; nếu hai admin sửa cùng lúc, lượt lưu từ phiên bản cũ trả HTTP 409 và giữ dữ liệu mới. `GET /api/auth/config` chỉ trả các cài đặt học tập cần cho giao diện công khai.

Khóa học, học phí, chủ đề, trình độ và phân công giảng viên chỉnh trong quản lý khóa học. Tiêu đề bài, nội dung, `videoUrl` YouTube và timestamp chỉnh trong trình soạn bài. Đề, điểm đạt và câu hỏi chỉnh ở tab Quiz. Tài khoản chỉnh ở quản lý người dùng. Đây đều là bản ghi database, ứng dụng không lấy nội dung đang dùng từ file mẫu.

## Khởi tạo demo

`src/main/resources/demo/catalog.json` chỉ là dữ liệu để tạo database demo ban đầu. Seeder ghi nội dung vào các bảng `users`, `courses`, `lessons`, bảng quiz và các bảng nghiệp vụ liên quan. Không dùng JSON làm nơi lưu thay đổi trong ứng dụng.

Các marker `base-demo-courses-v1`, `role-scenarios-v1`, `youtube-demo-courses-v1` và `demo-login-accounts-v1` trong `demo_seed_runs` ngăn chạy lại bộ mẫu đã hoàn tất. Database cũ đã có khóa chỉ được ghi marker bộ nền; không thêm lại sáu khóa nền. Khởi động lại giữ nội dung đã sửa; không xóa marker để thay video.

Danh sách tài khoản đăng nhập nhanh nằm trong `demo_login_accounts` và liên kết bằng `user_id`. Tên/vai trò được đọc từ `users`, nên đổi username không cần sửa JavaScript hoặc file mẫu. Chỉ hiện trong chế độ demo, với tài khoản hoạt động và còn dùng mật khẩu mẫu. Mật khẩu tài khoản vẫn được băm trong `users.password_hash`. `DEMO_PASSWORD` chỉ dùng khởi tạo và điền nhanh cho tài khoản mẫu; đổi biến không ghi đè mật khẩu đã lưu.

## Cấu hình hạ tầng

Kết nối database (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`), cổng (`PORT`), thời gian phiên (`SESSION_TIMEOUT`), mật khẩu/email khởi tạo demo (`DEMO_PASSWORD`, `DEMO_EMAIL_DOMAIN`) và trần HTTP multipart vẫn thuộc cấu hình máy chủ. Các giá trị này cần có trước khi ứng dụng kết nối database hoặc do servlet container quản lý.

`LESSON_MAX_FILE_SIZE` mặc định `20MB`, `LESSON_MAX_REQUEST_SIZE` mặc định `21MB`. Đây là trần của máy chủ; giới hạn tài liệu thực tế lưu trong database, mặc định 5 MB. Admin không thể lưu dung lượng vượt trần máy chủ. Nếu hạ trần HTTP, API công khai và backend tự giới hạn theo trần mới. `.env.example` chỉ minh họa; Spring Boot không tự đọc file `.env`.

Role, trạng thái, loại file hỗ trợ, nhãn giao diện và bốn lựa chọn quiz vẫn thuộc cấu trúc ứng dụng. Bốn lựa chọn khớp các cột `option_a`–`option_d`; đổi số lựa chọn cần thay schema. Giới hạn độ dài của trường khớp DTO/schema. Màu minh họa khóa được chọn tự động từ tên chủ đề lưu trong database.
