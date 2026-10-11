# Cải thiện bản đang làm và báo cáo — 11/10/2026

Đợt nâng cấp này sửa frontend của hệ thống hiện có, không thêm migration hoặc đổi quy tắc chấm điểm/báo cáo ở backend.

| Phần | Trước | Sau |
| --- | --- | --- |
| Bài kiểm tra đầu vào | Tải lại trang mất hướng học, thời gian và đáp án chưa nộp | Giữ bản trong tab theo tài khoản; tiếp tục, xem kết quả cũ hoặc xóa bản đang làm; nộp thành công mới xóa bản tạm |
| Quiz học viên | Lựa chọn chỉ còn trong bộ nhớ của trang | Giữ theo tài khoản, bài và phiên bản đề; tải lại vẫn nhận đúng câu đã chọn, không tự chọn câu chưa trả lời |
| Soạn quiz | Bản soạn mất khi tải lại toàn bộ trang | Khôi phục bản soạn với revision ban đầu; xung đột giữ nội dung, dùng quy trình tải đề mới/lấy revision mới trước khi lưu |
| Báo cáo hoạt động | Lỗi thiếu nút thử lại; có thể bấm chuyển nhiều trang khi đang tải | Có trạng thái tải, khóa nút, thử lại và đưa về trang hợp lệ khi số trang giảm |
| Đổi khoảng ngày/CSV | Số liệu cũ vẫn nằm cạnh khoảng mới; phản hồi xuất có thể đến sau khi đã đổi ngày | Xóa số liệu hoạt động cũ khi sửa ngày, bỏ phản hồi lỗi thời; không tải CSV của khoảng cũ khi người dùng đã đổi ngày |

Các bản đang làm được lưu bằng sessionStorage, riêng từng tab và tách khóa theo tài khoản. Không phải bản nháp trên máy chủ; kết quả bài kiểm tra, bài làm quiz và phiên bản quiz đã lưu vẫn ở PostgreSQL. Đóng tab/đổi thiết bị không được cam kết khôi phục bản đang làm. Dữ liệu JSON sai, sai phiên bản hoặc vượt giới hạn bị loại trước khi hiển thị. Khi storage bị chặn hoặc đầy, giữ bản hiện tại trong bộ nhớ, hiển thị cảnh báo rõ và cảnh báo rời trang; không báo đã lưu qua tải trang nếu việc lưu không thành công.

Kiểm chứng thực chạy:

- 36/36 kiểm thử JavaScript đạt, gồm sáu ca mới: khôi phục đáp án một phần, tách tài khoản/phiên bản, loại bản sai/JSON hỏng, storage bị chặn, xóa sau lưu và quota đầy không làm lấy nhầm bản cũ.
- Toàn bộ 23 module JavaScript hợp lệ qua `node --check`; `git diff --check` đạt.
- 10/10 hành trình Chromium có sẵn đạt trên demo PostgreSQL, 54,4 giây.
- Playwright CLI dùng tài khoản/khóa thử riêng: kiểm tra đầu vào Java 6 giờ/tuần giữ hai đáp án qua điều hướng và tải lại; đổi sang học viên khác không thấy bản đó; xóa bản không đổi kết quả cũ; bản sai version bị bỏ. Quiz giữ lựa chọn qua tải lại và xóa bản tạm sau nộp. Giảng viên khôi phục bản soạn, gặp revision 409 từ thay đổi ở tab khác, giữ nội dung rồi lưu phiên bản kế tiếp.
- Mô phỏng HTTP 503 và phản hồi chậm tại trình duyệt: báo cáo thử lại thành công, khóa phân trang khi tải, đổi ngày bỏ phản hồi cũ, số trang giảm đưa về trang hợp lệ; CSV lỗi thử lại được, tên file đúng khoảng đã áp dụng; CSV đến muộn sau đổi ngày không tạo download.
- Kiểm tra đầu vào ở 390px không tràn ngang. Ảnh: `output/playwright/assessment-resume-mobile.png`, `assessment-resume-prompt.png`, `report-retry.png`; script: `improvements-review.js`, `report-delayed-export.js`, `assessment-mobile-preview.js`. Đây là artifact kiểm chứng cục bộ được Git bỏ qua.

Backend không đổi trong đợt này nên không chạy lại toàn bộ bộ Java; kết quả nền của đợt trước là 156/156 với H2. Các kiểm tra phiên bản/chấm điểm hiện tại trong trình duyệt đi qua backend/PostgreSQL thật. Ca lỗi/phân trang báo cáo bổ sung dùng phản hồi có kiểm soát tại trình duyệt, không phải đo tải hoặc thay dữ liệu báo cáo trong database.
