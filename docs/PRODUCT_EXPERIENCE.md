# Course Management — định hướng trải nghiệm sản phẩm

Ngày rà soát: 07/10/2026. Bản nâng cấp này tập trung vào quyết định chọn khóa, tiếp tục học và công việc theo từng vai trò. Những mục ở phần “Tiếp theo” chưa được triển khai.

## Cơ sở tham khảo

- [Trang khóa học Coursera](https://www.coursera.org/learn/introduction-to-cloud): trình bày kết quả học tập và các module để người học đánh giá nội dung trước khi chọn khóa.
- [Hướng dẫn quản lý khóa học Udemy](https://support.udemy.com/hc/en-us/articles/230048607-How-to-Navigate-the-Course-Management-Dashboard): hướng dẫn chuẩn bị mục tiêu, chương trình, thông tin giới thiệu và xuất bản theo từng bước.
- [Tiến độ bài học trên Udemy](https://support.udemy.com/hc/en-us/articles/229607188-How-to-Mark-or-Unmark-Lectures-as-Complete-on-a-Browser): hiển thị trạng thái hoàn thành ngay trong chương trình khóa học.

Áp dụng các nguyên tắc đó vào quy mô và nghiệp vụ hiện tại: thông tin để quyết định phải có trước đăng ký; phòng học phải giữ được vị trí và suy nghĩ của học viên; bảng tổng quan phải giúp từng vai trò làm việc tiếp theo. Course Management dùng thiết kế và nội dung riêng, không sao chép giao diện hay đưa ra các cam kết chưa có cơ sở.

## Đi qua hệ thống theo từng vai trò

| Vai trò và mục tiêu | Vướng mắc trước đây | Đã nâng cấp | Cách nghiệm thu |
|---|---|---|---|
| Khách: tìm khóa phù hợp trước khi tạo tài khoản | Trang chủ chỉ có thông điệp; mở danh mục bị chuyển sang đăng nhập | Tìm kiếm ngay trang chủ, chủ đề và khóa mới lấy từ dữ liệu thật; danh mục và chương trình công khai | Tìm Java từ trang chủ; xem chi tiết và học phí khi chưa đăng nhập; không nhận nội dung/URL bài học |
| Khách: đăng ký khóa vừa chọn | Đăng nhập xong về dashboard, mất ngữ cảnh | Giữ URL khóa qua đăng nhập và chuyển qua lại đăng ký/đăng nhập | Chọn khóa → đăng nhập → quay về đúng khóa; chỉ cho phép các trang nội bộ đã định danh |
| Học viên: tiếp tục học nhanh | Dashboard chung, chưa lưu bài vừa xem | Thẻ tiếp tục học, tiến độ, cảnh báo thanh toán; máy chủ nhớ bài gần nhất | Mở bài 2, rời phòng học rồi mở lại không có lessonId; vẫn ở bài 2 |
| Học viên: ghi lại điều học được | Không có ghi chú riêng | Ghi chú mỗi bài lưu trên máy chủ; lưu trước đổi bài/rời trang; thông báo chưa lưu và thử lại | Ghi chú giữ qua tải lại; lỗi lưu không đổi bài/mất văn bản; học viên khác và giảng viên không đọc được |
| Học viên: tìm/ôn lại khóa đã học | Danh sách không có tìm kiếm hoặc lọc | Tìm khóa đã đăng ký, lọc đang học/hoàn thành, sắp xếp tiến độ; CTA ôn lại | Lọc khóa hoàn thành; mở lại nội dung; trạng thái và phần trăm giữ nguyên |
| Học viên: đọc bài trên điện thoại | Chương trình dài đứng trước bài học; nhiều điều hướng gây phân tán | Chương trình thu gọn trên mobile, bài trước/sau, chế độ tập trung, thông báo hoàn thành khóa | Chiều rộng 390 px không tràn; mở chương trình và chuyển bài; có thể trở lại điều hướng |
| Giảng viên: chuẩn bị một khóa tốt | Chỉ sửa bài, không sửa được giới thiệu/mục tiêu; chưa biết thiếu gì | Dashboard riêng, checklist giới thiệu/mục tiêu/bài xuất bản/nội dung, sửa metadata của khóa được giao | Giảng viên sửa khóa mình phụ trách; không sửa khóa người khác, đổi học phí/phân công hay trạng thái khóa |
| Admin: xử lý việc chờ | Dashboard chủ yếu là lời chào và thẻ khám phá | Trung tâm vận hành với yêu cầu chờ theo thời gian, khóa nháp cần kiểm tra, lối vào quản lý và báo cáo | Mở nhóm chờ duyệt từ dashboard; bộ lọc PENDING đã được chọn; duyệt vẫn cấp quyền học trong transaction |

Checklist là hướng dẫn chất lượng, không thay thế luật xuất bản: backend vẫn yêu cầu ít nhất một bài xuất bản trước khi mở khóa học. Không hiển thị kết quả học tập tự bịa khi giảng viên chưa nhập.

## Thay đổi dữ liệu và quyền

- `/api/discovery/catalog`, `/api/discovery/categories`, `/api/discovery/courses/{id}` chỉ đọc metadata của khóa PUBLISHED. Khóa nháp/lưu trữ trả 404 kể cả khi gọi discovery bằng phiên admin. Nội dung bài, thanh toán và tiến độ tiếp tục yêu cầu đăng nhập và quyền thích hợp.
- `/api/auth/session` trả hồ sơ của chính phiên hiện tại hoặc `null` cho khách; không trả mật khẩu. Phiên bị khóa/hết hiệu lực được loại bỏ bởi bộ lọc phiên như trước.
- `PUT /api/courses/{id}` cho admin và giảng viên phụ trách. Giảng viên được sửa thông tin mô tả, tên, chủ đề, trình độ, thời lượng và mục tiêu; học phí/phân công giảng viên giữ nguyên. Xuất bản khóa vẫn là quyền admin.
- `GET/PUT /api/enrollments/{id}/notes/{lessonId}` quản lý ghi chú riêng, tối đa 10.000 ký tự. Chuỗi rỗng xóa nội dung ghi chú; ghi chú được hiển thị như văn bản.
- `PUT /api/enrollments/{id}/access_lesson/{lessonId}` lưu vị trí vừa đọc. Cả ghi chú và vị trí chỉ chấp nhận bài đã xuất bản trong khóa của lượt đăng ký còn hiệu lực, thuộc học viên đang đăng nhập.
- Ghi chú và vị trí dùng bản ghi `lesson_progress`, không tự hoàn thành bài. Các thao tác ghi khóa course rồi enrollment theo thứ tự thống nhất, tránh tạo trùng với thao tác hoàn thành bài.
- Flyway V4 bổ sung cột note, V5 chỉ đổi đúng câu nhận diện thương hiệu trong nội dung demo. Giữ dữ liệu còn lại. Không reset database.
- Tên sản phẩm, tiêu đề trang, nhận diện SVG và cấu hình đã đổi thành Course Management.

## Tiếp theo, theo giá trị với người dùng

| Ưu tiên | Hạng mục | Người hưởng lợi | Tiêu chí hoàn thành |
|---|---|---|---|
| Đã triển khai 08/10 | Soạn nội dung có cấu trúc, video và tài liệu trong phòng học | Giảng viên, học viên | Markdown giới hạn, preview dùng chung renderer, YouTube/MP4/WebM, file đính kèm được kiểm tra quyền |
| P2 | Hồ sơ giảng viên, yêu cầu đầu vào và đối tượng phù hợp | Khách, học viên | Người học hiểu khóa dành cho ai và phải chuẩn bị gì; thông tin do giảng viên cung cấp |
| P2 | Lưu khóa quan tâm và đề xuất dựa trên chủ đề đã chọn | Khách, học viên | Lưu giữa thiết bị, gỡ lưu được; đề xuất giải thích được, không gắn nhãn “phổ biến” bằng dữ liệu giả |
| P2 | Mục tiêu học theo tuần và nhắc học tùy chọn | Học viên | Học viên chủ động chọn mục tiêu và bật/tắt nhắc; không tạo streak khi chưa có hoạt động học |
| P2 | Báo cáo nội dung, kiểm duyệt đánh giá và lịch sử thao tác | Admin | Theo dõi người thực hiện, lý do, trạng thái xử lý; giữ lịch sử và quyền học |
| P3 | Chứng nhận có xác minh | Học viên | Chỉ cấp khi có tiêu chí đánh giá năng lực đủ rõ; mã xác minh và khả năng thu hồi hợp lệ |

Trước khi triển khai rộng cần kiểm tra với người dùng thật ở ba vai trò, đo khả năng tìm đúng khóa, quay lại bài, hoàn thành bài tập và xử lý yêu cầu chờ. Tỷ lệ hoàn thành nội dung không tự chứng minh chất lượng học tập.

## Bằng chứng kiểm tra

### Hoàn thiện chi tiết thao tác — 08/10/2026

- **Biểu mẫu:** hiển thị lỗi tiếng Việt sát ô nhập, liên kết lỗi với ô qua `aria-describedby`, đưa focus tới lỗi đầu tiên. Kiểm tra các ràng buộc bắt buộc/email/URL/độ dài/số và nội dung chỉ có khoảng trắng. Mật khẩu có nút hiện/ẩn, giữ nguyên giá trị và không tự bỏ khoảng trắng. Textarea có số ký tự; nút gửi cho biết đang xử lý và chặn gửi lặp.
- **Hộp soạn:** tiêu đề được đặt tên cho dialog. Đóng/Hủy/Escape khi có thay đổi sẽ hiện lựa chọn tiếp tục sửa hoặc bỏ thay đổi ngay trong hộp. Trong lúc lưu, khóa ô nhập và thao tác đóng để nội dung gửi không bị thay đổi giữa chừng. Lỗi API giữ nội dung để thử lại. Rời/tải lại trang khi còn thay đổi dùng cảnh báo gốc của trình duyệt; chưa có lưu nháp tự động.
- **Soạn bài:** tab soạn/xem trước hỗ trợ phím mũi tên và Home/End. Preview dùng cùng bộ hiển thị văn bản/liên kết với phòng học, có số từ và thời gian đọc ước tính 200 từ/phút. Chỉ chấp nhận liên kết HTTP/HTTPS. Preview không lưu hoặc xuất bản; bài mới vẫn phải được xuất bản từ chương trình. Video/tài liệu tiếp tục mở ở thẻ mới; chưa có trình soạn rich text hoặc tải file.
- **Danh mục:** nhãn chỉ rõ từ khóa/chủ đề/học phí/trạng thái đang lọc, gỡ từng điều kiện hoặc xóa tất cả. Trạng thái chọn có `aria-pressed`; khung chờ giữ hình dáng thẻ khóa học và tôn trọng thiết lập giảm chuyển động.
- **Chuyển khoản:** trình bày ba bước tạo yêu cầu → chuyển khoản → chờ đối chiếu, không suy diễn rằng hệ thống đã nhận tiền. Sao chép riêng số tài khoản, số tiền không kèm ký hiệu tiền tệ và nội dung chuyển khoản. Nếu trình duyệt chặn clipboard, chọn văn bản và hướng dẫn sao chép thủ công.
- **Mobile và bàn phím:** menu có nền che, nút đóng, Escape, khóa cuộn nền và giữ Tab trong menu khi mở; nội dung nền không nhận focus. Thu/phóng qua ngưỡng desktop tự đóng menu và khôi phục điều hướng. Trang hiện tại có `aria-current`. Điều chỉnh màu chữ phụ để dễ đọc hơn.

Nghiệm thu trên Chromium: lỗi bắt buộc/focus đầu tiên, hiện/ẩn mật khẩu, preview giữ văn bản và không thực thi HTML, tab bằng bàn phím, Escape giữ bản soạn, URL ngoài HTTP/HTTPS và tiêu đề chỉ có khoảng trắng bị chặn; HTTP 503 giữ nội dung, lưu lại thành công và đọc được sau tải lại. Menu giữ Tab/Shift+Tab, đóng bằng Escape/nền che và khôi phục khi chuyển sang desktop; bộ lọc khôi phục qua Back/tải lại. Clipboard sao chép đúng ba giá trị và có fallback khi bị chặn. Mobile 390 × 844 không tràn ngang. 74 backend trên H2 và sáu E2E hiện có đạt; bản JAR đã đóng gói thành công. Xem bằng chứng và giới hạn trong kế hoạch kiểm thử.

### Danh sách lớn và kiểm tra từng vai trò — 08/10/2026

Demo thêm 36 khóa học và các tài khoản/kịch bản học tập, thanh toán, đánh giá, hỏi đáp, quiz và thông báo vào PostgreSQL. Seeder có marker trong cùng transaction, không lặp dữ liệu hoặc ghi đè chỉnh sửa khi khởi động lại.

Người dùng, thanh toán và thông báo tải 10 mục/trang; việc học tải 9 khóa/trang; đánh giá tải 6/trang; báo cáo tải 8 khóa/trang. Bộ lọc, thứ tự và trang của danh sách chính nằm trong URL; tìm kiếm đổi về trang đầu, trang vượt giới hạn tự điều chỉnh. Các số tổng trên dashboard/báo cáo tính trên toàn bộ phạm vi có quyền, không lấy số dòng trang hiện tại. Chọn giảng viên/học viên/người nhận chỉ tải 20 kết quả và tìm thêm bằng tên, email hoặc tài khoản.

Trang khóa học chia thành các tab thật: kết quả, chương trình, đánh giá, hỏi đáp và quiz theo quyền. Tab hỗ trợ phím mũi tên, Home/End và liên kết hash. Chương trình và điều hướng phòng học hiển thị 8 bài/trang; phòng học tự mở trang chứa bài đang học. Metadata bài học vẫn được tải đầy đủ vì trang cần dùng cho điều hướng và tính mức sẵn sàng; phân trang phần này thực hiện trên giao diện. Form thêm bài tự chọn thứ tự tiếp theo.

Playwright CLI đã dùng khách, học viên, giảng viên và admin trên dữ liệu lớn. Đã kiểm tra tìm kiếm/trang/tải lại, đăng nhập quay về khóa, đăng ký/học, ghi chú, đánh giá, quiz và lịch sử, hỏi đáp/phản hồi/ẩn/hiện, tạo/sửa/khóa/mở/xóa tài khoản, thông báo, soạn/sửa/xuất bản/ẩn/xóa bài, chỉnh sửa/trạng thái khóa, duyệt/từ chối thanh toán và báo cáo. Mô phỏng HTTP 503 có thể thử lại; phản hồi tìm kiếm cũ không ghi đè kết quả mới. Mobile 390 × 844 không tràn ngang; các bảng quản trị cuộn trong khung riêng. Đây là kiểm tra với tài khoản mẫu, chưa thay thế đánh giá của người dùng thật.

74 kiểm thử backend đạt trên H2 và PostgreSQL; sáu E2E đạt sau khi thêm tìm kiếm để truy cập bản ghi ở ngoài trang đầu. Ảnh và script kiểm tra được giữ trong `output/playwright`; kết quả chi tiết ở kế hoạch kiểm thử.

### Nâng cấp quiz theo bài — 07/10/2026

Giảng viên chọn bài tại trang khóa học, soạn 1–20 câu với bốn lựa chọn, một đáp án đúng và giải thích bắt buộc. Có mức đạt, lưu nháp/xuất bản và thống kê câu hay sai. Mỗi lần lưu tạo phiên bản mới; editor kiểm tra revision để tránh ghi đè thầm lặng khi có người sửa đồng thời. Thống kê tính mọi lần làm của phiên bản hiện tại, ghi rõ số học viên duy nhất và số lần làm lại.

Học viên làm quiz ngay trong phòng học hoặc trang khóa, nhận phản hồi từng câu và lịch sử từng phiên bản. Bài làm cũ giữ nguyên đề/đáp án/giải thích, không dùng đáp án đã được sửa để chấm lại. Điểm quiz tách khỏi đánh dấu đọc xong; mức đạt của quiz là mục tiêu luyện tập, chưa phải tiêu chí cấp chứng nhận. Bản này chưa hỗ trợ tự luận, nhiều đáp án đúng, giới hạn lượt hay lịch thi.

Backend kiểm tra đăng ký, bài xuất bản, quyền quản lý và chủ bài làm; chỉ trả đáp án/giải thích cho học viên sau khi nộp. Gửi lại lần nộp cùng UUID và câu trả lời nhận lại kết quả đã có. Giao diện giữ câu trả lời và UUID khi lỗi mạng, kể cả tải lại trang trong cùng phiên trình duyệt; đổi đề lúc đang làm yêu cầu tải phiên bản mới. Bản soạn giảng viên và lựa chọn chưa nộp được giữ khi đổi bài trong phiên trang đang mở, chưa lưu trên máy chủ.

Flyway V7 thêm bảng phiên bản, câu hỏi, lần làm và đáp án; giữ dữ liệu hiện có. Profile demo thêm quiz Java mẫu khi bài demo chưa có quiz. Xóa bài học xóa cả quiz/lần làm; ẩn bài giữ lịch sử và chặn học viên truy cập.

### Nâng cấp hỏi đáp theo bài — 07/10/2026

Học viên đặt câu hỏi trong phòng học hoặc chọn bài tại trang khóa học. Các học viên đã đăng ký có thể đọc câu hỏi công khai trong khóa. Giảng viên phụ trách/admin đọc, trả lời, cập nhật phản hồi và ẩn/hiện câu hỏi ở trang khóa học. Danh sách lấy từng trang 10 câu; backend loại câu hỏi ẩn trước khi tính tổng và phân trang cho học viên. Bản này hỗ trợ một phản hồi hiện tại cho mỗi câu hỏi; trao đổi nhiều tầng và lịch sử sửa phản hồi thuộc giai đoạn sau.

Thông báo cho giảng viên khi có câu hỏi, cho tác giả khi có phản hồi; liên kết mang courseId, lessonId và questionId. Giao diện tải thêm câu hỏi được liên kết nếu nó không nằm ở trang đầu, rồi đưa người dùng tới đúng nội dung. Lỗi gửi giữ nguyên văn bản để thử lại, đổi bài giữ bản nháp trong phiên trang đang mở. Bản nháp chưa gửi không được lưu trên máy chủ.

Quyền hỏi đáp dùng đăng ký còn hiệu lực và bài xuất bản; bài nháp và câu hỏi bị ẩn chỉ dành cho người quản lý khóa. Ghi chú riêng vẫn tách khỏi hỏi đáp công khai. Flyway V6 thêm bảng, không reset database. Lưu câu hỏi/phản hồi và tạo thông báo cùng transaction; lỗi thông báo rollback thay đổi.

Các kiểm thử tự động, log và hành trình UI được ghi trong [kế hoạch kiểm thử](KE_HOACH_KIEM_THU.md). Ảnh desktop/mobile và kịch bản Playwright CLI trong thư mục bị Git bỏ qua `output/playwright`; log backend trong `target/local-demo`. Không dùng dữ liệu thật cho các thanh toán thử.

### Tự lưu bản nháp và lịch sử nội dung — 08/10/2026

Trình soạn khôi phục bản nháp cá nhân từ máy chủ, báo đang lưu/đã lưu/lỗi và cho thử lại. Đóng trình soạn giữ bản nháp; chọn dùng bài hiện tại sẽ bỏ nội dung đang soạn sau xác nhận. Lịch sử cho phép đưa phiên bản trước về trình soạn, chưa cập nhật bài cho đến khi lưu thay đổi. Khóa theo khóa học và revision bảo vệ hai tab sửa đồng thời; thao tác xuất bản cũng lấy bài sau khóa để tránh ghi đè nội dung mới.

82 backend đạt trên H2 và PostgreSQL. Browser đã kiểm tra tự lưu, đóng/mở khôi phục, hai tab báo xung đột giữ nội dung và đưa lịch sử vào bản nháp rồi lưu lại.

### Báo cáo học viên dành cho giảng viên — 08/10/2026

Tab Học viên trong khóa chỉ xuất hiện cho giảng viên phụ trách/quản trị viên. Số tổng toàn khóa không thay đổi theo trang/bộ lọc; các dòng học viên hiển thị tiến độ, quiz, hoạt động và câu hỏi chờ phản hồi. Chọn câu hỏi sẽ mở đúng bài và đánh dấu câu hỏi trong tab Hỏi đáp. Trạng thái ngừng học vẫn giữ dữ liệu lịch sử; ghi chú riêng không xuất hiện.

Nghiệm thu Chromium bằng khóa mẫu 27 học viên: phân trang, tìm tài khoản, lọc trạng thái không có kết quả, sắp xếp, liên kết tới câu hỏi và cả tài khoản giảng viên phụ trách đạt. Desktop và mobile 390px không tràn ngang. 86 backend trên H2/PostgreSQL và sáu hành trình E2E đạt.

### Hoàn thiện khám phá, soạn bài và phòng học — 08/10/2026

Khách mở lại được chi tiết khóa và các tab chương trình/cách học. Lỗi mở trang có nút tải lại giữ đúng mã khóa và tab. Trình soạn cho xem trước ngay khi tài liệu còn đang tải; trên màn hình thấp, nội dung cuộn trong hộp và các nút Lưu/Hủy vẫn hiển thị.

Phòng học đặt tài liệu ngay sau nội dung, trước nút hoàn thành; hướng dẫn xem/tải tài liệu dành cho học viên, còn giới hạn upload chỉ dành cho người soạn. Chuyển bài đưa focus và vị trí cuộn về tiêu đề để người học bắt đầu đọc, thay vì giữ ở cuối bài cũ. Lỗi lưu ghi chú vẫn giữ văn bản và ngăn chuyển bài; lỗi tải tài liệu có thể thử lại. Bốn renderer, sáu E2E và kiểm tra Chromium desktop/mobile/màn hình ngang đạt trong đợt này.
