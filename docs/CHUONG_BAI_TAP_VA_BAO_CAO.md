# Chương, bài tập và báo cáo theo thời gian

## Sắp xếp chương trình

Giảng viên phụ trách/admin mở khóa → **Chương trình** → **Chia chương & sắp xếp bài**. Thêm và đổi tên chương, kéo bằng biểu tượng ⠿ để sắp xếp bài hoặc chuyển chương. Nút ↑ ↓ và ô chọn chương hỗ trợ bàn phím/điện thoại; chương cũng có nút đổi thứ tự. Xóa chương đưa các bài về nhóm chưa chia chương. Chọn **Lưu chương trình** để áp dụng toàn bộ cùng lúc.

Danh sách công khai và phòng học hiển thị tên chương của từng bài. Khóa cũ tiếp tục hoạt động với bài chưa chia chương; không tự đổi nội dung, đăng ký hoặc tiến độ. Lưu chương trình chuẩn hóa thứ tự bài thành 1…N. Revision bảo vệ khi hai tab sửa hoặc có bài được thêm/sửa/xóa trong lúc sắp xếp. Khi xung đột/lỗi mạng, bản sắp xếp trên trang được giữ; tải lại chương trình chỉ bỏ thay đổi sau xác nhận. Bản sắp xếp chưa lưu chưa được lưu trên máy chủ.

API quản lý: `GET/PUT /api/courses/{id}/curriculum`. PUT nhận `{expectedRevision, groups:[{chapterId,title,lessonIds}]}`. Chương mới có `chapterId:null`; nhóm chưa chia chương có ID null và title rỗng. Mỗi bài của khóa phải xuất hiện đúng một lần; chương của khóa khác bị chặn. Tối đa 100 chương có tên. Nội dung đang soạn của bài dùng revision riêng và được báo xung đột nếu chương trình đã đổi.

## Bài tập và chấm điểm

Giảng viên/admin mở tab **Bài tập**, chọn bài học, nhập tên/đề/tiêu chí, chọn xuất bản rồi lưu. Học viên đã đăng ký vào phòng học hoặc tab Bài tập để nhập lời giải và/hoặc đính kèm một tệp. Hỗ trợ PDF, TXT UTF-8, PNG, JPG, WebP; cùng giới hạn dung lượng/tính hợp lệ như tài liệu bài học trong Cài đặt.

Mỗi học viên nộp một lần cho mỗi bài tập. Lượt nộp giữ nguyên tên/đề/phiên bản đã làm dù giảng viên sửa hoặc ẩn đề sau đó. Học viên xem bài nộp, điểm 0–100, người chấm và nhận xét; có nút tải lại điểm. Giảng viên lọc bài chưa chấm, đọc từng trang năm bài, tải file và chấm/cập nhật điểm. Cập nhật một điểm giữ nội dung chưa lưu trong các form chấm khác.

Quyền tải tệp được kiểm tra ở máy chủ: chủ bài nộp còn quyền học hoặc giảng viên phụ trách/admin. Học viên khác không xem được bài nộp hay danh sách lớp. Giảng viên vẫn xem lịch sử của học viên đã ngừng học. Bài/khóa nháp và đăng ký đã ngừng học chặn học viên theo quyền phòng học hiện có.

UUID và hash nội dung giúp thử lại cùng lượt nộp không tạo trùng; khóa theo khóa học bảo vệ yêu cầu đồng thời. Đề hoặc điểm thay đổi trong lúc người dùng thao tác trả 409. Khi mất phản hồi nộp bài, trang kiểm tra bài đã lưu trước khi gửi lại; giữ nguyên văn bản, tệp và UUID trong lúc chưa rõ kết quả. Nút **Kiểm tra / thử lại bài nộp** tiếp tục bằng yêu cầu gốc. Nếu xác nhận xung đột phiên bản đề mà chưa có bài nộp, có thể sửa hoặc tải lại đề. Nội dung đang soạn chỉ giữ trong trang hiện tại, chưa lưu qua tải lại toàn bộ trang. Điểm bài tập độc lập với tiến độ đọc bài và quiz. Chưa có hạn nộp, nộp lại nhiều phiên bản, email/thông báo điểm hoặc chứng nhận.

Khi lọc **Chỉ bài chưa chấm**, lưu điểm loại bài khỏi danh sách và cập nhật tổng số; trang cuối trống được đưa về trang còn bài. Nếu đang soạn nhận xét cho bài khác, giữ form đó thay vì tải lại toàn bộ danh sách. Chuyển trang/lọc/tải lại cảnh báo trước khi bỏ nội dung chưa lưu.

API:

- `GET/PUT /api/lessons/{id}/assignment`: xem đề/bài của mình hoặc lưu đề với `{expectedRevision,title,instructions,published}`.
- `POST /api/lessons/{id}/assignment/submissions`: multipart với `expectedRevision`, `submissionKey` UUID, `answer`, `file` tùy chọn.
- `GET /api/lessons/{id}/assignment/submissions?ungradedOnly=false&page=0&size=5`: người quản lý đọc danh sách; size 1–50.
- `PUT /api/assignment-submissions/{id}/grade`: người quản lý gửi `{expectedRevision,score,feedback}`.
- `GET /api/assignment-submissions/{id}/file`: tải file riêng tư, attachment và no-store.

## Báo cáo thời gian và CSV

Admin mở **Báo cáo**, chọn từ/đến ngày và **Xem báo cáo**. Tối đa 366 ngày, gồm hai ngày đầu/cuối. Lọc ngày được giữ trong URL. Tổng toàn khoảng không thay đổi khi chuyển trang; từng trang hiển thị 20 khóa có hoạt động.

- Đăng ký: ngày `enrollment_date` trong khoảng. Số đã hoàn thành và tiến độ trung bình là **trạng thái hiện tại của nhóm đăng ký đó**, không phải số người hoàn thành đúng trong khoảng.
- Doanh thu: chỉ thanh toán `CONFIRMED` có `confirmed_at` trong khoảng; không dùng ngày tạo yêu cầu và không tính chờ/từ chối. Hai nhóm được tổng hợp riêng trước khi nối để không nhân doanh thu theo số học viên.
- **Xuất CSV / Excel** tải CSV UTF-8 có BOM mở được bằng Excel; không tạo file XLSX. Xuất toàn bộ khoảng đã xem, tối đa 5.000 khóa; vượt giới hạn trả lỗi để thu hẹp khoảng, không âm thầm cắt dữ liệu. Nội dung tên khóa có dấu phân cách/dấu nháy/xuống dòng được escape, nội dung có thể thành công thức được trung hòa.

API admin: `GET /api/reports/activity?from=YYYY-MM-DD&to=YYYY-MM-DD&page=0&size=20` và `GET /api/reports/activity/export?from=...&to=...`.

Khi tải báo cáo, nút xem/phân trang/xuất được khóa để không nhảy trang do bấm liên tục. Lỗi có nút **Thử tải lại báo cáo**. Đổi khoảng ngày xóa số liệu cũ khỏi phần báo cáo hoạt động và cần bấm xem để áp dụng; phản hồi cũ đến muộn không ghi đè. Nếu số trang giảm, chuyển về trang hợp lệ. Xuất CSV có trạng thái đang xuất; lỗi cho phép thử lại. Đổi khoảng ngày trong khi chờ CSV sẽ bỏ kết quả xuất cũ, cần áp dụng khoảng mới trước khi xuất tiếp.

## Dữ liệu và kiểm tra

Flyway V18 thêm chương, liên kết bài, revision, bài tập/bài nộp và chỉ mục báo cáo; giữ dữ liệu cũ. API chặn xóa bài có tiến độ, ghi chú, hỏi đáp, lượt làm quiz hoặc bài nộp; dùng ẩn bài/lưu trữ khóa để giữ lịch sử. Khóa đang xuất bản phải còn ít nhất một bài xuất bản: xuất bản bài thay thế hoặc nhờ admin chuyển khóa về nháp trước khi ẩn/xóa bài cuối.

Kiểm thử backend mới bao gồm bảo toàn ghi chú khi sắp xếp; sai quyền/CSRF/chương ngoại khóa; xung đột; riêng tư/tệp/đề cũ; nộp lặp và đồng thời; điểm được chấm lại; đăng ký ngừng học; ranh giới ngày/doanh thu/CSV. Xem kết quả thực chạy trong `KE_HOACH_KIEM_THU.md`.
