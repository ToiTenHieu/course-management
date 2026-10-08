# Luồng demo và kiểm thử Course Management

Tài liệu theo bản chạy ngày 08/10/2026. Địa chỉ: http://127.0.0.1:8080.

## 1. Chuẩn bị

Mở từng vai trò trong trình duyệt hoặc profile riêng để giữ các phiên độc lập. Ba tab trong cùng một profile dùng chung phiên đăng nhập. Riêng kiểm thử xung đột cần hai tab cùng tài khoản giảng viên.

| Vai trò | Tài khoản | Mật khẩu | Dùng để |
|---|---|---|---|
| Quản trị viên | `admin_demo` | `Demo123!` | Khóa học, tài khoản, thanh toán, báo cáo |
| Giảng viên | `teacher_demo` | `Demo123!` | Khóa Java/Thiết kế, bài học, quiz, hỏi đáp |
| Giảng viên mẫu | `teacher_scenario` | `Demo123!` | Báo cáo khóa Figma có nhiều học viên |
| Học viên mẫu | `student_demo` | `Demo123!` | Dữ liệu có sẵn, ghi chú, thông báo, lịch sử |
| Học viên mới | Tự đăng ký, tên có hậu tố thời gian | Tự đặt | Đăng ký mới, thanh toán và học từ 0% |

Dữ liệu đã đối chiếu trên máy hiện tại:

| Khóa | Liên kết | Mục đích |
|---|---|---|
| Java từ nền tảng đến ứng dụng | http://127.0.0.1:8080/course-detail.html?id=2 | Miễn phí, 4 bài xuất bản; demo 25% → 50% → 75% → 100% |
| Thiết kế giao diện với tư duy sản phẩm | http://127.0.0.1:8080/course-detail.html?id=3 | 249.000đ; duyệt thanh toán thủ công |
| Figma cho sản phẩm số · Nhập môn | http://127.0.0.1:8080/course-detail.html?id=19#students | Giảng viên `teacher_scenario`, 27 học viên; phân trang và báo cáo |

ID và số liệu có thể thay đổi khi chuyển database hoặc tiếp tục thao tác. Khi đó tìm khóa theo tên trong danh mục. Dữ liệu demo giữ qua khởi động, không tự trở về trạng thái ban đầu. Dùng học viên mới cho luồng đăng ký/thanh toán để tránh gặp khóa đã đăng ký hoặc đã hoàn thành.

Chuẩn bị thêm một PNG/JPG nhỏ, một PDF hoặc TXT UTF-8 và một URL YouTube/MP4 có thể phát. Tạo khóa riêng **Demo soạn bài – [thời gian]**, giá 0, giao cho `teacher_demo`, để thử sửa/ẩn/xóa mà không làm thay đổi khóa Java dùng trình diễn tiến độ.

## 2. Kịch bản trình diễn khoảng 20–25 phút

Thứ tự gợi ý: khám phá → học miễn phí → thanh toán → soạn nội dung/bản nháp → quiz → hỏi đáp → báo cáo. Có thể chuẩn bị sẵn tài khoản mới và khóa soạn bài để tiết kiệm thời gian.

### D01. Khách tìm khóa và đăng ký tài khoản — 2 phút

1. Mở trang chủ trong phiên chưa đăng nhập, tìm “Java”.
2. Đổi chủ đề, lọc miễn phí và mở chi tiết khóa.
3. Xem giới thiệu, kết quả học tập, chương trình và học phí.
4. Chọn đăng ký học; chuyển tới đăng nhập/đăng ký.
5. Tạo tài khoản học viên mới, đăng nhập, tiếp tục tới khóa đã chọn.

**Mong đợi:** khách chỉ thấy khóa xuất bản và nội dung giới thiệu/chương trình; chưa đọc được toàn bộ bài hoặc tải tài liệu. Đăng nhập giữ đích khóa đã chọn. Tài khoản mới có không gian học viên.

### D02. Học miễn phí, ghi chú và hoàn thành — 3 phút

1. Học viên mới mở khóa Java, chọn **Đăng ký miễn phí** rồi **Vào phòng học**.
2. Đọc bài đầu; ghi “Cần ôn lại phần cài đặt Java”, lưu ghi chú bằng thao tác lưu trong phòng học.
3. Đánh dấu bài đầu hoàn thành: tiến độ 25%.
4. Chuyển bài, tải lại trang: kiểm tra vị trí học và ghi chú đã lưu.
5. Hoàn thành bốn bài; mở **Khóa học của tôi**.

**Mong đợi:** tiến độ lần lượt 25%, 50%, 75%, 100%; cuối cùng trạng thái Hoàn thành. Reload giữ dữ liệu; ghi chú chỉ thuộc học viên hiện tại. Bài nháp không được tính vào tiến độ.

**Nhánh video YouTube:** tìm “Demo YouTube” và chọn **HTML nhập môn · Demo YouTube** hoặc **JavaScript nhập môn · Demo YouTube**. Đăng ký miễn phí, vào phòng học, bấm phát video; chuyển bài thứ hai để kiểm tra video mở đúng mốc. Xem nguồn freeCodeCamp.org/tác giả trong nội dung và thử link **mở trên YouTube** dưới player. Các bài dùng chung một video dài, học viên tự dừng ở cuối phần được hướng dẫn. Hoàn thành 5 bài để thấy tiến độ 20% → 40% → 60% → 80% → 100%; bài cuối có quiz ôn tập. Video tiếng Anh, cần kết nối Internet; ứng dụng chỉ lưu URL. Giảng viên có thể sửa URL như bài học thông thường nếu nguồn không còn khả dụng. Khởi động lại demo không tạo trùng hoặc ghi đè các bài đã sửa.

### D03. Thanh toán và mở quyền học — 3 phút

1. Học viên mới mở khóa Thiết kế giao diện; chọn **Đăng ký & thanh toán**.
2. Xem số tiền/nội dung chuyển khoản; thử nút sao chép.
3. Đóng hướng dẫn: trạng thái chờ, chưa có quyền vào phòng học.
4. Trong phiên admin: mở **Duyệt thanh toán**, tìm theo tên học viên, chọn **Xác nhận** và xác nhận trong hộp thoại.
5. Học viên tải lại chi tiết khóa, vào phòng học và kiểm tra thông báo.

**Mong đợi:** chỉ sau khi admin xác nhận mới có quyền học; số tiền và khóa đúng. Xác nhận lặp không cấp quyền hoặc tạo đăng ký trùng. Đây là đối chiếu thủ công bằng dữ liệu demo.

**Nhánh kiểm thử:** dùng học viên khác để admin **Từ chối**; học viên vẫn chưa có quyền học và có thể tạo yêu cầu mới.

### D04. Giảng viên soạn nội dung, video và tài liệu — 3 phút

1. Admin tạo khóa Demo soạn bài ở trạng thái Nháp, giao cho `teacher_demo`.
2. Giảng viên mở **Khóa phụ trách**, vào khóa đó, chọn **Thêm bài**.
3. Chọn **Nội dung có cấu trúc**, nhập mẫu ở phần 3; thêm video.
4. Mở **Xem trước**: kiểm tra tiêu đề, chữ đậm, danh sách, mã và video.
5. **Lưu thay đổi**; mở **Sửa**, tải PNG và PDF/TXT, chọn **Chèn ảnh vào bài**, xem trước rồi lưu.
6. Giảng viên **Xuất bản** bài; admin đổi trạng thái khóa sang **PUBLISHED**.
7. Học viên đăng ký khóa miễn phí này, đọc bài, xem ảnh/video và tải tài liệu.

**Mong đợi:** bài mới lưu ở trạng thái Nháp; bài xuất bản mới hiện cho học viên. Preview và phòng học hiển thị cùng nội dung. Tài liệu chỉ tải được theo quyền học. Bài chỉ có video cũng xem trước được.

### D05. Tự lưu, lịch sử và xung đột — 3 phút

1. Giảng viên sửa bài vừa tạo; thêm một đoạn, chờ báo **Đã lưu bản nháp trên máy chủ**.
2. Đóng trình soạn, chọn **Đóng, giữ bản nháp**; mở Sửa lại.
3. Trong phiên học viên, tải lại bài trước khi giảng viên lưu thay đổi.
4. Giảng viên **Lưu thay đổi**; học viên tải lại bài lần nữa.
5. Sửa tiếp, mở **Lịch sử nội dung**, chọn **Đưa vào bản nháp**; xem trước rồi lưu nếu muốn phục hồi.
6. Để demo xung đột: mở cùng bài ở hai tab giảng viên trước khi sửa; tab A sửa và chờ lưu nháp; tab B sửa và chờ phản hồi.

**Mong đợi:** mở lại khôi phục bản nháp; tự lưu chưa cập nhật bài học viên đang đọc. Lưu thay đổi mới cập nhật bài. Lịch sử được đưa về trình soạn để xem trước. Tab B báo xung đột và giữ văn bản, không ghi đè bản nháp tab A.

Bản nháp này áp dụng cho trình soạn **nội dung bài học**. Trình soạn quiz dùng cơ chế giữ bản soạn trong phiên và revision riêng.

### D06. Soạn quiz, làm bài và xem lịch sử — 3 phút

1. Giảng viên vào tab **Quiz**, chọn bài xuất bản trong khóa Demo soạn bài.
2. **Soạn quiz**: thêm hai câu, mỗi câu bốn lựa chọn, chọn đáp án/giải thích, mức đạt 70%; xuất bản và lưu.
3. Học viên làm đúng một câu, nộp quiz: 50%, cần ôn thêm.
4. **Làm lại quiz**, trả lời đúng cả hai: 100%, đạt; xem phản hồi và lịch sử.
5. Giảng viên xem thống kê quiz; sửa đề và lưu một phiên bản mới.
6. Học viên mở lại lịch sử của lần làm trước đó.

**Mong đợi:** backend tự chấm; sau nộp mới có đáp án/giải thích. Thống kê phiên bản đã làm ghi nhận hai lượt và câu trả lời sai. Lịch sử cũ vẫn gắn với đề cũ. Đạt quiz không tự đánh dấu bài học hoàn thành.

### D07. Hỏi đáp và thông báo tới đúng câu hỏi — 2 phút

1. Học viên chọn bài, gửi câu “Em nên kiểm tra kết quả bài thực hành thế nào?”.
2. Giảng viên mở **Thông báo**, chọn thông báo câu hỏi mới; trả lời trong tab **Hỏi đáp**.
3. Học viên mở thông báo phản hồi, đọc câu hỏi đã được đánh dấu.
4. Giảng viên ẩn câu hỏi; học viên tải lại danh sách; sau đó giảng viên hiện lại.

**Mong đợi:** thông báo tới đúng bài/câu hỏi, kể cả ngoài trang đầu. Câu bị ẩn không xuất hiện với học viên; người quản lý vẫn thấy và hiện lại được. Lỗi gửi không xóa văn bản đang nhập.

### D08. Báo cáo học viên theo khóa — 2 phút

1. Đăng nhập `teacher_scenario`, mở khóa Figma → **Học viên**.
2. Xem tổng toàn khóa, tiến độ, số bài hoàn thành, quiz, hoạt động gần nhất và hàng chờ câu hỏi.
3. Chuyển trang 2, tìm `student_scenario_25`, đổi trạng thái và sắp xếp theo tiến độ.
4. Chọn **Xem câu đầu tiên** trên một học viên có câu hỏi chờ phản hồi.

**Mong đợi:** 10 học viên/trang; tổng toàn khóa giữ nguyên khi lọc/đổi trang. Điểm quiz là trung bình/cao nhất của mọi lượt nộp còn lưu trong khóa. Liên kết mở đúng câu hỏi. Báo cáo không trả email hoặc ghi chú riêng.

### D09. Admin quản lý và kiểm tra mobile — 2 phút, nếu còn thời gian

1. Admin tạo một tài khoản thử; tìm/lọc theo vai trò, khóa rồi mở lại tài khoản đó.
2. Trong phiên của tài khoản thử, gửi request sau khi bị khóa.
3. Admin mở **Báo cáo**, chọn học viên/giảng viên, đổi trang; tổng vẫn tính toàn bộ dữ liệu.
4. Thu màn hình về 390×844, mở/đóng menu, tìm khóa, xem phòng học và báo cáo.

**Mong đợi:** tài khoản bị khóa mất quyền sử dụng ở request tiếp theo. Menu hỗ trợ Escape/Tab; màn hình không tràn ngang. Không khóa tài khoản đang dùng trình diễn.

## 3. Nội dung có thể copy khi soạn bài

````markdown
# Mục tiêu bài học

- Hiểu **biến và kiểu dữ liệu**.
- Chạy được một chương trình Java đơn giản.

## Ví dụ

```java
public class Hello {
    public static void main(String[] args) {
        System.out.println("Hello Course Management!");
    }
}
```

## Bài thực hành

Đổi lời chào thành tên của bạn rồi chạy lại chương trình.
````

Ví dụ quiz: câu 1 hỏi kiểu dữ liệu phù hợp cho số nguyên (A: int, B: String, C: boolean, D: char; đúng A). Câu 2 hỏi lệnh in ra màn hình (A: input(), B: System.out.println(), C: read(), D: scan(); đúng B). Mỗi câu thêm giải thích.

## 4. Checklist kiểm thử thủ công

Ghi mỗi ca: người chạy, thời điểm, dữ liệu, Pass/Fail và screenshot/log nếu lỗi. Các ca thay đổi/xóa dùng tài khoản hoặc khóa thử riêng.

| Mã | Thao tác | Kết quả mong đợi |
|---|---|---|
| T01 | Form đăng nhập rỗng, sai mật khẩu, rồi nhập đúng | Có lỗi dễ đọc; vẫn đăng nhập lại được; đúng dashboard vai trò |
| T02 | Đăng ký tên/email đã tồn tại, email sai, chỉ khoảng trắng | Chặn dữ liệu không hợp lệ, không tạo tài khoản trùng |
| T03 | Khách/học viên mở khóa nháp hoặc đọc bài nháp | Không lộ khóa/bài ngoài quyền; giảng viên phụ trách vẫn đọc được |
| T04 | Học viên chưa đăng ký gọi API nội dung hoặc tải tài liệu | Backend chặn, không chỉ ẩn nút trên UI |
| T05 | Đăng ký miễn phí lặp/đồng thời | Một enrollment cho học viên và khóa |
| T06 | Hoàn thành bài lặp, reload, rồi hoàn thành hết | Không có progress trùng; % đúng số bài published; đạt 100% thì Hoàn thành |
| T07 | Giảng viên ẩn/xuất bản thêm một bài | Tiến độ và trạng thái được tính lại theo chương trình mới |
| T08 | Hai học viên đọc/sửa ghi chú trên cùng bài | Mỗi người chỉ đọc/sửa ghi chú của mình; reload giữ dữ liệu |
| T09 | Thanh toán PENDING hoặc REJECTED, thử vào phòng học | Chưa được cấp quyền; CONFIRMED mới được học |
| T10 | Hai thao tác xác nhận hoặc xác nhận/từ chối đồng thời | Một kết quả nhất quán; không cấp quyền/trùng thông báo ngoài nghiệp vụ |
| T11 | Bài chỉ có video; Markdown chứa HTML hoặc URL javascript | Video được nhận diện; HTML là văn bản, URL không an toàn không chạy |
| T12 | Upload PDF/TXT/ảnh hợp lệ; tệp giả đuôi ảnh, quá 5 MB, tệp thứ 21 | Tệp hợp lệ tải được; tệp sai/đạt giới hạn bị từ chối với thông báo |
| T13 | Giảng viên khác sửa bài, upload/xóa tài liệu hoặc đọc báo cáo khóa | Backend từ chối; không lộ dữ liệu hoặc thay đổi nội dung |
| T14 | Soạn bài, chờ tự lưu, đóng/mở hoặc reload rồi mở lại | Bản nháp khôi phục; bài live không đổi trước Lưu thay đổi |
| T15 | Hai tab sửa cùng bài; hoặc admin cập nhật bài trong khi giảng viên đang soạn | Xung đột khi bản nháp/bài gốc đã đổi; giữ văn bản và bản nháp |
| T16 | Đưa lịch sử vào bản nháp rồi hủy/chưa lưu | Bài live giữ nguyên; lưu lại mới phục hồi nội dung |
| T17 | Mất mạng khi tự lưu hoặc lưu thay đổi, nối lại và thử lại | Hiện lỗi, giữ nội dung; báo đã lưu khi request thành công |
| T18 | Quiz thiếu đáp án, lựa chọn trùng, ngưỡng ngoài 1–100 | Chặn dữ liệu không hợp lệ; không lưu đề sai |
| T19 | Trước nộp xem JSON đề; sau nộp xem phản hồi | Trước nộp không có đáp án/giải thích; sau nộp đúng điểm và phản hồi |
| T20 | Gửi lại cùng submissionKey/đáp án do mất phản hồi | Trả kết quả cũ, không tăng số lượt; cùng key nhưng đáp án khác bị chặn |
| T21 | Giảng viên sửa quiz, học viên nộp đề cũ | Báo xung đột/tải đề mới; lịch sử đã nộp vẫn đọc theo đề cũ |
| T22 | Học viên khác đọc chi tiết bài làm không thuộc mình | Không đọc được; người quản lý khóa đọc theo quyền |
| T23 | Gửi hỏi đáp, trả lời, ẩn/hiện và mở thông báo câu ngoài trang đầu | Lưu/thông báo đúng; câu ẩn không lộ; liên kết tới đúng câu |
| T24 | Báo cáo khóa có nhiều học viên; đổi trang/lọc/tìm tên không có | Dòng không trùng giữa trang; tổng toàn khóa không bị tính theo trang; empty state rõ |
| T25 | Báo cáo quiz có lượt ở khóa khác và phiên bản cũ | Chỉ tổng hợp trong khóa; phiên bản cũ của cùng khóa vẫn được tính |
| T26 | Tìm có %, _, !; thay nhanh từ khóa; danh sách trả chậm | Ký tự được tìm đúng nghĩa; kết quả cũ không đè kết quả mới |
| T27 | Tạo thông báo cho một người; người khác xem/đánh dấu đã đọc | Không lộ hoặc sửa thông báo người khác |
| T28 | Admin khóa tài khoản/đổi quyền/đổi mật khẩu đang có phiên | Phiên cũ hết hiệu lực ở request tiếp theo |
| T29 | Xóa dữ liệu đang được tham chiếu; xóa bài thử riêng | Dữ liệu tham chiếu được bảo vệ; xóa bài thử dọn dữ liệu phụ thuộc của bài |
| T30 | Request ghi không CSRF; request API chưa đăng nhập | Bị chặn; không có thay đổi dữ liệu |
| T31 | Màn hình 390×844; menu Tab/Shift+Tab/Escape; preview dài | Không tràn ngang; focus/đóng menu đúng; nội dung dài cuộn được |
| T32 | Danh mục lọc/trang, reload và Back; HTTP 503 danh sách | Trạng thái URL khôi phục trên màn có đồng bộ URL; lỗi có thử lại |
| T33 | Khách mở chi tiết khóa bằng liên kết có `id` và `#curriculum`; đổi tab bằng End/Home | Trang mở được; tab Chương trình được chọn; bàn phím chuyển đúng tab |
| T34 | HTTP 503 khi mở trang khóa, sau đó bấm Thử lại khi máy chủ hoạt động | Tải lại đúng mã khóa và tab đang mở, không mất tham số URL |
| T35 | Tải tài liệu chậm trong trình soạn; chuyển tab Xem trước; xoay màn hình 844×390 | Preview hoạt động khi tài liệu còn tải; nội dung cuộn và nút Lưu/Hủy vẫn nhìn thấy |
| T36 | Phòng học: lỗi lưu ghi chú, lỗi tải tài liệu và chuyển bài trên mobile | Lỗi lưu giữ bài/ghi chú; tài liệu có nút thử lại, nằm trước hoàn thành; chuyển bài đưa focus và cuộn về tiêu đề |
| T37 | Đổi tab khóa khi là khách rồi đăng nhập từ nút đăng ký | Quay về đúng khóa, query và tab vừa chọn |
| T38 | Đóng trình soạn khi lưu nháp lỗi, sau đó thử lại | Giữ dialog/văn bản đến khi lưu nháp thành công; khóa thao tác trong lúc đóng |
| T39 | Bỏ nháp bài mới có thứ tự lớn hơn 1; tải bài gốc lỗi khi bỏ nháp bài cũ | Giữ thứ tự mặc định của bài mới; lỗi tải bài gốc không xóa nháp máy chủ |
| T40 | Admin sửa hồ sơ/vai trò với email trùng hoặc đổi vai trò admin | Không lưu một phần; đổi vai trò hợp lệ làm phiên cũ hết hiệu lực |
| T41 | Máy chủ nhận thao tác ghi nhưng tải lại màn hình trả HTTP 503 | Báo thao tác đã lưu, đóng hộp soạn và cho tải lại; không gợi ý gửi trùng |
| T42 | Mở hai kết quả quiz liên tiếp, giữ phản hồi cũ chậm; xem lịch sử sau nộp | Kết quả mới nhất được giữ; nút Làm lại quiz vẫn có sau xem lịch sử |
| T43 | Bản nộp quiz trong sessionStorage hỏng; tải thống kê cũ chậm khi lưu đề mới; lỗi tải đề sau 409 | Bỏ bản hỏng và cho làm bài; giữ thống kê phiên bản mới; lỗi tải đề giữ bản soạn và nút thử lại |
| T44 | Khóa đã ngừng học; thông báo có hash/query mã hóa; username/email quá giới hạn | Giải thích trạng thái ngừng học, không cho đăng ký trùng; nhận link nội bộ hợp lệ, chặn dữ liệu quá giới hạn |
| T45 | Khởi động demo khi cổng bị chiếm/database hỏng; chạy lại script khi demo sẵn sàng | Lỗi có hướng dẫn; không báo sẵn sàng giả, dọn tiến trình/PID khi thất bại; chạy lại không tạo tiến trình trùng |

T05/T06/T10/T20/T30 nên kiểm tra thêm bằng API hoặc bộ test tự động, vì chỉ bấm UI không chứng minh được tình huống đồng thời/CSRF/idempotency. Bộ lọc tab Học viên giữ trong phiên trang; không yêu cầu giữ qua reload như danh mục khóa học.

## 5. Chạy kiểm thử tự động

PowerShell tại thư mục dự án:

```powershell
# Dừng demo trước khi Maven đóng gói lại JAR trên Windows.
.\scripts\stop-demo.ps1
.\mvnw.cmd -B -ntp verify
node --test tests/unit/*.test.mjs
.\scripts\start-demo.ps1 -SkipBuild

# Nếu chưa cài dependencies/trình duyệt:
npm ci
npx playwright install chromium

# Sau khi demo đã sẵn sàng:
$env:E2E_BASE_URL = 'http://127.0.0.1:8080'
npm run test:e2e
```

Chỉ khởi động demo nếu Maven thành công. Lần kiểm tra gần nhất: 90 backend trên H2/PostgreSQL, 8 kiểm thử Node và 6 E2E đạt. Playwright CLI kiểm tra 26 trang ở bốn kích thước, cùng các tình huống lỗi bản nháp, tải lại sau ghi và phản hồi quiz về sai thứ tự. Đây là kết quả đã ghi nhận, không phải lần chạy mới từ tài liệu này.

Maven mặc định dùng H2 profile test. Để chạy PostgreSQL thật, tạo database kiểm thử riêng `course_management_test`, rồi cấu hình `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver` trước khi chạy Maven test. Backend test xóa dữ liệu trước mỗi ca, không trỏ vào database demo. Khi quay lại H2, gỡ các biến `SPRING_DATASOURCE_*` đã đặt.

E2E tạo tài khoản và khóa mới trong database demo, giữ dữ liệu sau chạy. Xem báo cáo tại `output/e2e-report/index.html`; ca lỗi có screenshot/trace trong `output/e2e-results`. Các flow mới có thêm kiểm tra thủ công/Playwright CLI, không mặc định thuộc sáu E2E hiện có. Xem chi tiết bằng chứng trong [kế hoạch kiểm thử](KE_HOACH_KIEM_THU.md).
