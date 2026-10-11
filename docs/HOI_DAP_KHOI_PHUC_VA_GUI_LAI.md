# Khôi phục hỏi đáp và gửi lại an toàn — 11/10/2026

Luồng hỏi đáp trước đây giữ nội dung bằng Map trong trang: tải lại toàn bộ trang làm mất bản đang soạn. Câu hỏi chưa có mã gửi, nên khi máy chủ lưu xong nhưng phản hồi bị mất, thử gửi lại có thể tạo thêm câu hỏi và thông báo. Phản hồi đã có mã UUID ở backend, nhưng sửa textarea sau lỗi làm đổi UUID nên vẫn có thể gửi trùng.

Thay đổi:

- Câu hỏi và phản hồi có bản nháp sessionStorage tách theo tài khoản, bài học và câu hỏi. Cả trang chi tiết khóa và phòng học đều truyền đúng tài khoản. Hội thoại có bản nháp tự mở khi tải lại; bỏ bản nháp cần xác nhận.
- Trước khi gửi, lưu nguyên nội dung/mã UUID và trạng thái đang chờ. Lỗi mạng, timeout, HTTP 5xx hoặc kết quả thành công thiếu dữ liệu giữ nguyên bộ này. Tải lại rồi chọn **Kiểm tra và gửi lại** dùng lại đúng bộ đã gửi.
- Nội dung chờ xác nhận tạm khóa chỉnh sửa. Nếu lần gửi đầu bị từ chối rõ ràng bằng 4xx, cho sửa; sửa nội dung tạo mã mới. Sau một lần gửi không rõ kết quả, lỗi quyền ở lần thử sau không được coi là bằng chứng rằng lần đầu chưa lưu.
- Trong lúc gửi, khóa các form, phân trang và thao tác ẩn/hiện của khu vực hỏi đáp; giữ những bản soạn khác qua lần tải lại danh sách. Gửi thành công xóa nội dung đã gửi và bản tạm.
- Gửi thành công đưa tiêu điểm tới thông báo kết quả. Trước đây đưa về textarea trống; lần bấm mở trao đổi làm ô trống bị blur, hiện lỗi bắt buộc nhập và đẩy vị trí nút giữa mouse-down/mouse-up, nên bấm lần đầu không mở. Kiểm tra trình duyệt đã tái hiện lỗi và xác nhận mở được bằng một lần bấm sau sửa.
- Flyway V24 bổ sung `lesson_questions.client_request_id`, unique theo học viên và mã gửi; dữ liệu cũ giữ nguyên với mã null. API mới nhận `{body, clientRequestId}`; UUID tùy chọn để tương thích client cũ. Chỉ yêu cầu có UUID mới được bảo đảm chống gửi trùng.
- Backend khóa khóa học trước khi tạo, kiểm tra đăng ký/bài xuất bản/ẩn câu hỏi trước khi trả lại kết quả. Gửi lại cùng học viên, bài, mã và nội dung trả câu hỏi cũ, không tạo lại thông báo. Dùng lại mã cho bài/nội dung khác trả 409; không lộ số phản hồi đã ẩn khi gửi lại.

Bản nháp thuộc tab trình duyệt, không đồng bộ thiết bị và không cam kết tồn tại sau đóng tab. Nội dung đã gửi và mã chống trùng lưu trong PostgreSQL. Khi storage bị chặn/đầy, giữ bản trong bộ nhớ, báo giới hạn và cảnh báo rời trang. Bỏ một bản đang chờ có cảnh báo rằng lần gửi trước có thể đã được lưu; người dùng cần kiểm tra danh sách trước khi gửi mới.

Kiểm chứng thực chạy:

- 161/161 kiểm thử Java đạt trên H2 qua migration thật. Bốn ca nghiệp vụ/API mới kiểm tra cùng UUID/khác nội dung hoặc bài/khác học viên, gửi đồng thời, CSRF/UUID/tương thích client cũ, đăng ký bị thu hồi và số phản hồi đã ẩn; một ca migration kiểm tra giữ dữ liệu cũ và unique theo học viên.
- 43/43 kiểm thử JavaScript đạt, gồm bảy ca mới kiểm tra bản soạn/mã gửi qua tải trang, nội dung chờ không bị đổi, lỗi mạng/timeout/5xx/2xx hỏng, 4xx rõ ràng cho sửa, tải lại khi chưa nhận kết quả, xóa sau xác nhận và storage bị chặn.
- 10/10 hành trình Chromium có sẵn đạt trên demo PostgreSQL (1 phút); toàn bộ 24 module JavaScript hợp lệ, `git diff --check` đạt.
- Playwright CLI chạy trên demo PostgreSQL: hai học viên cùng tab không nhận nhầm bản; câu hỏi thực sự đã lưu nhưng bị ngắt phản hồi, tải lại/gửi lại vẫn một câu hỏi và một thông báo. Phản hồi đã lưu nhưng trình duyệt nhận 503 hoặc 200 thiếu dữ liệu được thử lại bằng cùng mã, không tăng số phản hồi/thông báo.
- Giảng viên khôi phục bản phản hồi qua tải trang, admin không thấy bản đó, gửi phản hồi chính thức lưu đúng nhãn vai trò. Phòng học giữ bản câu hỏi qua tải lại; viewport 390px không tràn ngang; không có lỗi JavaScript chưa xử lý.
- Artifact cục bộ được Git bỏ qua: `output/playwright/questions-delivery-review.js`, `questions-draft-mobile.png`. Khóa học thử đã lưu trữ để giữ hội thoại kiểm chứng.
