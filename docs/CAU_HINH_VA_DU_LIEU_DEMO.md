# Dữ liệu và cài đặt trong database

## Chỉnh ngay trong ứng dụng

Đăng nhập admin → **Cài đặt** (`/settings.html`) để đổi:

- Dung lượng một tài liệu, số tài liệu mỗi bài, số câu mỗi quiz.
- Điểm đạt mặc định, chủ đề và trình độ điền sẵn khi tạo mới.
- Ngân hàng, số tài khoản, chủ tài khoản và mã BIN ngân hàng dùng trong hướng dẫn thanh toán/QR.

Các giá trị nằm ở một bản ghi trong bảng `application_settings`, được Flyway V13 tạo với giá trị ban đầu. Backend đọc database khi xử lý yêu cầu, không cần build hoặc khởi động lại sau khi lưu. Tải lại các trang đang mở để giao diện nhận cài đặt mới. Mặc định áp dụng cho nội dung mới; bài, quiz và khóa đã lưu không bị ghi đè. Giảm giới hạn không xóa tài liệu hoặc câu hỏi cũ; lần thêm tài liệu/lưu quiz tiếp theo dùng giới hạn mới.

`GET /api/settings` và `PUT /api/settings` chỉ dành cho admin. Request lưu cần CSRF và `revision`; nếu hai admin sửa cùng lúc, lượt lưu từ phiên bản cũ trả HTTP 409 và giữ dữ liệu mới. `GET /api/auth/config` chỉ trả các cài đặt học tập cần cho giao diện công khai.

Khóa học, học phí, chủ đề, trình độ và phân công giảng viên chỉnh trong quản lý khóa học. Tiêu đề bài, nội dung, `videoUrl` YouTube và timestamp chỉnh trong trình soạn bài. Đề, điểm đạt và câu hỏi chỉnh ở tab Quiz. Tài khoản chỉnh ở quản lý người dùng. Đây đều là bản ghi database, ứng dụng không lấy nội dung đang dùng từ file mẫu.

## QR chuyển khoản

Flyway V14 bổ sung `bank_bin`, mặc định để trống để giữ thông tin ngân hàng cũ. Admin điền mã BIN 6 chữ số đúng với ngân hàng nhận và tài khoản gồm 1–19 ký tự chữ/số để bật QR. Tra cứu mã tại [danh sách ngân hàng VietQR.io](https://www.vietqr.io/danh-sach-api/api-danh-sach-ngan-hang/); không dùng số tài khoản giả của demo để chuyển tiền.

Hướng dẫn xuất hiện khi tạo yêu cầu ở trang khóa hoặc mở lại yêu cầu chờ trong Thanh toán của tôi. QR dùng số tiền đã lưu trên yêu cầu và mã chuyển khoản riêng `TT...`, không dùng học phí hiện tại của khóa. Chỉ yêu cầu PENDING, số tiền VND nguyên dương tối đa 13 chữ số và thông tin hợp lệ mới tạo QR; trường hợp khác vẫn hiển thị hướng dẫn thủ công. Quét mã không xác nhận đã nhận tiền; admin tiếp tục đối chiếu, xác nhận và mở quyền học theo luồng hiện có.

Ảnh lấy từ dịch vụ bên ngoài theo [VietQR.io Quick Link](https://www.vietqr.io/danh-sach-api/link-tao-ma-nhanh/), gửi BIN, tài khoản, tên người nhận, số tiền và nội dung chuyển khoản; không gửi tên học viên hoặc khóa học. Cần Internet. Lỗi hoặc tải quá 15 giây có nút thử lại, các nút sao chép thông tin vẫn dùng được. Trên điện thoại có thể lưu ảnh QR rồi chọn từ thư viện trong ứng dụng ngân hàng hỗ trợ. Template `qr_only` dùng cho bản demo; tài liệu nhà cung cấp yêu cầu tạo template riêng khi dùng cho dự án chính thức.

## Khởi tạo demo

`src/main/resources/demo/catalog.json` chỉ là dữ liệu để tạo database demo ban đầu. Seeder ghi nội dung vào các bảng `users`, `courses`, `lessons`, bảng quiz và các bảng nghiệp vụ liên quan. Không dùng JSON làm nơi lưu thay đổi trong ứng dụng.

Các marker `base-demo-courses-v1`, `role-scenarios-v1`, `youtube-demo-courses-v1` và `demo-login-accounts-v1` trong `demo_seed_runs` ngăn chạy lại bộ mẫu đã hoàn tất. Database cũ đã có khóa chỉ được ghi marker bộ nền; không thêm lại sáu khóa nền. Khởi động lại giữ nội dung đã sửa; không xóa marker để thay video.

Danh sách tài khoản đăng nhập nhanh nằm trong `demo_login_accounts` và liên kết bằng `user_id`. Tên/vai trò được đọc từ `users`, nên đổi username không cần sửa JavaScript hoặc file mẫu. Chỉ hiện trong chế độ demo, với tài khoản hoạt động và còn dùng mật khẩu mẫu. Mật khẩu tài khoản vẫn được băm trong `users.password_hash`. `DEMO_PASSWORD` chỉ dùng khởi tạo và điền nhanh cho tài khoản mẫu; đổi biến không ghi đè mật khẩu đã lưu.

## Cấu hình hạ tầng

Kết nối database (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`), cổng (`PORT`), thời gian phiên (`SESSION_TIMEOUT`), mật khẩu/email khởi tạo demo (`DEMO_PASSWORD`, `DEMO_EMAIL_DOMAIN`) và trần HTTP multipart vẫn thuộc cấu hình máy chủ. Các giá trị này cần có trước khi ứng dụng kết nối database hoặc do servlet container quản lý.

`LESSON_MAX_FILE_SIZE` mặc định `20MB`, `LESSON_MAX_REQUEST_SIZE` mặc định `21MB`. Đây là trần của máy chủ; giới hạn tài liệu thực tế lưu trong database, mặc định 5 MB. Admin không thể lưu dung lượng vượt trần máy chủ. Nếu hạ trần HTTP, API công khai và backend tự giới hạn theo trần mới. `.env.example` chỉ minh họa; Spring Boot không tự đọc file `.env`.

Role, trạng thái, loại file hỗ trợ, nhãn giao diện và bốn lựa chọn quiz vẫn thuộc cấu trúc ứng dụng. Bốn lựa chọn khớp các cột `option_a`–`option_d`; đổi số lựa chọn cần thay schema. Giới hạn độ dài của trường khớp DTO/schema. Màu minh họa khóa được chọn tự động từ tên chủ đề lưu trong database.

## Hồ sơ giảng viên và thông tin chọn khóa

Giảng viên mở **Hồ sơ của tôi → Hồ sơ giảng viên công khai** để lưu giới thiệu (tối đa 10.000 ký tự) và chuyên môn (2.000 ký tự, mỗi dòng một mục). Tên công khai dùng họ và tên hiện tại; email và username không nằm trong hồ sơ công khai. `GET/PUT /api/teacher-profile` chỉ thao tác với giảng viên đang đăng nhập, ghi yêu cầu CSRF. Trường bị bỏ qua giữ nguyên; chuỗi rỗng xóa nội dung.

`GET /api/discovery/teachers/{id}` trả tên, giới thiệu/chuyên môn của giảng viên đang hoạt động. `GET /api/discovery/teachers/{id}/courses?page=0&size=9` chỉ trả khóa PUBLISHED của người đó, size tối đa 100, kể cả khi người xem là admin. Trang `/teacher.html?id=...` xem được trước đăng nhập; giảng viên bị khóa hoặc đổi vai trò không còn hồ sơ công khai.

Giảng viên phụ trách/admin sửa **Đối tượng phù hợp** và **Yêu cầu đầu vào** trong hộp chỉnh sửa khóa, mỗi trường tối đa 10.000 ký tự, mỗi dòng một mục. Cả hai được lưu khi tạo/sửa khóa và hiển thị ở tab **Đối tượng & chuẩn bị**; tab **Giảng viên** hiển thị hồ sơ và liên kết danh sách khóa. API sửa khóa cũ không gửi hai trường vẫn giữ nội dung đã lưu; gửi chuỗi rỗng để xóa. Quyền đổi học phí/phân công/xuất bản vẫn do admin quản lý.

Flyway V16 thêm `users.biography`, `users.expertise`, `courses.prerequisites`, `courses.target_audience`. Hồ sơ và khóa ngoài bộ demo chưa có nội dung hiển thị trạng thái đang cập nhật.

Catalog demo có giới thiệu/chuyên môn cho hai giảng viên mẫu và đối tượng/yêu cầu phù hợp cho 44 khóa mẫu (6 khóa nền tảng, 2 khóa YouTube và 36 khóa tình huống). Các phần này được lưu ngay khi tạo mới. Hồ sơ ghi rõ là mẫu, không đưa ra chứng chỉ hoặc số năm kinh nghiệm giả.

Database demo đã có dữ liệu được bổ sung một lần sau khi các bộ mẫu khởi tạo, với marker `demo-public-profiles-and-course-fit-v1` trong `demo_seed_runs`. Chỉ điền trường trống của hai tài khoản giảng viên demo và các khóa họ phụ trách; tên, học phí, trạng thái, bài học và nội dung đã nhập giữ nguyên. Khóa trùng tên catalog nhận nội dung theo khóa; khóa đã đổi tên hoặc thêm để trình diễn dùng mẫu theo chủ đề, lấy từ `courseFitDefaults` trong JSON. Khởi động lại không điền lại phần đã chủ động xóa, không tạo lại khóa đã xóa và không tạo tài khoản demo bị thiếu. Profile thường không chạy bộ bổ sung này.

## Bổ sung nội dung cho database đã khởi tạo

Sáu khóa nền trong `catalog.json` có mô tả/mục tiêu riêng, 24 bài Markdown theo chủ đề và sáu quiz tổng kết, mỗi quiz năm câu. Bài thực hành gồm Java, thiết kế giao diện, SQL, HTML/CSS/JavaScript, Git và Spring Boot, có ví dụ, bài tập và cách tự đối chiếu kết quả. Quiz tổng kết ở bài cuối; quiz Java chuẩn bị môi trường ở bài đầu tiếp tục được giữ. Bộ 36 khóa tình huống vẫn phục vụ kiểm tra danh sách lớn và nghiệp vụ, chưa được nâng thành nội dung đào tạo đầy đủ.

Database mới dùng seeder hiện có để lưu nội dung từ catalog. Database demo đã khởi tạo cần nhập bổ sung qua `scripts/update-demo-content.py`; không xóa marker hay reset database. Script dùng thư viện chuẩn Python và các API quản lý hiện có, nên nội dung, lịch sử bài và quiz tiếp tục nằm trong database, chỉnh được bằng giao diện. Không cần khởi động lại để nhìn thấy dữ liệu vừa nhập. Bản JAR dùng để khởi tạo database mới cần được build từ catalog mới.

`content-update-baseline.json` chứa tên khóa/tài khoản tham chiếu, nội dung mẫu trước cập nhật và revision mong đợi. Nội dung mới lấy từ catalog; một mục bổ sung trong file baseline điền mục tiêu cho khóa cũ **Nhập môn Java - Demo** chỉ khi trường đang là `null`. Không có tên khóa, mã database, nội dung bài hay đáp án được cố định trong script hoặc Java/JavaScript.

PowerShell, từ thư mục dự án, với tài khoản admin hiện tại của bản demo:

```powershell
$env:COURSE_IMPORT_PASSWORD = 'mat-khau-admin-hien-tai'
python scripts/update-demo-content.py --base-url http://127.0.0.1:8080 --username admin_demo --report output/data-import/preview.json
# Đọc báo cáo preview; thêm --apply để ghi dữ liệu:
python scripts/update-demo-content.py --base-url http://127.0.0.1:8080 --username admin_demo --apply --report output/data-import/applied.json
Remove-Item Env:COURSE_IMPORT_PASSWORD
```

Mặc định chỉ xem trước. Script chỉ chấp nhận máy chủ đang bật demo và phiên admin. Chọn khóa bằng đúng tên và giảng viên tham chiếu trong JSON; thiếu hoặc trùng thì bỏ qua, không tạo lại bản ghi. Mỗi trường mô tả/mục tiêu chỉ được thay khi khớp dữ liệu cũ. Bài chỉ được sửa khi tên, nội dung, định dạng, thứ tự, video và revision vẫn khớp baseline; URL tài liệu đã thêm được giữ. API lưu bài giữ lịch sử, kiểm tra revision, không đổi trạng thái xuất bản hoặc tiêu thụ bản nháp. Quiz tổng kết chỉ được tạo khi bài cuối phù hợp và chưa có quiz; không sửa đề hay bài làm hiện có.

Báo cáo JSON lưu nội dung trước ghi, body gửi và kết quả từng thao tác. Mỗi API ghi là một transaction riêng; nếu lỗi giữa chừng, các thao tác trước có thể đã lưu. Đọc báo cáo rồi chạy lại để hoàn tất phần còn lại. Chạy lại sau khi hoàn tất không tạo thêm phiên bản bài hoặc quiz. Script đọc lại bản ghi ngay trước khi ghi; nên chạy khi người dùng không đồng thời sửa khóa, vì API metadata khóa hiện chưa có revision để khóa xung đột giữa lần đọc và lần ghi. Các lần nâng nội dung sau dùng một baseline mới qua `--baseline` với đúng nội dung/revision cũ, không cần sửa thuật toán nhập.

Kiểm tra cơ chế giữ dữ liệu: `python -m unittest discover -s tests/unit -p test_demo_content_import.py -v`.
