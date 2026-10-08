# Kế hoạch và kết quả kiểm thử bản làm lại

Phạm vi: đồ án/demo quản lý khóa học, với ba vai trò, nội dung mẫu và thanh toán chuyển khoản do admin duyệt. Kiểm thử xác nhận nghiệp vụ và lỗi hồi quy; chưa dùng kết quả này để khẳng định khả năng chịu tải hay tích hợp ngân hàng thật.

## Các tầng kiểm thử

| Tầng | Công cụ và môi trường | Mục tiêu |
|---|---|---|
| Nghiệp vụ và API | JUnit, Spring Boot, MockMvc, repository thật | Phân quyền, HTTP, validation, session, transaction và ràng buộc dữ liệu |
| Database | H2 PostgreSQL mode và PostgreSQL 18 | Chạy Flyway thật, kiểm tra unique index, rollback và khóa dữ liệu khi có request đồng thời |
| Giao diện | Playwright + Chromium | Hành trình người dùng qua form, điều hướng, modal, lưu/tải lại dữ liệu và màn hình nhỏ |
| CI | GitHub Actions | Chạy lại sau mỗi push/PR, lưu JUnit XML, báo cáo HTML, screenshot/trace khi thất bại |

## Đối chiếu các lỗi đã sửa

Các tên dưới đây nằm trong `src/test/java/com/example/course_management/BusinessFlowTests.java`.

| Lỗi/rủi ro nghiệp vụ | Test tiêu biểu | Điều kiện đạt |
|---|---|---|
| Lộ nội dung/URL bài học cho người chưa đăng ký | `unregisteredLessonListRedactsContent`, `unrelatedTeacherCannotReadFullContent`, `registeredStudentCanReadContent` | Metadata không chứa nội dung; đọc đầy đủ chỉ khi có quyền |
| Lộ khóa nháp; giảng viên không thấy bản nháp của mình | `draftCourseDoesNotLeakLessons`, `teacherCanFindOwnDraftButNotOthers` | Giảng viên thấy khóa của mình, người khác không thấy khóa nháp |
| Đăng ký thẳng khóa có phí | `paidCourseCannotBeEnrolledDirectly` | Bị từ chối, không có enrollment |
| Xác nhận thanh toán không cấp quyền một cách nguyên tử | `paymentConfirmationGrantsExactlyOneEnrollment`, `confirmationRollsBackIfDownstreamWriteFails` | Thanh toán, enrollment và thông báo commit cùng nhau; khi lỗi phải rollback |
| Request đồng thời tạo bản ghi trùng | `concurrentPaymentRequestsCreateOnlyOne`, `concurrentEnrollmentsCreateOnlyOne`, `databaseRejectsDuplicateEnrollment` | Chỉ một bản ghi hợp lệ; có unique index bảo vệ |
| Hai admin xử lý cùng thanh toán và đọc trạng thái cũ | `concurrentConfirmationsGrantAccessOnlyOnce`, `concurrentConfirmAndRejectHaveOneConsistentOutcome` | Chỉ một thao tác thành công, trạng thái cuối khớp quyền học |
| Thanh toán bị từ chối vẫn cấp quyền | `rejectedPaymentDoesNotGrantAccessAndCanBeRequestedAgain` | Không cấp quyền; cho phép yêu cầu lại |
| Phiên cũ tiếp tục dùng sau khóa/đổi quyền/đổi mật khẩu | `blockedAccountLosesExistingSession`, `roleChangeRevokesExistingSession`, `passwordChangeRevokesOldSessionAndOldPassword` | Phiên cũ trả 401; mật khẩu cũ không đăng nhập được |
| Tiến độ sai khi chương trình thay đổi | `publicationChangesRecalculateProgressAndCompletion`, `deletingLessonRemovesProgressAndRecalculates`, `repeatedCompletionIsIdempotent` | Chỉ đếm bài published; không vượt 100%; không thêm bản ghi khi hoàn thành lặp |
| Hoàn thành bài nháp, bài của khóa khác hoặc enrollment người khác | `unpublishedLessonCannotBeCompleted`, `lessonFromDifferentCourseCannotAffectProgress`, `enrollmentCannotBeReadOrCompletedByAnotherUser` | Bị từ chối và dữ liệu tiến độ giữ nguyên |
| Nâng quyền, ghi thiếu CSRF, dữ liệu không hợp lệ | `registrationCannotEscalateRole`, `studentCannotConfirmPaymentEvenWithValidCsrf`, `authenticatedWriteWithoutCsrfDoesNotChangeData`, `negativeCoursePriceIsRejected`, `malformedRequestIsClientErrorAndAnonymousApiRequiresLogin` | Đúng HTTP 400/401/403 và không làm thay đổi dữ liệu |
| Liên kết nguy hiểm và HTML trong hồ sơ | `dangerousContentLinkIsRejected` và E2E hồ sơ | Từ chối URL nguy hiểm; HTML hiển thị như văn bản và không sinh event handler |
| Tổng/trang danh mục sai hoặc lộ khóa nháp qua bộ lọc/chủ đề | `catalogPaginatesBeforeReturningVisibleCourses`, `catalogAndCategoriesRespectEachRole` | Quyền xem áp dụng trước phân trang/tính tổng; metadata và chủ đề không lộ dữ liệu riêng |
| Tìm/lọc hoặc thứ tự trang không ổn định | `catalogCombinesSearchCategoryPriceAndTeacherFilters`, `catalogSearchTreatsWildcardCharactersLiterally`, `catalogSortHasStableTieBreakerAcrossPages` | Kết hợp bộ lọc đúng; tìm theo giảng viên/mô tả; wildcard là ký tự thường; ID phân định khi bằng giá/tên |
| Thống kê sai hoặc tăng truy vấn theo số khóa | `catalogReturnsCorrectAggregatesWithoutCountingDraftLessons`, `catalogQueryCountDoesNotGrowWithCourseCount` | Không tính bài nháp; khóa chưa có thống kê trả 0/null; tối đa 5 truy vấn cho trang 9 khóa |
| Tham số phân trang không hợp lệ | `catalogRejectsInvalidParametersAndRequiresLogin` | HTTP 400 cho tham số sai/quá giới hạn và 401 nếu chưa đăng nhập |

## Sáu hành trình giao diện

Nguồn: `tests/e2e/journeys.spec.cjs`. Mỗi ca dùng tài khoản mới hoặc vai trò demo thích hợp. Playwright kiểm tra trạng thái hiển thị sau thao tác; không dùng sleep cố định để chờ dữ liệu.

1. Nhập sai mật khẩu, thấy lỗi, sửa mật khẩu và đăng nhập thành công.
2. Đăng ký qua form, đăng ký khóa miễn phí, hoàn thành bốn bài, đạt 100%, tải lại vẫn giữ tiến độ và trạng thái hoàn thành.
3. Tạo yêu cầu khóa trả phí, chưa được vào phòng học; admin duyệt ở một phiên riêng, học viên tải lại và đọc được nội dung.
4. Lưu tên chứa thẻ HTML/event handler, tải lại vẫn giữ văn bản và không chèn thẻ hoặc event handler vào DOM.
5. Đăng nhập ở 390 × 844, mở menu và đi tới danh sách khóa học; không tràn ngang.
6. Giảng viên vào khóa nháp được giao, thêm nội dung, xuất bản bài và tải lại để kiểm tra dữ liệu đã lưu.

Mọi ca kiểm tra thêm rằng trang không phát sinh lỗi JavaScript chưa được xử lý. Ca lỗi tự lưu screenshot và trace để điều tra.

## Chạy lại

Backend H2, không cần PostgreSQL:

```powershell
.\mvnw.cmd -B -ntp verify
```

Backend PostgreSQL: tạo riêng database `course_management_test`, rồi đặt các biến sau (mật khẩu lấy từ môi trường của người chạy):

```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5432/course_management_test'
$env:SPRING_DATASOURCE_USERNAME = 'postgres'
$env:SPRING_DATASOURCE_PASSWORD = $env:DB_PASSWORD
$env:SPRING_DATASOURCE_DRIVER_CLASS_NAME = 'org.postgresql.Driver'
.\mvnw.cmd -B -ntp test
```

Mỗi ca backend dọn 14 bảng (gồm bảng đánh dấu dữ liệu demo, hỏi đáp và bốn bảng quiz) trong database test trước khi tạo fixture. Không dùng database demo hoặc database thật. Xóa các biến `SPRING_DATASOURCE_*` trên sau khi test nếu tiếp tục chạy ứng dụng trong cùng terminal.

Giao diện: làm theo mục Kiểm tra trong README. Chỉ trỏ E2E tới bản demo; các ca tạo tài khoản, enrollment, thanh toán giả và một khóa nháp mới.

## Kết quả đã chạy tại máy ngày 07/10/2026

| Kiểm tra | Kết quả |
|---|---|
| Maven verify, JDK 21 + H2 | 48 test, 0 failure, 0 error; Flyway V5 đạt, đóng gói JAR thành công |
| Maven test, JDK 21 + PostgreSQL 18 | 48 test, 0 failure, 0 error; Flyway V5 đạt |
| Playwright Chromium, backend thật trên PostgreSQL demo | 6/6 hành trình đạt |
| Cú pháp module JavaScript | `api.js` và `app.js` hợp lệ |
| Giao diện thủ công | Đã xem login, dashboard, phòng học desktop và mobile |
| Playwright CLI, trải nghiệm sản phẩm | Khách tìm Java từ trang chủ, xem chương trình công khai, đăng nhập về đúng khóa; giảng viên sửa giới thiệu/mục tiêu của khóa được giao và tải lại thấy checklist 4/4; dashboard ba vai trò và mobile 390 px |
| Playwright CLI, ghi chú và phòng học | Ghi chú giữ khi đổi bài/tải lại; mô phỏng lỗi lưu giữ nguyên văn bản và bài; HTML trong ghi chú không thực thi; quay lại bài gần nhất không có lessonId; chế độ tập trung; không tạo liên kết /null |
| Playwright CLI, danh mục mới | Chuyển trang 1/2, tải lại giữ trang; tìm giảng viên + lọc miễn phí + sắp xếp; quay lại khôi phục bộ lọc; chặn request tạo lỗi và thử lại thành công; phản hồi Java đến sau SQL không ghi đè kết quả |
| Danh mục trên điện thoại | 390 × 844, không tràn ngang; đã xem ảnh chụp desktop/mobile trong `output/playwright` |

Các log chạy tại máy và báo cáo nằm trong `target/local-demo` và `output`, không đưa vào Git. CI tạo bằng chứng độc lập trong mục Actions của repo. Các bài kiểm tra tải, nhiều trình duyệt và cổng thanh toán thật thuộc giai đoạn tiếp theo.


## Kiểm thử bổ sung cho trải nghiệm theo vai trò

| Ca backend | Điều được bảo vệ |
|---|---|
| `guestsCanDiscoverPublishedCoursesWithoutReceivingLessonContent` | Guest chỉ xem khóa published; metadata chương trình không chứa textContent/contentUrl; khóa draft trả 404 cả với phiên admin |
| `publicSessionReturnsOnlyTheCurrentActiveProfile` | Guest nhận null; phiên hợp lệ chỉ nhận hồ sơ của chính mình; khóa tài khoản vô hiệu hóa phiên |
| `teacherCanImproveOwnCourseMetadataButCannotChangeAssignmentOrPrice` | Giảng viên sửa mục tiêu của khóa phụ trách; không đổi học phí/phân công/trạng thái; người khác và học viên bị chặn |
| `privateNotesPersistWithoutCompletingLessonsAndSurviveCompletion` | Ghi chú lưu/đọc/xóa bằng chuỗi rỗng; không tăng tiến độ; hoàn thành bài giữ ghi chú và không tạo progress trùng |
| `notesAndResumeRequireAnActiveOwnerAndPublishedLessonInTheirCourse` | Không đọc/ghi ghi chú người khác; không ghi lên bài draft hoặc khóa khác; lượt dropped không còn quyền |
| `recentLessonIsRestoredAndHiddenLessonsAreExcludedFromResume` | Nhớ bài vừa đọc; bài bị ẩn không được dùng làm vị trí quay lại; đọc bài không tự hoàn thành |
| `notesValidateSizeAndCsrf` | Chặn note null/quá 10.000 ký tự và request thiếu CSRF; không ghi dữ liệu lỗi |

Giữ sáu hành trình E2E hiện có; cập nhật tiêu đề dashboard được kỳ vọng theo từng vai trò. Kiểm tra UI mới dùng Playwright CLI, không thay backend bằng mock. Riêng ca lỗi ghi chú chặn một request PUT để xác nhận hành vi thử lại; dữ liệu còn lại đi qua API và PostgreSQL thật.

## Kiểm thử hỏi đáp theo bài — 07/10/2026

| Ca backend bổ sung | Điều được bảo vệ |
|---|---|
| `questionsRequireEnrollmentAndStayInsidePublishedLessons` | Khách, người chưa đăng ký, giảng viên ngoài khóa, lượt dropped và bài nháp không có quyền tham gia |
| `questionAnswersNotifyTheAuthorAndLinkToTheExactContext` | Giảng viên/admin trả lời; giảng viên khác/học viên bị chặn; thông báo chứa đúng khóa/bài/câu hỏi; hỏi đáp không tăng tiến độ |
| `hiddenQuestionsAreExcludedBeforeCountingAndCanBeRestored` | Học viên không đọc trực tiếp hay nhận tổng số câu hỏi ẩn; quản lý thấy và hiện lại được; không trả lời câu hỏi đang ẩn |
| `questionsPaginateByNewestIdWithoutLeakingOtherLessons` | Mới nhất trước, trang không trùng, không lẫn bài khác, chặn page/size sai |
| `questionWritesValidateTextRolesAndCsrf` | Chặn nội dung trống/quá 5000 ký tự, vai trò sai, thiếu CSRF và hidden null |
| `questionAndAnswerRollbackIfTheirNotificationFails` | Lỗi tạo thông báo rollback câu hỏi/phản hồi, giữ dữ liệu nhất quán |
| `removingLessonCascadesQuestionsAndDoesNotBreakProgress` | Xóa bài không để câu hỏi mồ côi, nghiệp vụ xóa/tiến độ tiếp tục hoạt động |

Kết quả: 55/55 backend đạt trên H2 và PostgreSQL, Flyway V6 chạy thành công; sáu hành trình E2E hiện có đạt. Cú pháp `api.js`, `app.js`, `questions.js` hợp lệ. Log mới ở `target/local-demo/questions-*`; báo cáo E2E ở `output/e2e-report`.

Playwright CLI trên backend PostgreSQL demo đã kiểm tra học viên gửi → thông báo giảng viên → trả lời → thông báo học viên → đọc đúng câu hỏi, ẩn câu hỏi khỏi học viên, HTML hiển thị như văn bản, lỗi POST 503 giữ văn bản/thử lại thành công. Với 11 câu hỏi công khai, trang 2 hiển thị đúng; tải lại liên kết thông báo tải thêm câu hỏi cũ ở ngoài trang đầu. Kiểm tra 390 × 844 không tràn ngang; ảnh đã xem ở `output/playwright/questions-mobile.png` và `questions-teacher-desktop.png`. Script CLI phân trang ở `output/playwright/questions-pagination.js`. Các bằng chứng này bị Git bỏ qua; CI tự chạy bảy ca backend mới, sáu E2E hiện có và kiểm tra cú pháp module mới.

## Kiểm thử quiz theo bài — 07/10/2026

| Ca backend bổ sung | Điều được bảo vệ |
|---|---|
| `quizDraftsAndAnswerKeysAreNotVisibleToLearnersBeforeSubmission` | Quiz nháp/đáp án/giải thích không lộ cho học viên; khách bị chặn |
| `quizSubmissionGradesOnServerAndKeepsProgressSeparate` | Chấm đúng/sai, mức đạt và phản hồi; không đổi tiến độ đọc |
| `quizPassingUsesTheExactFractionAndDisplaysAnUnroundedScore` | 2/3 hiển thị 66%, đạt mức 66 nhưng chưa đạt mức 67 |
| `quizVersionsPreserveOldResultsAndRejectStaleEditorsAndSubmissions` | Lịch sử giữ đề cũ; editor/đề nộp cũ trả 409; retry đã lưu vẫn trả đúng lần làm |
| `quizHistoryIsPrivateAndRequiresAnActiveEnrollmentAndVisibleLesson` | Học viên khác không xem bài làm; giảng viên khác bị chặn; dropped/bài ẩn mất quyền |
| `quizStatisticsCountAttemptsAndWrongAnswersForCurrentVersionOnly` | Thống kê số lần làm/học viên, điểm và câu sai đúng theo phiên bản |
| `repeatedAndConcurrentQuizSubmissionsStoreExactlyOneAttempt` | Hai request đồng thời cùng UUID tạo một lần làm; UUID dùng cho nội dung khác bị chặn |
| `quizHistoryPaginatesAndInvalidAnswersDoNotCreateAttempts` | Phân trang mới nhất trước, tham số/lựa chọn sai không tạo bản ghi |
| `quizEndpointsValidateNestedQuestionsRolesAndCsrf` | Validate bốn lựa chọn khác nhau, giải thích, ngưỡng đạt, câu trả lời; vai trò/CSRF được bảo vệ |
| `deletingLessonRemovesItsQuizzesAndAttemptsWithoutOrphans` | Xóa bài không để phiên bản, câu hỏi hay bài làm mồ côi |

Kết quả chạy mới: Maven verify đạt 65/65 trên H2 và đóng gói JAR; PostgreSQL đạt 65/65 với Flyway V7; sáu E2E hiện có đạt. Kiểm tra cú pháp thêm `quiz.js` vào CI. Log ở `target/local-demo/quiz-verify.log`, `quiz-postgres.log`, `quiz-e2e.log`.

Playwright CLI kiểm tra giảng viên soạn/lưu phiên bản, học viên làm 2/3 đạt 66% và làm đúng đạt 100%, đọc phản hồi và lịch sử, thống kê giảng viên ghi nhận đúng câu sai. Bài đạt 100% vẫn giữ tiến độ đọc 0%. Mô phỏng mất phản hồi sau khi backend thật đã lưu, tải lại vẫn giữ lựa chọn/UUID; thử nhận kết quả trả bài cũ và lịch sử chỉ có hai lần làm, không tạo lần thứ ba. Script ở `output/playwright/quiz-lost-response.js`. Hai editor mở cùng phiên bản: lưu từ tab thứ nhất thành công, tab thứ hai báo xung đột và giữ nội dung; học viên nộp đề cũ cũng báo tải đề mới. Mobile 390 × 844 không tràn ngang. Ảnh ở `output/playwright/quiz-student-mobile.png` và `quiz-teacher-desktop.png`.

## Danh sách lớn và dữ liệu theo vai trò — Flyway V8/V9

Backend hiện đạt 74/74 trên H2 và PostgreSQL thật. Log: `target/local-demo/role-upgrade-verify.log`, `paged-lists-postgres.log`.

| Ca kiểm thử mới | Tiêu chí |
| --- | --- |
| `pagedUsersFilterBeforeCountingAndTreatWildcardsLiterally` | Bộ lọc trước phân trang; `%`, `_` là ký tự tìm kiếm thật |
| `pagedPaymentsKeepStudentScopeAndUseStableOrdering` | Không lộ thanh toán học viên khác; ID quyết định khi thời gian bằng nhau |
| `pagedNotificationsArePrivateAndUnreadCountsUpdateAfterReading` | Tổng riêng và bulk-read chỉ tác động người hiện tại |
| `pagedLearningIncludesCourseMetadataWithoutOneRequestPerCourse` | Metadata theo nhóm, tối đa sáu truy vấn cho trang có dữ liệu |
| `courseStateFindsThePendingPaymentForTheRequestedCourseOnly` | Không lấy nhầm yêu cầu đang chờ của khóa khác |
| `reviewsPaginateAndOwnReviewRemainsAvailableBeyondTheFirstPage` | Đánh giá cũ của chính mình vẫn đọc được |
| `markAllReadRequiresAuthenticationAndCsrf` | Chặn chưa đăng nhập và thiếu CSRF |
| `pagedReportsKeepWholeDatasetTotalsWithBoundedQueriesAndAdminPermissions` | Tổng toàn bộ, danh sách có giới hạn, báo cáo giảng viên tối đa mười truy vấn |

Dữ liệu mẫu chạy trong transaction, có marker V8 để không tạo trùng hoặc ghi đè khi khởi động lại. Test seeder kiểm tra số liệu, trạng thái thanh toán, hỏi đáp, bài làm và bảo toàn chỉnh sửa. V9 bổ sung chỉ mục phân trang người dùng, thanh toán, đăng ký, đánh giá và thông báo.

Sáu E2E đạt trên bản demo có dữ liệu lớn; helper chọn khóa tìm kiếm theo tiêu đề và admin tìm thanh toán theo học viên thay vì giả định bản ghi ở trang đầu. Log mới: `target/local-demo/role-upgrade-e2e.log`. CI kiểm tra cú pháp `lists.js`.

Playwright CLI kiểm tra thêm các thao tác ở bốn vai trò, bộ lọc/trang qua tải lại, báo cáo có tổng toàn bộ, tài khoản được tìm ngoài 20 kết quả đầu, thông báo bulk-read riêng, đánh giá và ghi chú lưu qua tải lại. Khóa 23 bài hiển thị 8 bài/trang, tự chọn trang chứa bài hiện tại; thứ tự thêm bài liên tiếp là 1 rồi 2. Đã tạo/sửa/khóa/mở/xóa tài khoản mẫu, tạo/đọc/xóa thông báo, tạo/sửa khóa, sửa/xuất bản/ẩn/xóa bài và chuyển trạng thái khóa, từ chối thanh toán. HTTP 503 cho danh sách có nút thử lại; phản hồi tìm kiếm cũ được trả chậm nhưng không thay thế kết quả mới.

Các script `role-audit-*.js` và ảnh `users-admin-mobile.png`, `reviews-student-mobile.png`, `learn-student-mobile.png`, `reports-admin-desktop.png`, `quiz-teacher-scenario.png` nằm trong `output/playwright` (Git bỏ qua). Mobile không tràn ngang; bảng rộng cuộn trong khung. Các API list cũ vẫn còn để tương thích; syllabus/điều hướng bài tải metadata đầy đủ và chỉ phân trang trên giao diện.

## Hoàn thiện UX/UI thao tác — 08/10/2026

74/74 backend trên H2 đạt, sáu hành trình E2E hiện có đạt trên PostgreSQL demo. Đóng gói JAR thành công sau khi dừng tiến trình demo đang giữ file trên Windows. Không có thay đổi schema/nghiệp vụ backend trong đợt này. Cú pháp sáu module JavaScript hợp lệ; CI bổ sung kiểm tra `experience.js`.

| Kiểm tra bổ sung bằng Playwright CLI | Kết quả |
| --- | --- |
| Gửi form đăng nhập rỗng, sửa lỗi và hiện/ẩn mật khẩu | Lỗi sát ô, `aria-invalid`/mô tả và focus đầu tiên đúng; đổi kiểu hiển thị giữ giá trị |
| Preview bài học và tab Home/End | Cùng văn bản/liên kết với phòng học, giữ xuống dòng, HTML là văn bản, tài liệu mở ở thẻ mới |
| Escape khi đang soạn, tiếp tục sửa, bỏ thay đổi | Giữ nội dung đến khi chủ động bỏ; không đóng thầm lặng |
| URL FTP, tiêu đề chỉ có khoảng trắng; gửi từ preview | Chặn gửi, mở lại tab soạn và đưa focus đến ô lỗi |
| HTTP 503 khi lưu rồi thử lại | Giữ nội dung; lưu lại thành công, vẫn có bài sau reload |
| Giữ request lưu chậm | Ô nhập/nút đóng bị khóa, Escape không đóng và chỉ có một request lưu |
| Menu mobile: Tab/Shift+Tab, Escape, nền che và resize | Focus không ra ngoài, đóng trả focus, chuyển desktop gỡ inert/khóa cuộn |
| Nhãn bộ lọc, gỡ từng điều kiện, Back và xóa tất cả | URL/`aria-pressed`/kết quả đồng bộ, trạng thái giữ qua reload |
| Sao chép tài khoản/số tiền/nội dung CK; clipboard bị chặn | Đúng giá trị gốc; fallback chọn đúng văn bản để sao chép thủ công |
| Mobile 390 × 844, ảnh desktop/mobile | Menu, preview và hướng dẫn thanh toán không tràn ngang; nội dung dài cuộn trong dialog |

Log: `target/local-demo/ux-detail-verify.log` (74 test đạt, bước đóng gói đầu gặp file bị giữ), `ux-detail-final-package.log` (đóng gói thành công), `ux-detail-e2e.log` (6/6 đạt). Script QA và ảnh `ux-*.png` ở `output/playwright`, đã xem ảnh để kiểm tra bố cục. Bản nháp hộp soạn chưa tự lưu trên máy chủ; video vẫn mở ở liên kết ngoài. Kiểm tra trình duyệt khác và đánh giá với người dùng thật còn thuộc đợt tiếp theo.

### Nội dung và tài liệu bài học (08/10/2026)
- Backend H2: 77 bài kiểm thử đạt; kiểm tra quyền đọc/tải tài liệu, quyền giảng viên, CSRF và định dạng tệp.
- Renderer: 4 bài kiểm thử Node đạt (Markdown, URL ảnh và video, escape HTML).
- Trình duyệt: tạo/sửa bài, xem trước, tải TXT/PNG, tải xuống, chèn ảnh và viewport 390px đạt.

