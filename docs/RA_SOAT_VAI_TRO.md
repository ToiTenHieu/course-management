# Rà soát và nâng cấp theo vai trò — 11/10/2026

Đã đọc các luồng giao diện/API theo bốn vai trò và sửa các lỗi dưới đây. Các phiên kiểm tra trình duyệt độc lập dùng dữ liệu mẫu mới trên demo PostgreSQL, kết hợp kiểm thử backend H2. Phạm vi kiểm tra tập trung vào khám phá/đăng nhập, học/nộp bài, chấm bài/quản lý nội dung và xử lý thanh toán.

| Vai trò | Vấn đề trước khi sửa | Hành vi sau khi sửa |
| --- | --- | --- |
| Khách và mọi vai trò | Đăng nhập sau khi hết phiên dễ mất trang quản lý, bộ lọc hoặc tab đang mở | Quay lại trang nội bộ hợp lệ với query/hash; từ chối đường dẫn ngoài website hoặc trang không được hỗ trợ |
| Học viên | Máy chủ đã lưu bài tập nhưng phản hồi bị mất; sửa bài rồi thử lại có thể xung đột với lần nộp cũ | Kiểm tra bài đã lưu, hiển thị kết quả nếu có; nếu chưa rõ thì giữ đúng nội dung/tệp/UUID và kiểm tra trước khi gửi lại |
| Học viên | Thanh toán bị từ chối không có lý do hoặc thông báo, giao diện gợi ý gửi lại ngay | Đọc lý do và thông báo riêng, hướng dẫn đối chiếu trước khi gửi lại; điện thoại hiển thị thông tin và lý do theo từng mục |
| Giảng viên | Bài đã chấm vẫn nằm trong bộ lọc chưa chấm; trang cuối có thể trống; tải lại có thể làm mất nhận xét đang soạn | Loại bài đã chấm, cập nhật tổng/trang, giữ form chấm khác đang sửa; bỏ phản hồi tải lỗi đã lỗi thời |
| Giảng viên/admin | Xóa bài học kéo theo tiến độ, ghi chú, hỏi đáp, quiz và bài nộp | Chặn xóa bài có hoạt động bằng HTTP 409, hướng dẫn ẩn bài để giữ lịch sử |
| Giảng viên/admin | Ẩn/xóa bài xuất bản cuối làm khóa đã mở không còn nội dung học | Chặn cả giao diện lẫn API; khóa khóa học trong transaction để hai thao tác đồng thời không cùng xóa hết bài |
| Admin | Từ chối thanh toán thiếu giải thích và không thông báo cho học viên | Hộp nhập lý do bắt buộc trong UI; lưu lý do, thông báo và audit cùng transaction, hoàn tác tất cả nếu lưu lỗi |

Flyway V23 chỉ thêm `payments.rejection_reason`, không thay đổi yêu cầu cũ. Yêu cầu cũ hiển thị thông báo chưa có lý do lưu. API từ chối vẫn nhận yêu cầu cũ không có body, dùng lời giải thích mặc định yêu cầu đối chiếu; nếu gửi body thì `reason` phải có nội dung, tối đa 1.000 ký tự. Ví dụ: `PUT /api/payments/{id}/reject` với `{"reason":"Sai mã chuyển khoản; vui lòng liên hệ để đối chiếu."}`.

Kết quả kiểm tra:

- Backend: 156/156 đạt với H2 và 23 migration; bao gồm bảo toàn lịch sử, bài xuất bản cuối, ẩn bài đồng thời, lý do thanh toán/riêng tư/rollback và kiểm tra đầu vào.
- JavaScript: 30/30 đạt; Python: 6/6 đạt; cú pháp toàn bộ module JavaScript hợp lệ.
- Bộ hành trình Chromium có sẵn: 10/10 đạt trên demo PostgreSQL.
- Kiểm tra bổ sung qua Playwright CLI: khách tìm và xem khóa, đăng nhập giữ khóa; admin quay lại trang thanh toán; học viên nộp bài sau khi POST đã lưu nhưng bị thay phản hồi bằng HTTP 503; thông báo/lý do từ chối đến đúng học viên; giảng viên chấm bài ở trang 2 rồi quay về trang 1, giữ nhận xét khác đang soạn; chặn xóa bài có bài nộp và khóa nút ẩn/xóa bài xuất bản cuối. Không phát sinh lỗi JavaScript chưa xử lý trong các phiên này.
- Giao diện thanh toán kiểm tra ở 390px, lý do nằm trong chiều rộng màn hình; ảnh kiểm tra tại `output/playwright/role-student-payment-mobile.png`. Ảnh chấm bài tại `output/playwright/role-teacher-grading.png`. Báo cáo E2E tại `output/e2e-report/index.html`.

Đây là kết quả của các tình huống đã chạy, không khẳng định mọi lỗi trong sản phẩm đã được tìm hết. Bài tập đang soạn vẫn chỉ được giữ trong phiên trang; chưa hỗ trợ nháp bài nộp trên máy chủ. Bộ backend đầy đủ lần này chạy H2; kiểm tra PostgreSQL là các hành trình trình duyệt, không phải chạy lại toàn bộ 156 ca trên PostgreSQL.
