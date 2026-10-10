# Kiểm tra theo vai trò — 09/10/2026

Kiểm tra giao diện bằng trình duyệt trong ứng dụng desktop, kết hợp API trên PostgreSQL demo tại `http://127.0.0.1:8080` và bộ kiểm thử backend trên H2. Các tab kiểm thử đều nằm trong ứng dụng; không mở Chrome ngoài.

## Kết quả và lỗi đã sửa

Phát hiện khối mã Markdown có chữ sáng nhưng kế thừa nền sáng của quy tắc `code` toàn cục. Ví dụ code gần như không đọc được. Đã thêm quy tắc `.structured-content pre code` trong `src/main/resources/static/css/style.css`: nền trong suốt, kế thừa màu chữ của khối `pre`, bỏ padding và bo góc bên trong. Quy tắc dùng chung cho phòng học và xem trước bài; inline code và mã chuyển khoản giữ kiểu hiện có.

Trình duyệt xác nhận màu chữ `rgb(239, 247, 244)` trên nền khối `rgb(23, 45, 42)`, nền của `code` trong suốt, trong cả phòng học và tab Xem trước của giảng viên. Đã xem ảnh desktop sau sửa; kiểm tra CSS trên mobile 390×844 và không tràn ngang. Đây là lỗi sản phẩm xác định được trong lượt kiểm tra này; các luồng bên dưới hoạt động trong phạm vi đã thử.

## Phạm vi đã kiểm tra trực tiếp

| Vai trò | Hành trình và kết quả |
|---|---|
| Khách | Tìm Java, xem chi tiết/các tab khóa, chương trình khóa có khóa quyền nội dung; đăng ký từ khóa quay về đúng mã khóa và tab; form rỗng báo lỗi theo trường. |
| Học viên mới | Đăng ký, đăng nhập sai/đúng, đăng ký miễn phí, xem Markdown, ghi chú riêng và đọc lại, chuyển bài/khôi phục vị trí, tiến độ 25%; quiz thiếu đáp án bị chặn, kết quả 100% và lịch sử lưu đúng. |
| Học viên mới | Tạo yêu cầu trả phí `TT000066`, hướng dẫn chuyển khoản thủ công và sao chép số tiền; trước duyệt chưa có enrollment, sau duyệt có enrollment và đọc được bài. Hồ sơ có HTML hiển thị nguyên văn, rồi khôi phục tên ban đầu. |
| Hai học viên | Đặt câu hỏi, giảng viên phản hồi, học viên khác trả lời trong cùng hội thoại; nhãn vai trò và thông báo đúng. Phản hồi bị ẩn không xuất hiện trong nội dung hoặc số lượng học viên nhận. |
| Giảng viên | Dashboard, danh sách khóa phụ trách, thông báo dẫn đúng câu hỏi; báo cáo tìm học viên ngoài trang đầu và hiển thị tiến độ/quiz đúng. Hồ sơ công khai lưu và đọc lại được. |
| Giảng viên | Soạn bài Markdown riêng, preview, đóng/khôi phục nháp, tải TXT; hai tab báo xung đột và giữ văn bản; lưu phiên bản, khôi phục lịch sử, xuất bản bài, soạn/xuất bản quiz. |
| Admin | Dashboard và hàng đợi; tìm/duyệt đúng yêu cầu thử, số liệu tổng cập nhật và cấp quyền học. Tìm tài khoản ngoài trang đầu, sửa/khôi phục hồ sơ, khóa/mở khóa. |
| Admin | Cấu hình BIN sai bị chặn; lưu lại cấu hình ban đầu. Báo cáo học viên/giảng viên có tổng đúng; xuất bản khóa thử thành công. |
| Học viên ở khóa thử | Đọc bài mới, thấy tài liệu/quiz mới, nộp quiz 100%, hoàn thành khóa 100%, tạo đánh giá 4 sao và đọc lại. |
| Mobile | Trang thanh toán, chi tiết/đánh giá và phòng học ở 390×844 không tràn ngang; điều hướng mở/đóng và Escape hoạt động. Kích thước trình duyệt được khôi phục sau kiểm tra. |

## Kiểm thử tự động và đối chiếu API

- `mvnw verify`: **111 đạt, 0 lỗi, BUILD SUCCESS**; log `target/local-demo/role-audit-final-verify.log`. Bao gồm quyền, CSRF, session, đồng thời, rollback, nháp/lịch sử, quiz, hỏi đáp, tải tài liệu và migration. Database của bộ này là H2, tách khỏi demo.
- **15 Node** và **6 Python** đã đạt trong lượt rà soát trước bản sửa CSS.
- **19 đối chiếu API trên PostgreSQL demo** đạt; báo cáo `output/role-audit/live-results.json`: quyền khách/học viên/giảng viên, không lộ đáp án trước nộp, riêng tư bài làm/enrollment, kết quả/tiến độ lưu sau restart, thanh toán đã cấp quyền, phản hồi bị ẩn và khóa QA vắng khỏi catalog.
- API tài liệu trả đúng từng byte của tệp TXT thử, `Content-Disposition: attachment` và `Cache-Control: no-store`; khách/học viên chưa đăng ký bị chặn. Trình duyệt trong ứng dụng chưa cung cấp sự kiện download trong lần thử, nên kết quả tải tệp được chứng minh bằng HTTP thật.
- `git diff --check` đạt. Không chạy lại bộ E2E CLI bên ngoài trong lượt này.

Ảnh đã kiểm tra: `output/role-audit/code-contrast-fixed-desktop.jpg`, `code-contrast-fixed-mobile.jpg`, `teacher-preview-fixed.jpg`. Các log/script/ảnh trong `output` và `target` được Git bỏ qua.

## Dữ liệu kiểm thử và phần cần bổ sung

Tạo riêng tài khoản học viên ID 144, khóa ID 78, bài ID 271, tài liệu ID 3; enrollment thử ID 93 và bài làm ID 32. Khóa 78 đã chuyển `ARCHIVED` để không làm nhiễu catalog; phản hồi thử đã hiện lại; tên/email và cấu hình ngân hàng được khôi phục. Tài khoản/dữ liệu thử được giữ để đối chiếu, thanh toán là giả lập trên demo.

Chat AI vẫn chưa tích hợp. QR hiện cần BIN ngân hàng và tài khoản nhận hợp lệ từ người vận hành; demo đang để BIN trống và tài khoản `DEMO-000001`, nên chỉ có hướng dẫn chuyển khoản. Chưa có webhook ngân hàng tự xác nhận. Đây là phần cấu hình/tính năng cần bổ sung, không phải lỗi được giải quyết bằng CSS. Video YouTube và thanh toán ngân hàng thật cần kiểm tra riêng khi có môi trường phù hợp.

Kết quả này ghi phạm vi và bằng chứng đã chạy; không khẳng định mọi tổ hợp dữ liệu hoặc tình huống vận hành đều đã được kiểm tra.
