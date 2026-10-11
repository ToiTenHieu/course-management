# Kiểm tra đầu vào và gợi ý lộ trình

Đăng nhập học viên → **Kiểm tra đầu vào** trên menu hoặc nút tại Tổng quan. Chọn Web, Java & Backend hoặc SQL & phân tích dữ liệu, chọn số giờ học mỗi tuần rồi làm bài. Có 4 câu kiến thức chung và 4 câu theo hướng học; mỗi câu một đáp án đúng, hoặc chọn **Chưa biết**. Nộp khi trả lời đủ 8 câu. Không giới hạn thời gian.

Kết quả hiển thị điểm tổng, điểm hai nhóm, đáp án và giải thích, các bước thực hành và liên kết khóa đang xuất bản. Điểm 0–3: bắt đầu từ nền tảng; 4–6: đang xây nền tảng; 7–8: sẵn sàng thực hành. Điểm từng nhóm dưới 3/4 thêm bước củng cố tương ứng. Hai bước dự án/phát triển tiếp theo luôn có; dự án Java là điều kiện thực hành trước bước Spring Boot.

Số tuần từng bước bằng số giờ ước tính chia cho giờ mỗi tuần, làm tròn lên. Đây là gợi ý dựa trên bài kiểm tra ngắn, không phải chứng nhận năng lực. Học viên điều chỉnh sau khi thực hành. Liên kết khóa được chọn theo chủ đề trong tên, chỉ trả metadata PUBLISHED; nếu không có, vẫn có mục tiêu tự thực hành. Gợi ý không tự đăng ký khóa hoặc tạo thanh toán.

Kết quả gần nhất lưu ở PostgreSQL theo tài khoản, xem được sau tải lại hoặc đổi thiết bị. Làm lại chỉ thay kết quả sau khi nộp thành công. Bản đang làm gồm hướng học, giờ/tuần và các câu đã chọn được giữ bằng sessionStorage theo tài khoản trong tab hiện tại. Quay lại/tải trang có thể **Tiếp tục bài kiểm tra**, **Xem kết quả gần nhất** hoặc **Xóa bản đang làm**; xóa bản đang làm không xóa kết quả đã nộp. Nộp thành công xóa bản tạm. Bản sai phiên bản đề hoặc dữ liệu không hợp lệ bị bỏ. Nếu storage không lưu được, giao diện báo rõ và cảnh báo khi rời trang; bản đó chỉ còn trong trang đang mở. Bản đang làm không đồng bộ sang thiết bị/tab khác và không được cam kết giữ sau đóng tab. Danh sách khóa gợi ý cập nhật theo tình trạng xuất bản hiện tại. Chỉ lưu kết quả gần nhất, chưa có lịch sử các lần kiểm tra.

## API và dữ liệu

- `GET /api/learning-assessment`: đề và các lựa chọn, không có đáp án đúng/giải thích.
- `POST /api/learning-assessment`: `{version: 1, goal: "WEB" | "JAVA" | "DATA", hoursPerWeek: 1..20, answers: [8 giá trị -1..3]}`; `-1` là Chưa biết, `0..3` là A..D. Máy chủ tự chấm, không nhận điểm từ trình duyệt. Version sai trả 409; đầu vào sai trả 400.
- `GET /api/learning-assessment/latest`: kết quả của chính học viên đang đăng nhập hoặc `null` nếu chưa làm. Mọi endpoint yêu cầu STUDENT; POST yêu cầu CSRF.
- Flyway V22 thêm `learning_assessments`, khóa chính student_id và khóa ngoại xóa theo tài khoản; lưu phiên bản đề, hướng học, giờ/tuần, đáp án và thời điểm nộp. Khóa tài khoản khi ghi để tránh tạo trùng khi nộp đồng thời. Đề phiên bản 1 phải giữ nguyên để chấm lại đúng kết quả đã lưu; nếu thay đề cần giữ ngân hàng phiên bản cũ.

Kiểm thử backend trong `BusinessFlowTests`: quyền/CSRF, không lộ đáp án trước khi nộp, input/version sai không ghi đè, chấm ba hướng học và ngưỡng điểm nhóm, thời gian, riêng tư, loại khóa nháp/lưu trữ và phân biệt Java/JavaScript. Chạy `./mvnw.cmd -B -ntp verify` và `node --test tests/unit/*.test.mjs`.

Kiểm tra ngày 11/10/2026: bốn ca backend mới đạt qua `-Dtest=BusinessFlowTests#learningAssessment* verify`; 22 ca JavaScript và 6 ca Python đạt. Lần chạy toàn bộ có 149/150 ca đạt, ca còn lại phát hiện độ chính xác timestamp khi đọc lại; đã sửa lưu theo microsecond và kiểm tra lại cả bốn ca mới thành công. Migration V22 chạy được trên H2 và PostgreSQL demo. Playwright CLI kiểm tra tài khoản thử riêng: mở từ Tổng quan, nộp thiếu câu, Web 8/8 → hai bước thực hành, làm lại SQL toàn Chưa biết → 0/8 và bốn bước, tải lại giữ kết quả. Giao diện desktop/390px đã xem ảnh, không tràn ngang hoặc lỗi console. Ảnh và script kiểm tra cục bộ ở `output/playwright/assessment-*` (không đưa vào Git).
