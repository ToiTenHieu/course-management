# Mục tiêu tuần và lịch sử thao tác

## Học viên

Trong **Tổng quan**, chọn mục tiêu 1–50 bài và bấm **Lưu mục tiêu**. Kế hoạch áp dụng cho tuần hiện tại, từ thứ Hai đến Chủ nhật theo giờ Việt Nam. Tuần mới bắt đầu với gợi ý 3 bài; học viên tự lưu kế hoạch mới. Không tự áp dụng mục tiêu của tuần trước.

Số bài hoàn thành lấy từ lần đầu học viên đánh dấu hoàn thành bài học trong khóa đã đăng ký. Mở bài, lưu ghi chú hoặc hoàn thành lại không tăng số liệu. Hoạt động đã ghi vẫn được giữ nếu bài bị ẩn/xóa hoặc chương trình thay đổi; tiến độ khóa học vẫn được tính riêng theo bài đang xuất bản. **Hoạt động 8 tuần gần nhất** cho thấy cả các tuần chưa đặt mục tiêu.

**Nhắc mục tiêu tại tổng quan** chỉ hiển thị lời nhắc số bài còn lại tại trang này. Không gửi email, push hoặc tạo thông báo lặp. Bỏ chọn để tắt lời nhắc.

Mục tiêu lưu theo tài khoản. Hai phiên sửa cùng lúc phải tải bản mới trước khi ghi đè. Khi lưu lỗi, ô nhập giữ nội dung; **Tải lại mục tiêu** cho phép bỏ thay đổi để lấy bản mới. Rời trang khi chưa lưu có cảnh báo.

## Quản trị viên

Mở **Lịch sử thao tác** trong menu. Lọc khoảng ngày, loại thao tác và tên người thực hiện/đối tượng. Danh sách phân trang 20 bản ghi, thời gian theo giờ Việt Nam; bộ lọc ngày tối đa 366 ngày và được giữ trên URL.

Ghi nhận năm loại thay đổi thành công:

- Xác nhận hoặc từ chối thanh toán.
- Đổi vai trò tài khoản, kể cả qua biểu mẫu quản lý người dùng.
- Khóa/mở tài khoản.
- Đổi trạng thái khóa học (nháp, xuất bản, lưu trữ).

Bản ghi gồm thời điểm, tài khoản/vai trò người thực hiện, mã và tên đối tượng, trạng thái trước/sau. Tên được chụp tại thời điểm thao tác; sửa tên sau này không sửa lịch sử. Các lời gọi nội bộ không có phiên đăng nhập được ghi là **Hệ thống**. Không ghi mật khẩu, email, ghi chú học viên, nội dung bài nộp hoặc tệp đính kèm.

Lịch sử lưu trong cùng giao dịch với thao tác. Thao tác lỗi hoặc bị từ chối không tạo bản ghi thành công; gửi lại trạng thái giống hiện tại cũng không thêm bản ghi. API không có thao tác sửa/xóa lịch sử; đây là lịch sử ứng dụng, chưa phải kho chứng cứ chống sửa bởi người có quyền quản trị database. Các thao tác trước khi bật tính năng không được dựng lại.

## API và dữ liệu

- `GET /api/learning-goal`: học viên, mục tiêu tuần hiện tại và hoạt động 8 tuần.
- `PUT /api/learning-goal`: học viên, CSRF; `weekStart`, `expectedRevision`, `lessonTarget`, `dashboardReminder`. Phiên cũ hoặc tuần đã đổi trả 409.
- `GET /api/audit-logs?from=YYYY-MM-DD&to=YYYY-MM-DD&action=&search=&page=0&size=20`: chỉ admin; kích thước trang 1–100.

Flyway V19 bổ sung mục tiêu, hoạt động hoàn thành và lịch sử. Hoạt động cũ có `completed_at` được chuyển sang bảng mới, gộp theo học viên/bài và giữ thời điểm sớm nhất; bản ghi chưa có thời điểm hoàn thành không được suy đoán. Xóa tài khoản xóa mục tiêu/hoạt động riêng của tài khoản; lịch sử thao tác giữ tên người thực hiện, đặt tham chiếu tài khoản đã xóa thành `null`.
