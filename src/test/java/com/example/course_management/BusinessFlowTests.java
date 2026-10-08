package com.example.course_management;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.course_management.dto.request.*;
import com.example.course_management.entity.*;
import com.example.course_management.exception.*;
import com.example.course_management.repository.*;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.*;
import java.math.BigDecimal;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BusinessFlowTests {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired UserRepository users;
  @Autowired CourseRepository courses;
  @Autowired LessonRepository lessons;
  @Autowired EnrollmentRepository enrollments;
  @Autowired LessonProgressRepository progress;
  @Autowired PaymentRepository payments;
  @Autowired LessonService lessonService;
  @Autowired CourseService courseService;
  @Autowired EnrollmentService enrollmentService;
  @Autowired PaymentService paymentService;
  @Autowired UserService userService;
  @Autowired PasswordEncoder encoder;
  @Autowired ReviewRepository reviews;
  @Autowired LessonQuestionService questionService;
  @Autowired LessonQuestionRepository questions;
  @Autowired LessonQuizService quizService;
  @Autowired LessonQuizRepository quizzes;
  @Autowired QuizAttemptRepository quizAttempts;
  @Autowired com.example.course_management.config.DemoScenarioSeeder demoScenarios;
  @Autowired com.example.course_management.config.DemoYouTubeSeeder demoYouTube;
  @Autowired PagedListService pagedLists;
  @Autowired jakarta.persistence.EntityManagerFactory entityManagerFactory;
  @MockitoSpyBean NotificationRepository notifications;
  User admin, teacher, student, outsider;
  Course free, paid;
  Lesson first, second;

  @BeforeEach
  void setup() {
    reset(notifications);
    for (String table :
        new String[] {
          "demo_seed_runs",
          "quiz_answers",
          "quiz_attempts",
          "quiz_questions",
          "lesson_quiz_versions",
          "lesson_questions",
          "notifications",
          "reviews",
          "lesson_progress",
          "payments",
          "enrollments",
          "lesson_drafts",
          "lesson_content_versions",
          "lesson_resources",
          "lessons",
          "courses",
          "users"
        }) jdbc.update("DELETE FROM " + table);
    admin = user("admin", Role.ADMIN);
    teacher = user("teacher", Role.TEACHER);
    student = user("student", Role.STUDENT);
    outsider = user("outsider", Role.TEACHER);
    free = course("Free", BigDecimal.ZERO);
    paid = course("Paid", new BigDecimal("100000"));
    first = lesson(free, "First", 1, true);
    second = lesson(free, "Second", 2, true);
    lesson(paid, "Paid lesson", 1, true);
  }

  @Test
  void administrativeProfileAndRoleSaveTogetherAndInvalidateOldSessions() throws Exception {
    var adminSession = login(admin);
    var studentSession = login(student);
    String path = "/api/users/" + student.getUserId() + "/management";
    String body = """
        {"fullName":"Updated learner","email":"updated@example.invalid","role":"TEACHER"}
        """;
    mvc.perform(put(path).session(studentSession).with(csrf()).contentType("application/json").content(body))
        .andExpect(status().isForbidden());
    mvc.perform(put(path).session(adminSession).contentType("application/json").content(body))
        .andExpect(status().isForbidden());
    mvc.perform(put(path).session(adminSession).with(csrf()).contentType("application/json")
        .content("""
          {"fullName":"Must not save","email":"admin@example.invalid","role":"TEACHER"}
          """))
        .andExpect(status().isConflict());
    var unchanged = users.findById(student.getUserId()).orElseThrow();
    assertEquals("student", unchanged.getFullName());
    assertEquals(Role.STUDENT, unchanged.getRole());
    assertEquals(0, unchanged.getAuthVersion());
    mvc.perform(put("/api/users/" + admin.getUserId() + "/management").session(adminSession).with(csrf())
        .contentType("application/json").content(body))
        .andExpect(status().isForbidden());
    assertEquals("admin", users.findById(admin.getUserId()).orElseThrow().getFullName());
    mvc.perform(put(path).session(adminSession).with(csrf()).contentType("application/json").content(body))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data.fullName").value("Updated learner"))
        .andExpect(jsonPath("$.data.role").value("TEACHER"));
    assertEquals(1, users.findById(student.getUserId()).orElseThrow().getAuthVersion());
    mvc.perform(get("/api/auth/me").session(studentSession)).andExpect(status().isUnauthorized());
  }

  @Test
  void notificationLinksAcceptTabsAndEncodedSearchButRejectExternalTargets() throws Exception {
    var session = login(admin);
    String target = "/course-detail.html?id=" + free.getCourseId() + "&lessonId=" + first.getLessonId() + "#questions";
    mvc.perform(post("/api/notifications").session(session).with(csrf()).contentType("application/json")
        .content("{\"userId\":" + student.getUserId() + ",\"message\":\"Open discussion\",\"targetUrl\":\"" + target + "\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data.targetUrl").value(target));
    var policy = new ContentPolicy(enrollments);
    assertDoesNotThrow(() -> policy.targetUrl("/courses.html?search=Java%20c%C6%A1%20b%E1%BA%A3n"));
    for (String invalid : new String[]{"//evil.example/courses.html", "https://evil.example/courses.html", "/courses.html#<script>", "/courses.html\n", "/../courses.html"})
      assertThrows(BadRequestException.class, () -> policy.targetUrl(invalid));
    mvc.perform(post("/api/notifications").session(session).with(csrf()).contentType("application/json")
        .content("{\"userId\":" + student.getUserId() + ",\"message\":\"Test\",\"type\":\"" + "x".repeat(51) + "\"}"))
        .andExpect(status().isBadRequest());
    assertEquals(1, notifications.count());
  }

  @Test
  void registrationAndAdminCreationShareAccountFieldLimits() throws Exception {
    String longEmail = "a".repeat(60) + "@" + "b".repeat(50) + ".invalid";
    mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json")
        .content("{\"username\":\"new_user\",\"fullName\":\"User\",\"password\":\"Password123!\",\"email\":\"" + longEmail + "\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(post("/api/users").session(login(admin)).with(csrf()).contentType("application/json")
        .content("""
          {"username":"invalid username","fullName":"User","password":"Password123!","email":"user@example.invalid","role":"STUDENT"}
          """))
        .andExpect(status().isBadRequest());
    assertEquals(4, users.count());
  }

  @Test
  void equalLessonPositionsKeepAStableProgramOrder() {
    var third = lesson(free, "Same position", 1, true);
    var draft = lesson(free, "Draft in same position", 1, false);
    assertEquals(java.util.List.of(first.getLessonId(), third.getLessonId(), second.getLessonId()),
        lessons.findByCourse_CourseIdAndIsPublishedTrueOrderByOrderIndex(free.getCourseId()).stream().map(Lesson::getLessonId).toList());
    assertEquals(java.util.List.of(first.getLessonId(), third.getLessonId(), draft.getLessonId(), second.getLessonId()),
        lessons.findByCourse_CourseIdOrderByOrderIndex(free.getCourseId()).stream().map(Lesson::getLessonId).toList());
  }

  User user(String name, Role role) {
    var u = new User();
    u.setUsername(name);
    u.setFullName(name);
    u.setEmail(name + "@example.invalid");
    u.setRole(role);
    u.setPasswordHash(encoder.encode("Password123!"));
    return users.saveAndFlush(u);
  }

  Course course(String title, BigDecimal price) {
    var c = new Course();
    c.setCategory("Lập trình");
    c.setLevel("Cơ bản");
    c.setTitle(title);
    c.setTeacher(teacher);
    c.setPrice(price);
    c.setStatus(CourseStatus.PUBLISHED);
    return courses.saveAndFlush(c);
  }

  Lesson lesson(Course c, String title, int order, boolean published) {
    var l = new Lesson();
    l.setCourse(c);
    l.setTitle(title);
    l.setOrderIndex(order);
    l.setIsPublished(published);
    l.setContentUrl("https://example.com/lesson");
    l.setTextContent("Private lesson body");
    return lessons.saveAndFlush(l);
  }

  CustomUserDetails actor(User u) {
    return new CustomUserDetails(u);
  }

  MockHttpSession login(User u) throws Exception {
    return (MockHttpSession)
        mvc.perform(
                post("/api/auth/login")
                    .with(csrf())
                    .contentType("application/json")
                    .content(
                        "{\"username\":\"" + u.getUsername() + "\",\"password\":\"Password123!\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getRequest()
            .getSession(false);
  }

  void enrollForQuestions(User u) {
    var e = new Enrollment();
    e.setStudent(u);
    e.setCourse(free);
    enrollments.saveAndFlush(e);
  }

  @Test
  void demoScenariosAreConsistentAndDoNotOverwriteEditsOrCreateDuplicates() {
    demoScenarios.seed();
    assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM demo_seed_runs", Integer.class));
    var demo = users.findByUsername("student_demo").orElseThrow();
    assertEquals(12, enrollments.findByStudent_UserId(demo.getUserId()).size());
    assertEquals(26, questions.count());
    assertEquals(26, quizAttempts.count());
    assertTrue(payments.search(PaymentStatus.PENDING).size() > 10);
    var edited =
        courses.findAll().stream()
            .filter(c -> c.getTitle().startsWith("Python thực hành"))
            .findFirst()
            .orElseThrow();
    edited.setTitle("Tên đã được chỉnh sửa");
    courses.saveAndFlush(edited);
    long userCount = users.count(), courseCount = courses.count(), paymentCount = payments.count();
    demoScenarios.seed();
    assertEquals(userCount, users.count());
    assertEquals(courseCount, courses.count());
    assertEquals(paymentCount, payments.count());
    assertEquals(
        "Tên đã được chỉnh sửa", courses.findById(edited.getCourseId()).orElseThrow().getTitle());
  }

  @Test
  void youtubeDemoCoursesPreserveExistingDataAndUseNormalLearningAccess() {
    demoYouTube.seed(); // Missing demo teacher must not record a completed seed.
    assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM demo_seed_runs", Integer.class));
    user("teacher_demo", Role.TEACHER);
    long existingCourses = courses.count(), existingLessons = lessons.count();
    demoYouTube.seed();
    assertEquals(existingCourses + 2, courses.count());
    assertEquals(existingLessons + 10, lessons.count());
    assertEquals(2, quizzes.count());
    assertEquals(0, payments.count());
    var samples = courses.findAll().stream().filter(c -> c.getTitle().endsWith("· Demo YouTube")).toList();
    for (var sample : samples) {
      assertEquals(CourseStatus.PUBLISHED, sample.getStatus());
      assertEquals(0, sample.getPrice().signum());
      var program = lessons.findByCourse_CourseIdAndIsPublishedTrueOrderByOrderIndex(sample.getCourseId());
      assertEquals(5, program.size());
      for (int i = 0; i < program.size(); i++) {
        var lesson = program.get(i);
        assertEquals(i + 1, lesson.getOrderIndex());
        assertEquals("MARKDOWN", lesson.getContentFormat());
        assertTrue(lesson.getVideoUrl().matches("https://www\\.youtube\\.com/watch\\?v=[a-zA-Z0-9_-]{11}&t=\\d+s"));
        assertTrue(lesson.getTextContent().contains("freeCodeCamp.org"));
      }
      var firstVideo = program.getFirst();
      assertNull(lessonService.getLessonsByCourse(sample.getCourseId(), actor(student)).getFirst().getVideoUrl());
      assertThrows(ForbiddenException.class, () -> lessonService.getLessonById(firstVideo.getLessonId(), actor(student)));
      var enrolled = enrollmentService.enroll(sample.getCourseId(), actor(student));
      assertEquals(firstVideo.getVideoUrl(), lessonService.getLessonById(firstVideo.getLessonId(), actor(student)).getVideoUrl());
      var completed = enrollmentService.completeLesson(enrolled.getEnrollmentId(), firstVideo.getLessonId(), actor(student));
      assertEquals(0, new BigDecimal("20").compareTo(completed.getProgressPercentage()));
      assertEquals(2, quizService.get(program.getLast().getLessonId(), actor(student)).questions().size());
    }
    var edited = lessons.findByCourse_CourseIdOrderByOrderIndex(samples.getFirst().getCourseId()).getFirst();
    edited.setVideoUrl("https://youtu.be/abcdefghijk");
    lessons.saveAndFlush(edited);
    var deleted = lessons.findByCourse_CourseIdOrderByOrderIndex(samples.getFirst().getCourseId()).get(1);
    lessons.delete(deleted);
    lessons.flush();
    demoYouTube.seed();
    assertEquals(existingCourses + 2, courses.count());
    assertEquals(existingLessons + 9, lessons.count());
    assertEquals("https://youtu.be/abcdefghijk", lessons.findById(edited.getLessonId()).orElseThrow().getVideoUrl());
    assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM demo_seed_runs", Integer.class));
    assertEquals("Free", courses.findById(free.getCourseId()).orElseThrow().getTitle());
  }

  @Test
  void pagedUsersFilterBeforeCountingAndTreatWildcardsLiterally() throws Exception {
    for (int i = 0; i < 13; i++) user("sample_" + i, Role.STUDENT);
    var one = pagedLists.users("sample_", Role.STUDENT, "active", 0, 5, actor(admin));
    var two = pagedLists.users("sample_", Role.STUDENT, "active", 1, 5, actor(admin));
    assertEquals(13, one.totalElements());
    assertEquals(3, one.totalPages());
    assertEquals(5, one.content().size());
    assertTrue(
        one.content().stream()
            .noneMatch(
                u -> two.content().stream().anyMatch(v -> v.getUserId().equals(u.getUserId()))));
    assertEquals(0, pagedLists.users("%", null, "", 0, 10, actor(admin)).totalElements());
    assertThrows(
        ForbiddenException.class, () -> pagedLists.users("", null, "", 0, 10, actor(student)));
    mvc.perform(get("/api/lists/users").session(login(teacher))).andExpect(status().isForbidden());
    mvc.perform(get("/api/lists/users?size=101").session(login(admin)))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/lists/users?status=unknown").session(login(admin)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void pagedPaymentsKeepStudentScopeAndUseStableOrdering() throws Exception {
    var other = user("another", Role.STUDENT);
    for (int i = 0; i < 7; i++) {
      var p = new Payment();
      p.setStudent(i % 2 == 0 ? student : other);
      p.setCourse(paid);
      p.setAmount(paid.getPrice());
      p.setTransferNote("FILTER_" + i);
      p.setCreatedAt(java.time.LocalDateTime.of(2026, 1, 1, 10, 0));
      payments.saveAndFlush(p);
    }
    var mine = pagedLists.payments("", PaymentStatus.PENDING, "old", 0, 2, actor(student));
    assertEquals(4, mine.totalElements());
    assertEquals(2, mine.content().size());
    assertTrue(mine.content().stream().allMatch(p -> p.getStudentId().equals(student.getUserId())));
    assertTrue(mine.content().getFirst().getPaymentId() < mine.content().get(1).getPaymentId());
    assertEquals(7, pagedLists.summary(actor(admin)).get("PENDING"));
    assertEquals(4, pagedLists.summary(actor(student)).get("PENDING"));
    assertFalse(pagedLists.summary(actor(teacher)).containsKey("users"));
    mvc.perform(get("/api/lists/payments").session(login(teacher)))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/lists/payments?sort=invalid").session(login(student)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void pagedNotificationsArePrivateAndUnreadCountsUpdateAfterReading() {
    for (int i = 0; i < 14; i++) {
      var n = new Notification();
      n.setUser(i == 13 ? teacher : student);
      n.setMessage("Notice " + i);
      n.setIsRead(i % 3 == 0);
      notifications.saveAndFlush(n);
    }
    var unread = pagedLists.notifications("unread", 0, 3, actor(student));
    assertEquals(8, unread.totalElements());
    assertEquals(3, unread.content().size());
    assertEquals(8, pagedLists.summary(actor(student)).get("unread"));
    var id = unread.content().getFirst().getNotificationId();
    var n = notifications.findById(id).orElseThrow();
    n.setIsRead(true);
    notifications.saveAndFlush(n);
    assertEquals(7, pagedLists.notifications("unread", 0, 3, actor(student)).totalElements());
    assertEquals(1, pagedLists.notifications("", 0, 10, actor(teacher)).totalElements());
    assertEquals(7, pagedLists.markAllRead(actor(student)));
    assertEquals(0, pagedLists.summary(actor(student)).get("unread"));
    assertEquals(1, pagedLists.summary(actor(teacher)).get("unread"));
    assertEquals(0, pagedLists.markAllRead(actor(student)));
    assertThrows(
        BadRequestException.class, () -> pagedLists.notifications("bad", 0, 10, actor(student)));
  }

  @Test
  void pagedLearningIncludesCourseMetadataWithoutOneRequestPerCourse() {
    for (int i = 0; i < 12; i++) {
      var c = course("Learning " + i, BigDecimal.ZERO);
      lesson(c, "A lesson", 1, true);
      var e = new Enrollment();
      e.setStudent(student);
      e.setCourse(c);
      enrollments.saveAndFlush(e);
    }
    var other = user("other", Role.STUDENT);
    enrollForQuestions(other);
    var stats = entityManagerFactory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
    stats.clear();
    var result =
        pagedLists.learning("Learning", EnrollmentStatus.ENROLLED, "new", 0, 9, actor(student));
    assertEquals(12, result.totalElements());
    assertEquals(9, result.content().size());
    assertTrue(result.content().stream().allMatch(e -> e.course().getLessonCount() == 1));
    assertTrue(
        stats.getPrepareStatementCount() <= 6, "A bounded page must use batched course statistics");
    assertEquals(1, pagedLists.learning("", null, "new", 0, 9, actor(other)).totalElements());
    assertThrows(
        ForbiddenException.class, () -> pagedLists.learning("", null, "new", 0, 9, actor(teacher)));
    assertThrows(
        BadRequestException.class,
        () -> pagedLists.learning("", null, "bad", 0, 9, actor(student)));
  }

  @Test
  void courseStateFindsThePendingPaymentForTheRequestedCourseOnly() {
    var one = paymentService.createPayment(paid.getCourseId(), actor(student));
    var anotherCourse = course("Another paid course", BigDecimal.TEN);
    paymentService.createPayment(anotherCourse.getCourseId(), actor(student));
    var state = pagedLists.courseState(paid.getCourseId(), actor(student));
    assertEquals(
        one.getPaymentId(),
        ((com.example.course_management.dto.response.PaymentResponse) state.get("payment"))
            .getPaymentId());
    assertNull(state.get("enrollment"));
    assertNull(
        pagedLists
            .courseState(paid.getCourseId(), actor(user("other", Role.STUDENT)))
            .get("payment"));
    assertThrows(
        ForbiddenException.class, () -> pagedLists.courseState(paid.getCourseId(), actor(teacher)));
  }

  @Test
  void reviewsPaginateAndOwnReviewRemainsAvailableBeyondTheFirstPage() {
    enrollForQuestions(student);
    var own = new Review();
    own.setCourse(free);
    own.setStudent(student);
    own.setRating(5);
    reviews.saveAndFlush(own);
    for (int i = 0; i < 8; i++) {
      var r = new Review();
      r.setCourse(free);
      r.setStudent(user("review_" + i, Role.STUDENT));
      r.setRating(4);
      reviews.saveAndFlush(r);
    }
    var result = pagedLists.reviews(free.getCourseId(), 0, 6, actor(student));
    assertEquals(9, result.totalElements());
    assertEquals(6, result.content().size());
    assertTrue(result.content().stream().noneMatch(r -> r.getReviewId().equals(own.getReviewId())));
    assertEquals(
        own.getReviewId(), pagedLists.ownReview(free.getCourseId(), actor(student)).getReviewId());
    free.setStatus(CourseStatus.DRAFT);
    courses.saveAndFlush(free);
    assertThrows(
        ResourceNotFoundException.class,
        () -> pagedLists.reviews(free.getCourseId(), 0, 6, actor(outsider)));
    assertEquals(9, pagedLists.reviews(free.getCourseId(), 0, 6, actor(teacher)).totalElements());
  }

  @Test
  void markAllReadRequiresAuthenticationAndCsrf() throws Exception {
    mvc.perform(put("/api/lists/notifications/read-all")).andExpect(status().isForbidden());
    mvc.perform(put("/api/lists/notifications/read-all").session(login(student)))
        .andExpect(status().isForbidden());
    mvc.perform(put("/api/lists/notifications/read-all").session(login(student)).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").value(0));
  }

  @Test
  void pagedReportsKeepWholeDatasetTotalsWithBoundedQueriesAndAdminPermissions() throws Exception {
    for (int i = 0; i < 12; i++) {
      var c = course("Report " + i, BigDecimal.ZERO);
      lesson(c, "Lesson", 1, true);
      var e = new Enrollment();
      e.setCourse(c);
      e.setStudent(student);
      enrollments.saveAndFlush(e);
    }
    var stats = entityManagerFactory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
    stats.clear();
    var teacherReport = pagedLists.teacherReport(teacher.getUserId(), 0, 5, actor(admin));
    var coursePage =
        (com.example.course_management.dto.response.PageResponse<?>) teacherReport.get("courses");
    assertEquals(14, coursePage.totalElements());
    assertEquals(5, coursePage.content().size());
    assertTrue(
        stats.getPrepareStatementCount() <= 10,
        "Teacher report must aggregate per page rather than per course");
    var studentReport = pagedLists.studentReport(student.getUserId(), 1, 5, actor(admin));
    var learningPage =
        (com.example.course_management.dto.response.PageResponse<?>) studentReport.get("courses");
    assertEquals(12, learningPage.totalElements());
    assertEquals(5, learningPage.content().size());
    assertEquals(12L, ((java.util.Map<?, ?>) studentReport.get("summary")).get("totalEnrollments"));
    assertThrows(
        ForbiddenException.class,
        () -> pagedLists.teacherReport(teacher.getUserId(), 0, 5, actor(student)));
    assertThrows(
        BadRequestException.class,
        () -> pagedLists.studentReport(teacher.getUserId(), 0, 5, actor(admin)));
    mvc.perform(get("/api/lists/student-report/" + student.getUserId()).session(login(teacher)))
        .andExpect(status().isForbidden());
  }

  SaveQuizRequest quizRequest(int revision, boolean published) {
    return new SaveQuizRequest(
        revision,
        "Practice quiz",
        70,
        published,
        java.util.List.of(
            new SaveQuizRequest.Question(
                "First question", java.util.List.of("A", "B", "C", "D"), 0, "A is correct"),
            new SaveQuizRequest.Question(
                "Second question",
                java.util.List.of("One", "Two", "Three", "Four"),
                1,
                "Two is correct")));
  }

  SubmitQuizRequest submission(
      com.example.course_management.dto.response.QuizResponse quiz, Integer... answers) {
    return new SubmitQuizRequest(
        quiz.quizVersionId(), java.util.UUID.randomUUID().toString(), java.util.List.of(answers));
  }

  @Test
  void quizDraftsAndAnswerKeysAreNotVisibleToLearnersBeforeSubmission() throws Exception {
    enrollForQuestions(student);
    quizService.save(first.getLessonId(), quizRequest(0, false), actor(teacher));
    assertNull(quizService.get(first.getLessonId(), actor(student)));
    assertNotNull(quizService.get(first.getLessonId(), actor(teacher)));
    var quiz = quizService.save(first.getLessonId(), quizRequest(1, true), actor(teacher));
    var learner = quizService.get(first.getLessonId(), actor(student));
    assertNull(learner.questions().getFirst().correctIndex());
    assertNull(learner.questions().getFirst().explanation());
    assertEquals(0, quiz.questions().getFirst().correctIndex());
    mvc.perform(get("/api/lessons/" + first.getLessonId() + "/quiz").session(login(student)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.questions[0].correctIndex").doesNotExist())
        .andExpect(jsonPath("$.data.questions[0].explanation").doesNotExist());
    mvc.perform(get("/api/lessons/" + first.getLessonId() + "/quiz"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void quizSubmissionGradesOnServerAndKeepsProgressSeparate() {
    enrollForQuestions(student);
    var quiz = quizService.save(first.getLessonId(), quizRequest(0, true), actor(teacher));
    var wrong = quizService.submit(first.getLessonId(), submission(quiz, 2, 1), actor(student));
    assertEquals(50, wrong.summary().score());
    assertEquals(1, wrong.summary().correctCount());
    assertFalse(wrong.summary().passed());
    assertFalse(wrong.feedback().getFirst().correct());
    assertEquals("A is correct", wrong.feedback().getFirst().explanation());
    var right = quizService.submit(first.getLessonId(), submission(quiz, 0, 1), actor(student));
    assertEquals(100, right.summary().score());
    assertTrue(right.summary().passed());
    assertEquals(0, progress.count());
    assertEquals(
        BigDecimal.ZERO.setScale(2), enrollments.findAll().getFirst().getProgressPercentage());
  }

  @Test
  void quizPassingUsesTheExactFractionAndDisplaysAnUnroundedScore() {
    enrollForQuestions(student);
    var questions =
        java.util.List.of(
            quizRequest(0, true).questions().getFirst(),
            quizRequest(0, true).questions().getFirst(),
            quizRequest(0, true).questions().getFirst());
    var quiz =
        quizService.save(
            first.getLessonId(),
            new SaveQuizRequest(0, "Boundary", 67, true, questions),
            actor(teacher));
    var result = quizService.submit(first.getLessonId(), submission(quiz, 0, 0, 1), actor(student));
    assertEquals(66, result.summary().score());
    assertFalse(result.summary().passed());
    var next =
        quizService.save(
            first.getLessonId(),
            new SaveQuizRequest(1, "Boundary", 66, true, questions),
            actor(teacher));
    assertTrue(
        quizService
            .submit(first.getLessonId(), submission(next, 0, 0, 1), actor(student))
            .summary()
            .passed());
  }

  @Test
  void quizVersionsPreserveOldResultsAndRejectStaleEditorsAndSubmissions() {
    enrollForQuestions(student);
    var oldQuiz = quizService.save(first.getLessonId(), quizRequest(0, true), actor(teacher));
    var oldSubmission = submission(oldQuiz, 0, 1);
    var oldResult = quizService.submit(first.getLessonId(), oldSubmission, actor(student));
    var edited =
        new SaveQuizRequest(
            1,
            "New title",
            100,
            true,
            java.util.List.of(
                new SaveQuizRequest.Question(
                    "Changed prompt",
                    java.util.List.of("X", "Y", "Z", "W"),
                    2,
                    "New explanation")));
    var newQuiz = quizService.save(first.getLessonId(), edited, actor(teacher));
    assertEquals(2, newQuiz.revision());
    var history = quizService.attempt(oldResult.summary().attemptId(), actor(student));
    assertEquals("Practice quiz", history.summary().title());
    assertEquals("First question", history.feedback().getFirst().prompt());
    assertEquals("A is correct", history.feedback().getFirst().explanation());
    assertThrows(
        ConflictException.class,
        () -> quizService.save(first.getLessonId(), quizRequest(1, true), actor(teacher)));
    assertThrows(
        ConflictException.class,
        () -> quizService.submit(first.getLessonId(), submission(oldQuiz, 0, 1), actor(student)));
    assertEquals(
        oldResult.summary().attemptId(),
        quizService
            .submit(first.getLessonId(), oldSubmission, actor(student))
            .summary()
            .attemptId());
    assertEquals(1, quizAttempts.count());
    quizService.save(first.getLessonId(), quizRequest(2, false), actor(admin));
    assertNull(quizService.get(first.getLessonId(), actor(student)));
    assertEquals(1, quizService.history(first.getLessonId(), 0, 5, actor(student)).totalElements());
  }

  @Test
  void quizHistoryIsPrivateAndRequiresAnActiveEnrollmentAndVisibleLesson() {
    enrollForQuestions(student);
    var quiz = quizService.save(first.getLessonId(), quizRequest(0, true), actor(teacher));
    var result = quizService.submit(first.getLessonId(), submission(quiz, 0, 1), actor(student));
    var another = user("other_student", Role.STUDENT);
    enrollForQuestions(another);
    assertThrows(
        ForbiddenException.class,
        () -> quizService.attempt(result.summary().attemptId(), actor(another)));
    assertEquals(0, quizService.history(first.getLessonId(), 0, 5, actor(another)).totalElements());
    assertThrows(
        ForbiddenException.class, () -> quizService.get(first.getLessonId(), actor(outsider)));
    assertThrows(
        ForbiddenException.class,
        () -> quizService.save(first.getLessonId(), quizRequest(1, true), actor(outsider)));
    assertThrows(
        ForbiddenException.class,
        () -> quizService.statistics(first.getLessonId(), actor(outsider)));
    assertNotNull(quizService.attempt(result.summary().attemptId(), actor(teacher)));
    var e =
        enrollments
            .findByStudent_UserIdAndCourse_CourseId(student.getUserId(), free.getCourseId())
            .orElseThrow();
    e.setStatus(EnrollmentStatus.DROPPED);
    enrollments.saveAndFlush(e);
    assertThrows(
        ForbiddenException.class,
        () -> quizService.history(first.getLessonId(), 0, 5, actor(student)));
    assertThrows(
        ForbiddenException.class,
        () -> quizService.submit(first.getLessonId(), submission(quiz, 0, 1), actor(student)));
    first.setIsPublished(false);
    lessons.saveAndFlush(first);
    assertThrows(
        ResourceNotFoundException.class,
        () -> quizService.get(first.getLessonId(), actor(another)));
    assertThrows(
        ResourceNotFoundException.class,
        () -> quizService.attempt(result.summary().attemptId(), actor(student)));
  }

  @Test
  void quizStatisticsCountAttemptsAndWrongAnswersForCurrentVersionOnly() {
    enrollForQuestions(student);
    var quiz = quizService.save(first.getLessonId(), quizRequest(0, true), actor(teacher));
    var empty = quizService.statistics(first.getLessonId(), actor(teacher));
    assertEquals(0, empty.attempts());
    assertNull(empty.averageScore());
    quizService.submit(first.getLessonId(), submission(quiz, 0, 1), actor(student));
    quizService.submit(first.getLessonId(), submission(quiz, 3, 1), actor(student));
    var stats = quizService.statistics(first.getLessonId(), actor(teacher));
    assertEquals(2, stats.attempts());
    assertEquals(1, stats.students());
    assertEquals(1, stats.passedAttempts());
    assertEquals(75.0, stats.averageScore());
    assertEquals(1, stats.questions().getFirst().incorrect());
    assertEquals(2, stats.questions().getFirst().answered());
    assertEquals(0, stats.questions().get(1).incorrect());
    quizService.save(first.getLessonId(), quizRequest(1, true), actor(teacher));
    assertEquals(0, quizService.statistics(first.getLessonId(), actor(admin)).attempts());
    assertThrows(
        ForbiddenException.class,
        () -> quizService.statistics(first.getLessonId(), actor(student)));
  }

  @Test
  void repeatedAndConcurrentQuizSubmissionsStoreExactlyOneAttempt() throws Exception {
    enrollForQuestions(student);
    var quiz = quizService.save(first.getLessonId(), quizRequest(0, true), actor(teacher));
    var request = submission(quiz, 0, 1);
    var ready = new CountDownLatch(2);
    var start = new CountDownLatch(1);
    var executor = Executors.newFixedThreadPool(2);
    try {
      Callable<Integer> work =
          () -> {
            ready.countDown();
            assertTrue(start.await(10, TimeUnit.SECONDS));
            return quizService
                .submit(first.getLessonId(), request, actor(student))
                .summary()
                .attemptId();
          };
      var one = executor.submit(work);
      var two = executor.submit(work);
      assertTrue(ready.await(10, TimeUnit.SECONDS));
      start.countDown();
      assertEquals(one.get(15, TimeUnit.SECONDS), two.get(15, TimeUnit.SECONDS));
    } finally {
      start.countDown();
      executor.shutdownNow();
    }
    assertEquals(1, quizAttempts.count());
    assertThrows(
        ConflictException.class,
        () ->
            quizService.submit(
                first.getLessonId(),
                new SubmitQuizRequest(
                    quiz.quizVersionId(), request.submissionKey(), java.util.List.of(3, 1)),
                actor(student)));
  }

  @Test
  void quizHistoryPaginatesAndInvalidAnswersDoNotCreateAttempts() {
    enrollForQuestions(student);
    var quiz = quizService.save(first.getLessonId(), quizRequest(0, true), actor(teacher));
    var one = quizService.submit(first.getLessonId(), submission(quiz, 0, 1), actor(student));
    var two = quizService.submit(first.getLessonId(), submission(quiz, 1, 0), actor(student));
    var page = quizService.history(first.getLessonId(), 0, 1, actor(student));
    assertEquals(2, page.totalPages());
    assertEquals(two.summary().attemptId(), page.content().getFirst().attemptId());
    assertEquals(
        one.summary().attemptId(),
        quizService
            .history(first.getLessonId(), 1, 1, actor(student))
            .content()
            .getFirst()
            .attemptId());
    assertThrows(
        BadRequestException.class,
        () -> quizService.submit(first.getLessonId(), submission(quiz, 0), actor(student)));
    assertThrows(
        BadRequestException.class,
        () -> quizService.submit(first.getLessonId(), submission(quiz, 4, 1), actor(student)));
    assertThrows(
        BadRequestException.class,
        () -> quizService.history(first.getLessonId(), -1, 5, actor(student)));
    assertThrows(
        BadRequestException.class,
        () -> quizService.history(first.getLessonId(), 0, 51, actor(student)));
    assertEquals(2, quizAttempts.count());
  }

  @Test
  void quizEndpointsValidateNestedQuestionsRolesAndCsrf() throws Exception {
    var teacherSession = login(teacher);
    var path = "/api/lessons/" + first.getLessonId() + "/quiz";
    var body =
        """
{"expectedRevision":0,"title":"Quiz","passPercentage":70,"published":true,
"questions":[{"prompt":"Question","options":["A","B","C","D"],"correctIndex":0,"explanation":"Explanation"}]}
""";
    mvc.perform(put(path).session(teacherSession).contentType("application/json").content(body))
        .andExpect(status().isForbidden());
    mvc.perform(
            put(path)
                .session(login(student))
                .with(csrf())
                .contentType("application/json")
                .content(body))
        .andExpect(status().isForbidden());
    for (var invalid :
        new String[] {
          body.replace("\"correctIndex\":0", "\"correctIndex\":4"),
          body.replace("\"A\",\"B\",\"C\",\"D\"", "\"A\",\"B\""),
          body.replace("Question", "  "),
          body.replace("Explanation", "  "),
          body.replace("\"B\"", "\"A\""),
          body.replace("70", "101")
        }) {
      mvc.perform(
              put(path)
                  .session(teacherSession)
                  .with(csrf())
                  .contentType("application/json")
                  .content(invalid))
          .andExpect(status().isBadRequest());
    }
    mvc.perform(
            put(path)
                .session(teacherSession)
                .with(csrf())
                .contentType("application/json")
                .content(body))
        .andExpect(status().isOk());
    assertEquals(1, quizzes.count());
    enrollForQuestions(student);
    var quiz = quizService.get(first.getLessonId(), actor(student));
    var submit =
        "{\"quizVersionId\":"
            + quiz.quizVersionId()
            + ",\"submissionKey\":\""
            + java.util.UUID.randomUUID()
            + "\",\"answers\":[0]}";
    var studentSession = login(student);
    mvc.perform(
            post(path + "/attempts")
                .session(studentSession)
                .contentType("application/json")
                .content(submit))
        .andExpect(status().isForbidden());
    mvc.perform(
            post(path + "/attempts")
                .session(studentSession)
                .with(csrf())
                .contentType("application/json")
                .content(submit.replace("[0]", "[null]")))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post(path + "/attempts")
                .session(studentSession)
                .with(csrf())
                .contentType("application/json")
                .content(submit.replace("[0]", "[4]")))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post(path + "/attempts")
                .session(studentSession)
                .with(csrf())
                .contentType("application/json")
                .content(submit))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.summary.score").value(100));
  }

  @Test
  void deletingLessonRemovesItsQuizzesAndAttemptsWithoutOrphans() {
    enrollForQuestions(student);
    var quiz = quizService.save(first.getLessonId(), quizRequest(0, true), actor(teacher));
    quizService.submit(first.getLessonId(), submission(quiz, 0, 1), actor(student));
    lessonService.deleteLesson(first.getLessonId(), actor(teacher));
    assertEquals(0, quizzes.count());
    assertEquals(0, quizAttempts.count());
    assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM quiz_answers", Integer.class));
    assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM quiz_questions", Integer.class));
  }

  @Test
  void questionsRequireEnrollmentAndStayInsidePublishedLessons() throws Exception {
    var path = "/api/lessons/" + first.getLessonId() + "/questions";
    mvc.perform(get(path)).andExpect(status().isUnauthorized());
    var session = login(student);
    mvc.perform(get(path).session(session)).andExpect(status().isForbidden());
    mvc.perform(
            post(path)
                .session(session)
                .with(csrf())
                .contentType("application/json")
                .content("{\"body\":\"Help\"}"))
        .andExpect(status().isForbidden());
    enrollForQuestions(student);
    mvc.perform(
            post(path)
                .session(session)
                .with(csrf())
                .contentType("application/json")
                .content("{\"body\":\"  How does this work?  \"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.body").value("How does this work?"));
    var draft = lesson(free, "Draft", 3, false);
    assertThrows(
        ResourceNotFoundException.class,
        () -> questionService.list(draft.getLessonId(), 0, 10, actor(student)));
    assertThrows(
        ResourceNotFoundException.class,
        () -> questionService.ask(draft.getLessonId(), "Help", actor(student)));
    assertThrows(
        ForbiddenException.class,
        () -> questionService.list(first.getLessonId(), 0, 10, actor(outsider)));
    var e =
        enrollments
            .findByStudent_UserIdAndCourse_CourseId(student.getUserId(), free.getCourseId())
            .orElseThrow();
    e.setStatus(EnrollmentStatus.DROPPED);
    enrollments.saveAndFlush(e);
    assertThrows(
        ForbiddenException.class,
        () -> questionService.list(first.getLessonId(), 0, 10, actor(student)));
  }

  @Test
  void questionAnswersNotifyTheAuthorAndLinkToTheExactContext() {
    enrollForQuestions(student);
    var q =
        questionService.ask(first.getLessonId(), "<img src=x onerror=alert(1)>", actor(student));
    assertEquals(0, progress.count());
    var teacherNotices = notifications.findByUser_UserIdOrderByCreatedAtDesc(teacher.getUserId());
    assertEquals(1, teacherNotices.size());
    assertEquals(
        "/course-detail.html?id="
            + free.getCourseId()
            + "&lessonId="
            + first.getLessonId()
            + "&questionId="
            + q.questionId(),
        teacherNotices.getFirst().getTargetUrl());
    assertThrows(
        ForbiddenException.class,
        () -> questionService.answer(q.questionId(), "No", actor(outsider)));
    assertThrows(
        ForbiddenException.class,
        () -> questionService.answer(q.questionId(), "No", actor(student)));
    var answer = questionService.answer(q.questionId(), "Read the example", actor(teacher));
    assertEquals("Read the example", answer.answer());
    assertEquals(teacher.getFullName(), answer.answeredByName());
    assertNotNull(answer.answeredAt());
    assertEquals(
        1, notifications.findByUser_UserIdOrderByCreatedAtDesc(student.getUserId()).size());
    questionService.answer(q.questionId(), "Updated explanation", actor(admin));
    assertEquals(
        "Updated explanation", questionService.get(q.questionId(), actor(student)).answer());
    assertEquals(1, questions.count());
  }

  @Test
  void hiddenQuestionsAreExcludedBeforeCountingAndCanBeRestored() throws Exception {
    enrollForQuestions(student);
    var q = questionService.ask(first.getLessonId(), "Help", actor(student));
    assertThrows(
        ForbiddenException.class,
        () -> questionService.visibility(q.questionId(), true, actor(outsider)));
    questionService.visibility(q.questionId(), true, actor(teacher));
    assertEquals(
        0, questionService.list(first.getLessonId(), 0, 10, actor(student)).totalElements());
    var managerPage = questionService.list(first.getLessonId(), 0, 10, actor(teacher));
    assertEquals(1, managerPage.totalElements());
    assertTrue(managerPage.content().getFirst().hidden());
    assertThrows(
        ResourceNotFoundException.class, () -> questionService.get(q.questionId(), actor(student)));
    mvc.perform(get("/api/questions/" + q.questionId()).session(login(student)))
        .andExpect(status().isNotFound());
    assertThrows(
        BadRequestException.class,
        () -> questionService.answer(q.questionId(), "Hidden", actor(teacher)));
    questionService.visibility(q.questionId(), false, actor(admin));
    assertEquals(
        1, questionService.list(first.getLessonId(), 0, 10, actor(student)).totalElements());
  }

  @Test
  void questionsPaginateByNewestIdWithoutLeakingOtherLessons() {
    enrollForQuestions(student);
    var one = questionService.ask(first.getLessonId(), "One", actor(student));
    var two = questionService.ask(first.getLessonId(), "Two", actor(student));
    questionService.ask(second.getLessonId(), "Other lesson", actor(student));
    var page = questionService.list(first.getLessonId(), 0, 1, actor(student));
    assertEquals(2, page.totalElements());
    assertEquals(2, page.totalPages());
    assertEquals(two.questionId(), page.content().getFirst().questionId());
    assertEquals(
        one.questionId(),
        questionService
            .list(first.getLessonId(), 1, 1, actor(student))
            .content()
            .getFirst()
            .questionId());
    assertTrue(questionService.list(first.getLessonId(), 2, 1, actor(student)).content().isEmpty());
    assertThrows(
        BadRequestException.class,
        () -> questionService.list(first.getLessonId(), -1, 10, actor(student)));
    assertThrows(
        BadRequestException.class,
        () -> questionService.list(first.getLessonId(), 0, 0, actor(student)));
    assertThrows(
        BadRequestException.class,
        () -> questionService.list(first.getLessonId(), 0, 51, actor(student)));
  }

  @Test
  void questionWritesValidateTextRolesAndCsrf() throws Exception {
    enrollForQuestions(student);
    var session = login(student);
    var path = "/api/lessons/" + first.getLessonId() + "/questions";
    mvc.perform(
            post(path)
                .session(session)
                .contentType("application/json")
                .content("{\"body\":\"Help\"}"))
        .andExpect(status().isForbidden());
    for (String body :
        new String[] {"{}", "{\"body\":\"   \"}", "{\"body\":\"" + "a".repeat(5001) + "\"}"}) {
      mvc.perform(
              post(path)
                  .session(session)
                  .with(csrf())
                  .contentType("application/json")
                  .content(body))
          .andExpect(status().isBadRequest());
    }
    mvc.perform(
            post(path)
                .session(login(teacher))
                .with(csrf())
                .contentType("application/json")
                .content("{\"body\":\"Help\"}"))
        .andExpect(status().isForbidden());
    var q = questionService.ask(first.getLessonId(), "Help", actor(student));
    mvc.perform(
            put("/api/questions/" + q.questionId() + "/answer")
                .session(session)
                .with(csrf())
                .contentType("application/json")
                .content("{\"body\":\"Answer\"}"))
        .andExpect(status().isForbidden());
    var teacherSession = login(teacher);
    mvc.perform(
            put("/api/questions/" + q.questionId() + "/answer")
                .session(teacherSession)
                .contentType("application/json")
                .content("{\"body\":\"Answer\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            put("/api/questions/" + q.questionId() + "/visibility")
                .session(teacherSession)
                .with(csrf())
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isBadRequest());
    assertNull(questionService.get(q.questionId(), actor(student)).answer());
  }

  @Test
  void questionAndAnswerRollbackIfTheirNotificationFails() {
    enrollForQuestions(student);
    doThrow(new IllegalStateException("Notification failed"))
        .when(notifications)
        .save(any(Notification.class));
    assertThrows(
        IllegalStateException.class,
        () -> questionService.ask(first.getLessonId(), "Help", actor(student)));
    assertEquals(0, questions.count());
    reset(notifications);
    var q = questionService.ask(first.getLessonId(), "Help", actor(student));
    doThrow(new IllegalStateException("Notification failed"))
        .when(notifications)
        .save(any(Notification.class));
    assertThrows(
        IllegalStateException.class,
        () -> questionService.answer(q.questionId(), "Answer", actor(teacher)));
    assertNull(questionService.get(q.questionId(), actor(student)).answer());
  }

  @Test
  void removingLessonCascadesQuestionsAndDoesNotBreakProgress() {
    enrollForQuestions(student);
    questionService.ask(first.getLessonId(), "Help", actor(student));
    lessonService.deleteLesson(first.getLessonId(), actor(teacher));
    assertEquals(0, questions.count());
    assertFalse(lessons.existsById(first.getLessonId()));
  }

  @Test
  void guestsCanDiscoverPublishedCoursesWithoutReceivingLessonContent() throws Exception {
    var draft = course("Secret draft", BigDecimal.ZERO);
    draft.setStatus(CourseStatus.DRAFT);
    draft.setCategory("Secret category");
    courses.saveAndFlush(draft);
    lesson(free, "Hidden draft lesson", 3, false);
    mvc.perform(get("/api/discovery/catalog").param("size", "1").param("search", "Free"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.content[0].title").value("Free"));
    mvc.perform(get("/api/discovery/categories"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Secret category"))));
    mvc.perform(get("/api/discovery/courses/" + free.getCourseId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.lessons.length()").value(2))
        .andExpect(jsonPath("$.data.lessons[0].textContent").doesNotExist())
        .andExpect(jsonPath("$.data.lessons[0].contentUrl").doesNotExist())
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Private lesson body"))));
    mvc.perform(get("/api/discovery/courses/" + draft.getCourseId()))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/discovery/courses/" + draft.getCourseId()).session(login(admin)))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/lessons/" + first.getLessonId())).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/discovery/catalog").param("size", "101"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void publicSessionReturnsOnlyTheCurrentActiveProfile() throws Exception {
    mvc.perform(get("/api/auth/session"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isEmpty());
    var session = login(student);
    mvc.perform(get("/api/auth/session").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.userId").value(student.getUserId()))
        .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    student.setIsActive(false);
    users.saveAndFlush(student);
    mvc.perform(get("/api/auth/session").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isEmpty());
  }

  @Test
  void teacherCanImproveOwnCourseMetadataButCannotChangeAssignmentOrPrice() throws Exception {
    var session = login(teacher);
    var body =
        "{\"title\":\"Improved course\",\"teacherId\":%d,\"description\":\"Clear introduction\",\"learningOutcomes\":\"Build a small application\",\"price\":0}"
            .formatted(teacher.getUserId());
    mvc.perform(
            put("/api/courses/" + free.getCourseId())
                .session(session)
                .with(csrf())
                .contentType("application/json")
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.learningOutcomes").value("Build a small application"));
    mvc.perform(
            put("/api/courses/" + free.getCourseId())
                .session(session)
                .with(csrf())
                .contentType("application/json")
                .content(body.replace("\"price\":0", "\"price\":100")))
        .andExpect(status().isForbidden());
    mvc.perform(
            put("/api/courses/" + free.getCourseId())
                .session(session)
                .with(csrf())
                .contentType("application/json")
                .content(
                    body.replace(
                        "\"teacherId\":" + teacher.getUserId(),
                        "\"teacherId\":" + outsider.getUserId())))
        .andExpect(status().isForbidden());
    mvc.perform(
            put("/api/courses/" + free.getCourseId())
                .session(login(outsider))
                .with(csrf())
                .contentType("application/json")
                .content(body))
        .andExpect(status().isForbidden());
    mvc.perform(
            put("/api/courses/" + free.getCourseId())
                .session(login(student))
                .with(csrf())
                .contentType("application/json")
                .content(body))
        .andExpect(status().isForbidden());
    mvc.perform(
            put("/api/courses/" + free.getCourseId() + "/status")
                .session(session)
                .with(csrf())
                .contentType("application/json")
                .content("{\"status\":\"ARCHIVED\"}"))
        .andExpect(status().isForbidden());
    assertEquals(
        BigDecimal.ZERO.setScale(2), courses.findById(free.getCourseId()).orElseThrow().getPrice());
    assertEquals(
        teacher.getUserId(),
        courses.findById(free.getCourseId()).orElseThrow().getTeacher().getUserId());
  }

  @Test
  void privateNotesPersistWithoutCompletingLessonsAndSurviveCompletion() throws Exception {
    var e = enrollmentService.enroll(free.getCourseId(), actor(student));
    var session = login(student);
    var path = "/api/enrollments/" + e.getEnrollmentId() + "/notes/" + first.getLessonId();
    mvc.perform(get(path).session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.note").value(""));
    mvc.perform(
            put(path)
                .session(session)
                .with(csrf())
                .contentType("application/json")
                .content("{\"note\":\"<script>my own idea</script>\"}"))
        .andExpect(status().isOk());
    mvc.perform(get(path).session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.note").value("<script>my own idea</script>"));
    assertEquals(
        0,
        enrollmentService
            .getEnrollmentDetail(e.getEnrollmentId(), actor(student))
            .getProgressPercentage()
            .intValue());
    enrollmentService.completeLesson(e.getEnrollmentId(), first.getLessonId(), actor(student));
    assertEquals(
        "<script>my own idea</script>",
        enrollmentService.getNote(e.getEnrollmentId(), first.getLessonId(), actor(student)).note());
    enrollmentService.saveNote(e.getEnrollmentId(), first.getLessonId(), "", actor(student));
    assertEquals(
        "",
        enrollmentService.getNote(e.getEnrollmentId(), first.getLessonId(), actor(student)).note());
    assertEquals(1, progress.findByEnrollment_EnrollmentId(e.getEnrollmentId()).size());
  }

  @Test
  void notesAndResumeRequireAnActiveOwnerAndPublishedLessonInTheirCourse() throws Exception {
    var e = enrollmentService.enroll(free.getCourseId(), actor(student));
    var other = user("another-student", Role.STUDENT);
    var path = "/api/enrollments/" + e.getEnrollmentId() + "/notes/" + first.getLessonId();
    mvc.perform(get(path)).andExpect(status().isUnauthorized());
    mvc.perform(get(path).session(login(other))).andExpect(status().isForbidden());
    mvc.perform(
            put(path)
                .session(login(other))
                .with(csrf())
                .contentType("application/json")
                .content("{\"note\":\"other\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(get(path).session(login(teacher))).andExpect(status().isForbidden());
    var draft = lesson(free, "Draft", 3, false);
    assertThrows(
        ForbiddenException.class,
        () ->
            enrollmentService.saveNote(
                e.getEnrollmentId(), draft.getLessonId(), "draft", actor(student)));
    var paidLesson = lessons.findByCourse_CourseIdOrderByOrderIndex(paid.getCourseId()).getFirst();
    assertThrows(
        ForbiddenException.class,
        () ->
            enrollmentService.accessLesson(
                e.getEnrollmentId(), paidLesson.getLessonId(), actor(student)));
    var entity = enrollments.findById(e.getEnrollmentId()).orElseThrow();
    entity.setStatus(EnrollmentStatus.DROPPED);
    enrollments.saveAndFlush(entity);
    assertThrows(
        ForbiddenException.class,
        () -> enrollmentService.getNote(e.getEnrollmentId(), first.getLessonId(), actor(student)));
    assertThrows(
        ForbiddenException.class,
        () ->
            enrollmentService.accessLesson(
                e.getEnrollmentId(), first.getLessonId(), actor(student)));
  }

  @Test
  void recentLessonIsRestoredAndHiddenLessonsAreExcludedFromResume() {
    var e = enrollmentService.enroll(free.getCourseId(), actor(student));
    enrollmentService.accessLesson(e.getEnrollmentId(), first.getLessonId(), actor(student));
    enrollmentService.accessLesson(e.getEnrollmentId(), second.getLessonId(), actor(student));
    assertEquals(
        second.getLessonId(),
        enrollmentService
            .getEnrollmentDetail(e.getEnrollmentId(), actor(student))
            .getLastLessonId());
    assertEquals(
        0,
        enrollmentService
            .getEnrollmentDetail(e.getEnrollmentId(), actor(student))
            .getProgressPercentage()
            .intValue());
    second.setIsPublished(false);
    lessons.saveAndFlush(second);
    assertEquals(
        first.getLessonId(),
        enrollmentService
            .getEnrollmentDetail(e.getEnrollmentId(), actor(student))
            .getLastLessonId());
  }

  @Test
  void notesValidateSizeAndCsrf() throws Exception {
    var e = enrollmentService.enroll(free.getCourseId(), actor(student));
    var session = login(student);
    var path = "/api/enrollments/" + e.getEnrollmentId() + "/notes/" + first.getLessonId();
    mvc.perform(
            put(path)
                .session(session)
                .contentType("application/json")
                .content("{\"note\":\"test\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            put(path)
                .session(session)
                .with(csrf())
                .contentType("application/json")
                .content("{\"note\":null}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            put(path)
                .session(session)
                .with(csrf())
                .contentType("application/json")
                .content("{\"note\":\"" + "x".repeat(10001) + "\"}"))
        .andExpect(status().isBadRequest());
    assertTrue(progress.findByEnrollment_EnrollmentId(e.getEnrollmentId()).isEmpty());
  }

  @Test
  void unregisteredLessonListRedactsContent() {
    var list = lessonService.getLessonsByCourse(paid.getCourseId(), actor(student));
    assertEquals(1, list.size());
    assertNull(list.getFirst().getTextContent());
    assertNull(list.getFirst().getContentUrl());
    assertThrows(
        ForbiddenException.class,
        () -> lessonService.getLessonById(list.getFirst().getLessonId(), actor(student)));
  }

  @Test
  void unrelatedTeacherCannotReadFullContent() {
    assertThrows(
        ForbiddenException.class,
        () -> lessonService.getLessonById(first.getLessonId(), actor(outsider)));
  }

  @Test
  void registeredStudentCanReadContent() {
    enrollmentService.enroll(free.getCourseId(), actor(student));
    assertEquals(
        "Private lesson body",
        lessonService.getLessonById(first.getLessonId(), actor(student)).getTextContent());
  }

  @Test
  void draftCourseDoesNotLeakLessons() {
    paid.setStatus(CourseStatus.DRAFT);
    courses.saveAndFlush(paid);
    assertThrows(
        ResourceNotFoundException.class,
        () -> lessonService.getLessonsByCourse(paid.getCourseId(), actor(student)));
  }

  @Test
  void teacherCanFindOwnDraftButNotOthers() {
    free.setStatus(CourseStatus.DRAFT);
    courses.saveAndFlush(free);
    assertTrue(
        courseService.getCourses(null, null, null, actor(teacher)).stream()
            .anyMatch(c -> c.getCourseId().equals(free.getCourseId())));
    assertFalse(
        courseService.getCourses(null, null, null, actor(outsider)).stream()
            .anyMatch(c -> c.getCourseId().equals(free.getCourseId())));
  }

  @Test
  void catalogPaginatesBeforeReturningVisibleCourses() throws Exception {
    var hidden = course("Hidden", BigDecimal.ZERO);
    hidden.setStatus(CourseStatus.DRAFT);
    courses.saveAndFlush(hidden);
    var session = login(student);
    mvc.perform(get("/api/courses/catalog").session(session).param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(2))
        .andExpect(jsonPath("$.data.totalPages").value(2))
        .andExpect(jsonPath("$.data.content.length()").value(1))
        .andExpect(jsonPath("$.data.content[0].courseId").value(paid.getCourseId()))
        .andExpect(jsonPath("$.data.content[0].lessons").isEmpty());
    mvc.perform(get("/api/courses/catalog").session(session).param("size", "1").param("page", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.page").value(1))
        .andExpect(jsonPath("$.data.content[0].courseId").value(free.getCourseId()));
    mvc.perform(get("/api/courses/catalog").session(session).param("page", "99"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(2))
        .andExpect(jsonPath("$.data.content.length()").value(0));
  }

  @Test
  void catalogAndCategoriesRespectEachRole() throws Exception {
    free.setStatus(CourseStatus.DRAFT);
    free.setCategory("Private category");
    courses.saveAndFlush(free);
    assertEquals(
        2,
        courseService
            .getCatalog(null, null, null, null, false, "new", 0, 9, actor(admin))
            .totalElements());
    assertEquals(
        2,
        courseService
            .getCatalog(null, null, null, null, false, "new", 0, 9, actor(teacher))
            .totalElements());
    assertEquals(
        1,
        courseService
            .getCatalog(null, null, null, null, false, "new", 0, 9, actor(outsider))
            .totalElements());
    assertEquals(
        0,
        courseService
            .getCatalog(
                null,
                teacher.getUserId(),
                CourseStatus.DRAFT,
                null,
                false,
                "new",
                0,
                9,
                actor(student))
            .totalElements());
    assertEquals(
        1,
        courseService
            .getCatalog(
                null,
                teacher.getUserId(),
                CourseStatus.DRAFT,
                null,
                false,
                "new",
                0,
                9,
                actor(teacher))
            .totalElements());
    assertTrue(
        courseService
            .getCategories(teacher.getUserId(), actor(teacher))
            .contains("Private category"));
    assertFalse(
        courseService
            .getCategories(teacher.getUserId(), actor(outsider))
            .contains("Private category"));
    mvc.perform(get("/api/courses/categories").session(login(student)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0]").value("Lập trình"));
  }

  @Test
  void catalogCombinesSearchCategoryPriceAndTeacherFilters() {
    free.setCategory("Thiết kế");
    courses.saveAndFlush(free);
    var result =
        courseService.getCatalog(
            " TEACHER ",
            teacher.getUserId(),
            CourseStatus.PUBLISHED,
            "Thiết kế",
            true,
            "price",
            0,
            9,
            actor(student));
    assertEquals(1, result.totalElements());
    assertEquals(free.getCourseId(), result.content().getFirst().getCourseId());
    assertEquals(
        0,
        courseService
            .getCatalog(null, outsider.getUserId(), null, null, false, "new", 0, 9, actor(student))
            .totalElements());
    paid.setDescription("Special syllabus");
    courses.saveAndFlush(paid);
    assertEquals(
        paid.getCourseId(),
        courseService
            .getCatalog("SYLLABUS", null, null, null, false, "new", 0, 9, actor(student))
            .content()
            .getFirst()
            .getCourseId());
  }

  @Test
  void catalogSearchTreatsWildcardCharactersLiterally() {
    var special = course("100%_\\ complete", BigDecimal.ZERO);
    assertEquals(
        special.getCourseId(),
        courseService
            .getCatalog("%_\\", null, null, null, false, "new", 0, 9, actor(student))
            .content()
            .getFirst()
            .getCourseId());
    assertEquals(
        1,
        courseService
            .getCatalog("%", null, null, null, false, "new", 0, 9, actor(student))
            .totalElements());
  }

  @Test
  void catalogSortHasStableTieBreakerAcrossPages() {
    var newest = course("Another free", BigDecimal.ZERO);
    var firstPage =
        courseService.getCatalog(null, null, null, null, false, "price", 0, 1, actor(student));
    var secondPage =
        courseService.getCatalog(null, null, null, null, false, "price", 1, 1, actor(student));
    assertEquals(newest.getCourseId(), firstPage.content().getFirst().getCourseId());
    assertEquals(free.getCourseId(), secondPage.content().getFirst().getCourseId());
    assertEquals(
        newest.getCourseId(),
        courseService
            .getCatalog(null, null, null, null, false, "title", 0, 1, actor(student))
            .content()
            .getFirst()
            .getCourseId());
  }

  @Test
  void catalogReturnsCorrectAggregatesWithoutCountingDraftLessons() {
    lesson(free, "Hidden lesson", 3, false);
    enrollmentService.enroll(free.getCourseId(), actor(student));
    var review = new Review();
    review.setCourse(free);
    review.setStudent(student);
    review.setRating(4);
    reviews.saveAndFlush(review);
    var result =
        courseService
            .getCatalog("Free", null, null, null, false, "new", 0, 9, actor(student))
            .content()
            .getFirst();
    assertEquals(2L, result.getLessonCount());
    assertEquals(1L, result.getEnrollmentCount());
    assertEquals(4.0, result.getAverageRating());
    var emptyCourse = course("Empty", BigDecimal.ZERO);
    var empty =
        courseService
            .getCatalog("Empty", null, null, null, false, "new", 0, 9, actor(student))
            .content()
            .getFirst();
    assertEquals(emptyCourse.getCourseId(), empty.getCourseId());
    assertEquals(0L, empty.getLessonCount());
    assertEquals(0L, empty.getEnrollmentCount());
    assertNull(empty.getAverageRating());
  }

  @Test
  void catalogQueryCountDoesNotGrowWithCourseCount() {
    for (int i = 0; i < 15; i++) course("Extra " + i, BigDecimal.ZERO);
    var statistics =
        entityManagerFactory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
    boolean enabled = statistics.isStatisticsEnabled();
    statistics.setStatisticsEnabled(true);
    try {
      statistics.clear();
      assertEquals(
          9,
          courseService
              .getCatalog(null, null, null, null, false, "new", 0, 9, actor(student))
              .content()
              .size());
      assertTrue(
          statistics.getPrepareStatementCount() <= 5,
          "Catalog must use at most 5 queries: courses with teacher, total, and 3 grouped"
              + " statistics");
    } finally {
      statistics.setStatisticsEnabled(enabled);
    }
  }

  @Test
  void catalogRejectsInvalidParametersAndRequiresLogin() throws Exception {
    mvc.perform(get("/api/courses/catalog")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/courses/categories")).andExpect(status().isUnauthorized());
    var session = login(student);
    for (var parameter :
        new String[][] {
          {"page", "-1"}, {"page", "2147483647"}, {"size", "0"}, {"size", "101"},
          {"size", "abc"}, {"sort", "unknown"}, {"status", "unknown"}, {"freeOnly", "unknown"},
          {"search", "x".repeat(256)}, {"category", "x".repeat(256)}
        }) {
      mvc.perform(get("/api/courses/catalog").session(session).param(parameter[0], parameter[1]))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.success").value(false));
    }
  }

  @Test
  void paidCourseCannotBeEnrolledDirectly() {
    assertThrows(
        BadRequestException.class,
        () -> enrollmentService.enroll(paid.getCourseId(), actor(student)));
  }

  @Test
  void unpublishedLessonCannotBeCompleted() {
    var e = enrollmentService.enroll(free.getCourseId(), actor(student));
    var draft = lesson(free, "Draft", 3, false);
    assertThrows(
        BadRequestException.class,
        () ->
            enrollmentService.completeLesson(
                e.getEnrollmentId(), draft.getLessonId(), actor(student)));
  }

  @Test
  void publicationChangesRecalculateProgressAndCompletion() {
    var e = enrollmentService.enroll(free.getCourseId(), actor(student));
    enrollmentService.completeLesson(e.getEnrollmentId(), first.getLessonId(), actor(student));
    enrollmentService.completeLesson(e.getEnrollmentId(), second.getLessonId(), actor(student));
    var hide = new UpdateLessonPublishRequest();
    hide.setIsPublished(false);
    lessonService.updatePublishStatus(second.getLessonId(), hide, actor(teacher));
    assertEquals(
        0,
        enrollmentService
            .getEnrollmentDetail(e.getEnrollmentId(), actor(student))
            .getProgressPercentage()
            .compareTo(new BigDecimal("100")));
    var third = lesson(free, "Third", 3, false);
    var publish = new UpdateLessonPublishRequest();
    publish.setIsPublished(true);
    lessonService.updatePublishStatus(third.getLessonId(), publish, actor(teacher));
    var updated = enrollments.findById(e.getEnrollmentId()).orElseThrow();
    assertEquals(0, updated.getProgressPercentage().compareTo(new BigDecimal("50")));
    assertEquals(EnrollmentStatus.ENROLLED, updated.getStatus());
    assertNull(updated.getCompletionDate());
  }

  @Test
  void deletingLessonRemovesProgressAndRecalculates() {
    var e = enrollmentService.enroll(free.getCourseId(), actor(student));
    enrollmentService.completeLesson(e.getEnrollmentId(), first.getLessonId(), actor(student));
    lessonService.deleteLesson(first.getLessonId(), actor(teacher));
    assertTrue(progress.findByEnrollment_EnrollmentId(e.getEnrollmentId()).isEmpty());
    assertEquals(
        0,
        enrollments
            .findById(e.getEnrollmentId())
            .orElseThrow()
            .getProgressPercentage()
            .compareTo(BigDecimal.ZERO));
  }

  @Test
  void repeatedCompletionIsIdempotent() {
    var e = enrollmentService.enroll(free.getCourseId(), actor(student));
    enrollmentService.completeLesson(e.getEnrollmentId(), first.getLessonId(), actor(student));
    var at =
        progress.findByEnrollment_EnrollmentId(e.getEnrollmentId()).getFirst().getCompletedAt();
    enrollmentService.completeLesson(e.getEnrollmentId(), first.getLessonId(), actor(student));
    assertEquals(1, progress.findByEnrollment_EnrollmentId(e.getEnrollmentId()).size());
    assertEquals(
        at,
        progress.findByEnrollment_EnrollmentId(e.getEnrollmentId()).getFirst().getCompletedAt());
  }

  @Test
  void paymentConfirmationGrantsExactlyOneEnrollment() {
    var p = paymentService.createPayment(paid.getCourseId(), actor(student));
    assertNotNull(p.getTransferNote());
    paymentService.confirmPayment(p.getPaymentId(), actor(admin));
    assertEquals(
        PaymentStatus.CONFIRMED, payments.findById(p.getPaymentId()).orElseThrow().getStatus());
    assertEquals(1, enrollments.countByCourse_CourseId(paid.getCourseId()));
    assertThrows(
        BadRequestException.class,
        () -> paymentService.confirmPayment(p.getPaymentId(), actor(admin)));
  }

  @Test
  void confirmationRollsBackIfDownstreamWriteFails() {
    var p = paymentService.createPayment(paid.getCourseId(), actor(student));
    doThrow(new IllegalStateException("Simulated database failure"))
        .when(notifications)
        .save(any(Notification.class));
    assertThrows(
        IllegalStateException.class,
        () -> paymentService.confirmPayment(p.getPaymentId(), actor(admin)));
    assertEquals(
        PaymentStatus.PENDING, payments.findById(p.getPaymentId()).orElseThrow().getStatus());
    assertEquals(0, enrollments.countByCourse_CourseId(paid.getCourseId()));
  }

  @Test
  void concurrentPaymentRequestsCreateOnlyOne() throws Exception {
    try (var executor = Executors.newFixedThreadPool(2)) {
      var start = new CountDownLatch(1);
      Callable<Boolean> work =
          () -> {
            start.await();
            try {
              paymentService.createPayment(paid.getCourseId(), actor(student));
              return true;
            } catch (ConflictException ex) {
              return false;
            }
          };
      var a = executor.submit(work);
      var b = executor.submit(work);
      start.countDown();
      assertNotEquals(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
      assertEquals(
          1, payments.findByStudent_UserIdOrderByCreatedAtDesc(student.getUserId()).size());
    }
  }

  @Test
  void concurrentEnrollmentsCreateOnlyOne() throws Exception {
    try (var executor = Executors.newFixedThreadPool(2)) {
      var start = new CountDownLatch(1);
      Callable<Boolean> work =
          () -> {
            start.await();
            try {
              enrollmentService.enroll(free.getCourseId(), actor(student));
              return true;
            } catch (ConflictException ex) {
              return false;
            }
          };
      var a = executor.submit(work);
      var b = executor.submit(work);
      start.countDown();
      assertNotEquals(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
      assertEquals(1, enrollments.countByCourse_CourseId(free.getCourseId()));
    }
  }

  @Test
  void blockedAccountLosesExistingSession() throws Exception {
    var session = login(student);
    var request = new UpdateStatusRequest();
    request.setIsActive(false);
    userService.updateStatus(student.getUserId(), request);
    mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
  }

  @Test
  void roleChangeRevokesExistingSession() throws Exception {
    var session = login(student);
    var request = new UpdateRoleRequest();
    request.setRole(Role.TEACHER);
    userService.updateRole(student.getUserId(), request);
    mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
  }

  @Test
  void selfPasswordChangeRequiresOldPasswordEvenForAdmin() {
    var r = new ChangePasswordRequest();
    r.setOldPassword("wrong");
    r.setNewPassword("NewPassword123!");
    assertThrows(
        ForbiddenException.class,
        () -> userService.changePassword(admin.getUserId(), r, actor(admin)));
  }

  @Test
  void loginRequiresCsrfAndReturnsJsonErrors() throws Exception {
    mvc.perform(post("/api/auth/login").contentType("application/json").content("{}"))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith("application/json"));
    mvc.perform(get("/api/auth/csrf"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.token").isNotEmpty());
  }

  @Test
  void negativeCoursePriceIsRejected() throws Exception {
    mvc.perform(
            post("/api/courses")
                .session(login(admin))
                .with(csrf())
                .contentType("application/json")
                .content(
                    "{\"title\":\"Invalid\",\"teacherId\":"
                        + teacher.getUserId()
                        + ",\"price\":-1}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void studentCannotManageUsers() throws Exception {
    mvc.perform(get("/api/users").session(login(student))).andExpect(status().isForbidden());
  }

  @Test
  void registrationCannotEscalateRole() throws Exception {
    mvc.perform(
            post("/api/auth/register")
                .with(csrf())
                .contentType("application/json")
                .content(
                    "{\"username\":\"newuser\",\"email\":\"new@example.invalid\",\"fullName\":\"New"
                        + " User\",\"password\":\"Password123!\",\"role\":\"ADMIN\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.role").value("STUDENT"));
  }

  @Test
  void dangerousContentLinkIsRejected() {
    var r = new CreateLessonRequest();
    r.setTitle("Unsafe");
    r.setOrderIndex(4);
    r.setContentUrl("javascript:alert(1)");
    assertThrows(
        BadRequestException.class,
        () -> lessonService.createLesson(free.getCourseId(), r, actor(teacher)));
  }

  @Test
  void databaseRejectsDuplicateEnrollment() {
    enrollmentService.enroll(free.getCourseId(), actor(student));
    var duplicate = new Enrollment();
    duplicate.setCourse(free);
    duplicate.setStudent(student);
    assertThrows(
        org.springframework.dao.DataIntegrityViolationException.class,
        () -> enrollments.saveAndFlush(duplicate));
  }

  @Test
  void concurrentConfirmationsGrantAccessOnlyOnce() throws Exception {
    var p = paymentService.createPayment(paid.getCourseId(), actor(student));
    try (var executor = Executors.newFixedThreadPool(2)) {
      var start = new CountDownLatch(1);
      Callable<Boolean> work =
          () -> {
            start.await();
            try {
              paymentService.confirmPayment(p.getPaymentId(), actor(admin));
              return true;
            } catch (BadRequestException ex) {
              return false;
            }
          };
      var a = executor.submit(work);
      var b = executor.submit(work);
      start.countDown();
      assertNotEquals(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
    }
    assertEquals(1, enrollments.countByCourse_CourseId(paid.getCourseId()));
    assertEquals(
        1, notifications.findByUser_UserIdOrderByCreatedAtDesc(student.getUserId()).size());
  }

  @Test
  void rejectedPaymentDoesNotGrantAccessAndCanBeRequestedAgain() {
    var p = paymentService.createPayment(paid.getCourseId(), actor(student));
    paymentService.rejectPayment(p.getPaymentId(), actor(admin));
    assertEquals(0, enrollments.countByCourse_CourseId(paid.getCourseId()));
    assertThrows(
        BadRequestException.class,
        () -> paymentService.confirmPayment(p.getPaymentId(), actor(admin)));
    assertNotEquals(
        p.getPaymentId(),
        paymentService.createPayment(paid.getCourseId(), actor(student)).getPaymentId());
  }

  @Test
  void concurrentConfirmAndRejectHaveOneConsistentOutcome() throws Exception {
    var p = paymentService.createPayment(paid.getCourseId(), actor(student));
    try (var executor = Executors.newFixedThreadPool(2)) {
      var start = new CountDownLatch(1);
      var confirm =
          executor.submit(
              () -> {
                start.await();
                try {
                  paymentService.confirmPayment(p.getPaymentId(), actor(admin));
                  return true;
                } catch (BadRequestException ex) {
                  return false;
                }
              });
      var reject =
          executor.submit(
              () -> {
                start.await();
                try {
                  paymentService.rejectPayment(p.getPaymentId(), actor(admin));
                  return true;
                } catch (BadRequestException ex) {
                  return false;
                }
              });
      start.countDown();
      boolean confirmed = confirm.get(10, TimeUnit.SECONDS);
      assertNotEquals(confirmed, reject.get(10, TimeUnit.SECONDS));
      assertEquals(
          confirmed ? PaymentStatus.CONFIRMED : PaymentStatus.REJECTED,
          payments.findById(p.getPaymentId()).orElseThrow().getStatus());
      assertEquals(confirmed ? 1 : 0, enrollments.countByCourse_CourseId(paid.getCourseId()));
    }
  }

  @Test
  void passwordChangeRevokesOldSessionAndOldPassword() throws Exception {
    var session = login(student);
    var request = new ChangePasswordRequest();
    request.setOldPassword("Password123!");
    request.setNewPassword("NewPassword123!");
    userService.changePassword(student.getUserId(), request, actor(student));
    mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/auth/login")
                .with(csrf())
                .contentType("application/json")
                .content("{\"username\":\"student\",\"password\":\"Password123!\"}"))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/auth/login")
                .with(csrf())
                .contentType("application/json")
                .content("{\"username\":\"student\",\"password\":\"NewPassword123!\"}"))
        .andExpect(status().isOk());
  }

  @Test
  void studentCannotConfirmPaymentEvenWithValidCsrf() throws Exception {
    var p = paymentService.createPayment(paid.getCourseId(), actor(student));
    mvc.perform(
            put("/api/payments/" + p.getPaymentId() + "/confirm")
                .session(login(student))
                .with(csrf()))
        .andExpect(status().isForbidden());
    assertEquals(
        PaymentStatus.PENDING, payments.findById(p.getPaymentId()).orElseThrow().getStatus());
    assertEquals(0, enrollments.countByCourse_CourseId(paid.getCourseId()));
  }

  @Test
  void enrollmentCannotBeReadOrCompletedByAnotherUser() {
    var e = enrollmentService.enroll(free.getCourseId(), actor(student));
    assertThrows(
        ForbiddenException.class,
        () -> enrollmentService.getEnrollmentDetail(e.getEnrollmentId(), actor(outsider)));
    assertThrows(
        ForbiddenException.class,
        () ->
            enrollmentService.completeLesson(
                e.getEnrollmentId(), first.getLessonId(), actor(outsider)));
    assertTrue(progress.findByEnrollment_EnrollmentId(e.getEnrollmentId()).isEmpty());
  }

  @Test
  void lessonFromDifferentCourseCannotAffectProgress() {
    var e = enrollmentService.enroll(free.getCourseId(), actor(student));
    var wrongLesson = lessons.findByCourse_CourseIdOrderByOrderIndex(paid.getCourseId()).getFirst();
    assertThrows(
        BadRequestException.class,
        () ->
            enrollmentService.completeLesson(
                e.getEnrollmentId(), wrongLesson.getLessonId(), actor(student)));
    assertTrue(progress.findByEnrollment_EnrollmentId(e.getEnrollmentId()).isEmpty());
  }

  @Test
  void authenticatedWriteWithoutCsrfDoesNotChangeData() throws Exception {
    var p = paymentService.createPayment(paid.getCourseId(), actor(student));
    mvc.perform(put("/api/payments/" + p.getPaymentId() + "/confirm").session(login(admin)))
        .andExpect(status().isForbidden());
    assertEquals(
        PaymentStatus.PENDING, payments.findById(p.getPaymentId()).orElseThrow().getStatus());
  }

  @Test
  void malformedRequestIsClientErrorAndAnonymousApiRequiresLogin() throws Exception {
    mvc.perform(get("/api/courses")).andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/courses")
                .session(login(admin))
                .with(csrf())
                .contentType("application/json")
                .content("{broken"))
        .andExpect(status().isBadRequest());
    assertEquals(2, courses.count());
  }

  @Test
  void lessonMediaRoundTripsAndMetadataDoesNotExposeContent() throws Exception {
    var request = new UpdateLessonRequest();
    request.setTitle("Structured"); request.setOrderIndex(1);
    request.setTextContent("# Mục tiêu\n- Java");
    request.setContentFormat("MARKDOWN"); request.setVideoUrl("https://youtu.be/abcdefghijk");
    lessonService.updateLesson(first.getLessonId(), request, actor(teacher));
    var saved = lessonService.getLessonById(first.getLessonId(), actor(teacher));
    assertEquals("MARKDOWN", saved.getContentFormat());
    assertEquals(request.getVideoUrl(), saved.getVideoUrl());
    assertTrue(lessonService.getContentPreview(first.getLessonId(), actor(teacher)).getHasVideoOrDocument());
    var metadata = lessonService.getLessonsByCourse(free.getCourseId(), actor(student)).getFirst();
    assertNull(metadata.getVideoUrl()); assertNull(metadata.getTextContent());
    request.setVideoUrl("javascript:alert(1)");
    assertThrows(BadRequestException.class, () -> lessonService.updateLesson(first.getLessonId(), request, actor(teacher)));
  }

  @Test
  void attachmentsRequireEnrollmentAndCorrectManagerAndUsePrivateDownloads() throws Exception {
    var session = login(teacher);
    byte[] content = "%PDF-1.4 sample".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    mvc.perform(multipart("/api/lessons/" + first.getLessonId() + "/resources")
        .file(new org.springframework.mock.web.MockMultipartFile("file", "guide.pdf", "application/pdf", content))
        .session(session).with(csrf())).andExpect(status().isOk());
    int id = jdbc.queryForObject("SELECT resource_id FROM lesson_resources", Integer.class);
    var studentSession = login(student);
    mvc.perform(get("/api/lesson-resources/" + id + "/content").session(studentSession)).andExpect(status().isForbidden());
    enrollForQuestions(student);
    mvc.perform(get("/api/lesson-resources/" + id + "/content").session(studentSession))
        .andExpect(status().isOk()).andExpect(content().bytes(content))
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    mvc.perform(delete("/api/lesson-resources/" + id).session(login(outsider)).with(csrf())).andExpect(status().isForbidden());
    mvc.perform(post("/api/lessons/" + first.getLessonId() + "/publish")
        .session(session).with(csrf())).andExpect(status().isMethodNotAllowed());
    first.setIsPublished(false); lessons.saveAndFlush(first);
    mvc.perform(get("/api/lesson-resources/" + id + "/content").session(studentSession)).andExpect(status().isNotFound());
    mvc.perform(delete("/api/lesson-resources/" + id).session(session).with(csrf())).andExpect(status().isOk());
    assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM lesson_resources", Integer.class));
  }

  @Test
  void attachmentUploadRejectsSpoofedFilesAndMissingCsrf() throws Exception {
    var file = new org.springframework.mock.web.MockMultipartFile("file", "bad.png", "image/png", "<script>x</script>".getBytes());
    var session = login(teacher);
    mvc.perform(multipart("/api/lessons/" + first.getLessonId() + "/resources").file(file).session(session).with(csrf()))
        .andExpect(status().isBadRequest());
    mvc.perform(multipart("/api/lessons/" + first.getLessonId() + "/resources").file(file).session(session))
        .andExpect(status().isForbidden());
    assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM lesson_resources", Integer.class));
  }

  @Autowired LessonDraftService lessonDrafts;

  SaveLessonDraftRequest draftRequest(int expected, Integer base, String text) {
    var r = new SaveLessonDraftRequest();
    r.setExpectedRevision(expected); r.setBaseRevision(base);
    r.setTitle("Draft title"); r.setOrderIndex(1); r.setTextContent(text);
    r.setContentFormat("MARKDOWN");
    return r;
  }

  UpdateLessonRequest contentRequest(int expected, Integer draftRevision) {
    var r = new UpdateLessonRequest();
    r.setTitle("Updated"); r.setOrderIndex(1); r.setTextContent("New live body");
    r.setContentFormat("MARKDOWN"); r.setExpectedRevision(expected); r.setDraftRevision(draftRevision);
    return r;
  }

  @Test
  void lessonDraftIsPrivateAndDoesNotChangePublishedContent() throws Exception {
    var d = lessonDrafts.save(free.getCourseId(), first.getLessonId(), draftRequest(0,0,"Unpublished draft"), actor(teacher));
    assertEquals(1, d.revision());
    assertEquals("Private lesson body", lessonService.getLessonById(first.getLessonId(),actor(teacher)).getTextContent());
    assertNull(lessonDrafts.get(free.getCourseId(),first.getLessonId(),actor(admin)));
    assertThrows(ForbiddenException.class, () -> lessonDrafts.get(free.getCourseId(),first.getLessonId(),actor(outsider)));
    assertThrows(ResourceNotFoundException.class, () -> lessonDrafts.get(paid.getCourseId(),first.getLessonId(),actor(teacher)));
    mvc.perform(get("/api/courses/"+free.getCourseId()+"/lesson-drafts/"+first.getLessonId()).session(login(student)))
        .andExpect(status().isForbidden());
    mvc.perform(put("/api/courses/"+free.getCourseId()+"/lesson-drafts/0").session(login(teacher))
        .contentType("application/json").content("{\"expectedRevision\":0}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void draftRevisionsRejectOtherTabsAndStayMonotonicAfterDiscard() {
    var d = lessonDrafts.save(free.getCourseId(),0,draftRequest(0,null,"First tab"),actor(teacher));
    assertThrows(ConflictException.class, () -> lessonDrafts.save(free.getCourseId(),0,draftRequest(0,null,"Second tab"),actor(teacher)));
    lessonDrafts.delete(free.getCourseId(),0,d.revision(),actor(teacher));
    var empty = lessonDrafts.get(free.getCourseId(),0,actor(teacher));
    assertNull(empty.content()); assertEquals(2,empty.revision());
    assertThrows(ConflictException.class, () -> lessonDrafts.save(free.getCourseId(),0,draftRequest(1,null,"Stale"),actor(teacher)));
    assertEquals(3,lessonDrafts.save(free.getCourseId(),0,draftRequest(2,null,"New"),actor(teacher)).revision());
  }

  @Test
  void savingLessonConsumesDraftAndCreatesRestorableHistory() {
    var d = lessonDrafts.save(free.getCourseId(),first.getLessonId(),draftRequest(0,0,"Draft"),actor(teacher));
    var live = lessonService.updateLesson(first.getLessonId(),contentRequest(0,d.revision()),actor(teacher));
    assertEquals(1,live.getContentRevision());
    assertNull(lessonDrafts.get(free.getCourseId(),first.getLessonId(),actor(teacher)).content());
    var history = lessonDrafts.history(first.getLessonId(),actor(teacher));
    assertEquals(1,history.size()); assertEquals("Private lesson body",history.getFirst().content().textContent());
    var restored = lessonDrafts.save(free.getCourseId(),first.getLessonId(),draftRequest(2,1,history.getFirst().content().textContent()),actor(teacher));
    assertEquals("Private lesson body", restored.content().textContent());
    assertEquals("New live body",lessonService.getLessonById(first.getLessonId(),actor(teacher)).getTextContent());
  }

  @Test
  void staleLessonSavePreservesDraftAndCurrentLesson() {
    var d = lessonDrafts.save(free.getCourseId(),first.getLessonId(),draftRequest(0,0,"Keep my draft"),actor(teacher));
    lessonService.updateLesson(first.getLessonId(),contentRequest(0,null),actor(admin));
    assertThrows(ConflictException.class, () -> lessonService.updateLesson(first.getLessonId(),contentRequest(0,d.revision()),actor(teacher)));
    assertEquals("Keep my draft",lessonDrafts.get(free.getCourseId(),first.getLessonId(),actor(teacher)).content().textContent());
    assertEquals(1,lessonDrafts.history(first.getLessonId(),actor(teacher)).size());
    assertThrows(ForbiddenException.class, () -> lessonDrafts.history(first.getLessonId(),actor(outsider)));
  }

  @Test
  void concurrentLessonEditorsHaveOnlyOneSuccessfulSave() throws Exception {
    var start = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      Callable<Boolean> save = () -> {
        start.await();
        try { lessonService.updateLesson(first.getLessonId(),contentRequest(0,null),actor(teacher)); return true; }
        catch (ConflictException e) { return false; }
      };
      var a = pool.submit(save); var b = pool.submit(save); start.countDown();
      assertNotEquals(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS));
      assertEquals(1,lessons.findById(first.getLessonId()).orElseThrow().getContentRevision());
      assertEquals(1,lessonDrafts.history(first.getLessonId(),actor(teacher)).size());
    }
  }

  @Autowired CourseStudentReportService courseStudentReports;

  @Test
  void courseStudentReportAggregatesOnlyThisCourseAndKeepsNotesPrivate() throws Exception {
    enrollForQuestions(student);
    var e = enrollments.findByStudent_UserIdAndCourse_CourseId(student.getUserId(),free.getCourseId()).orElseThrow();
    e.setProgressPercentage(new BigDecimal("50.00")); enrollments.saveAndFlush(e);
    var p = new LessonProgress(); p.setEnrollment(e); p.setLesson(first); p.setIsCompleted(true); p.setNote("Secret private note"); progress.saveAndFlush(p);
    var hidden = lesson(free,"Hidden",3,false);
    var hiddenProgress = new LessonProgress(); hiddenProgress.setEnrollment(e); hiddenProgress.setLesson(hidden); hiddenProgress.setIsCompleted(true); progress.saveAndFlush(hiddenProgress);
    var quiz = quizService.save(first.getLessonId(),quizRequest(0,true),actor(teacher));
    quizService.submit(first.getLessonId(),submission(quiz,2,1),actor(student));
    quizService.submit(first.getLessonId(),submission(quiz,0,1),actor(student));
    quizService.save(first.getLessonId(),quizRequest(1,false),actor(teacher));
    var otherEnrollment = new Enrollment(); otherEnrollment.setStudent(student); otherEnrollment.setCourse(paid); enrollments.saveAndFlush(otherEnrollment);
    var paidLesson = lessons.findByCourse_CourseIdOrderByOrderIndex(paid.getCourseId()).getFirst();
    var paidQuiz = quizService.save(paidLesson.getLessonId(),quizRequest(0,true),actor(teacher));
    quizService.submit(paidLesson.getLessonId(),submission(paidQuiz,0,1),actor(student));
    var firstPending = questionService.ask(second.getLessonId(),"First pending",actor(student));
    questionService.ask(first.getLessonId(),"Second pending",actor(student));
    var answered = questionService.ask(first.getLessonId(),"Answered",actor(student));
    questionService.answer(answered.questionId(),"Answer",actor(teacher));
    var hiddenQuestion = questionService.ask(first.getLessonId(),"Hidden",actor(student));
    questionService.visibility(hiddenQuestion.questionId(),true,actor(teacher));
    var latest = java.time.LocalDateTime.of(2030,1,2,3,4);
    jdbc.update("UPDATE lesson_questions SET created_at=? WHERE question_id=?",latest,firstPending.questionId());
    var report = courseStudentReports.get(free.getCourseId(),"",null,"name",0,10,actor(teacher));
    assertEquals(1,report.summary().students()); assertEquals(2,report.summary().publishedLessons()); assertEquals(2,report.summary().pendingQuestions());
    var row = report.students().content().getFirst();
    assertEquals(1,row.completedLessons()); assertEquals(2,row.quizAttempts());
    assertEquals(0,new BigDecimal("75").compareTo(row.averageScore())); assertEquals(100,row.bestScore());
    assertEquals(2,row.pendingQuestions()); assertEquals(firstPending.questionId(),row.pendingQuestionId()); assertEquals(second.getLessonId(),row.pendingLessonId()); assertEquals(latest,row.lastActivity());
    var json = mvc.perform(get("/api/courses/"+free.getCourseId()+"/students").session(login(teacher)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data.students.content[0].email").doesNotExist())
        .andReturn().getResponse().getContentAsString();
    assertFalse(json.contains("Secret private note")); assertFalse(json.contains("questionCount"));
  }

  @Test
  void courseStudentReportHasGlobalSummaryAndStableFilteredPagination() {
    enrollForQuestions(student);
    for (int i=0;i<11;i++) {
      var u = user(String.format("learner%02d",i),Role.STUDENT);
      var e = new Enrollment(); e.setStudent(u); e.setCourse(free);
      e.setStatus(i==0 ? EnrollmentStatus.COMPLETED : i==1 ? EnrollmentStatus.DROPPED : EnrollmentStatus.ENROLLED);
      e.setProgressPercentage(BigDecimal.valueOf(i==0?100:i*5)); enrollments.saveAndFlush(e);
    }
    var a = courseStudentReports.get(free.getCourseId(),"",null,"name",0,10,actor(teacher));
    var b = courseStudentReports.get(free.getCourseId(),"",null,"name",1,10,actor(teacher));
    assertEquals(12,a.students().totalElements()); assertEquals(2,a.students().totalPages()); assertEquals(10,a.students().content().size()); assertEquals(2,b.students().content().size());
    assertTrue(a.students().content().stream().noneMatch(x->b.students().content().stream().anyMatch(y->x.enrollmentId()==y.enrollmentId())));
    var filtered = courseStudentReports.get(free.getCourseId(),"learner",EnrollmentStatus.COMPLETED,"progress",0,10,actor(admin));
    assertEquals(1,filtered.students().totalElements()); assertEquals(12,filtered.summary().students()); assertEquals(1,filtered.summary().completed()); assertEquals(1,filtered.summary().dropped()); assertEquals(10,filtered.summary().enrolled());
    assertEquals(100,filtered.students().content().getFirst().progressPercentage().intValue());
  }

  @Test
  void courseStudentReportHandlesEmptyAndLiteralWildcardSearches() {
    var empty = courseStudentReports.get(free.getCourseId(),"",null,"name",0,10,actor(teacher));
    assertEquals(0,empty.summary().students()); assertEquals(0,empty.students().totalPages());
    student.setFullName("Learner %_!"); users.saveAndFlush(student); enrollForQuestions(student);
    assertEquals(1,courseStudentReports.get(free.getCourseId(),"%_!",null,"name",0,10,actor(teacher)).students().totalElements());
    assertEquals(0,courseStudentReports.get(free.getCourseId(),"missing",null,"name",0,10,actor(teacher)).students().totalElements());
    assertThrows(BadRequestException.class,()->courseStudentReports.get(free.getCourseId(),"x".repeat(256),null,"name",0,10,actor(teacher)));
    assertThrows(BadRequestException.class,()->courseStudentReports.get(free.getCourseId(),"",null,"name",-1,10,actor(teacher)));
    assertThrows(BadRequestException.class,()->courseStudentReports.get(free.getCourseId(),"",null,"name",0,101,actor(teacher)));
  }

  @Test
  void courseStudentReportRequiresTheCourseManager() throws Exception {
    assertThrows(ForbiddenException.class,()->courseStudentReports.get(free.getCourseId(),"",null,"name",0,10,actor(outsider)));
    assertThrows(ForbiddenException.class,()->courseStudentReports.get(free.getCourseId(),"",null,"name",0,10,actor(student)));
    mvc.perform(get("/api/courses/"+free.getCourseId()+"/students")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/courses/"+free.getCourseId()+"/students").session(login(student))).andExpect(status().isForbidden());
    mvc.perform(get("/api/courses/"+free.getCourseId()+"/students").session(login(outsider))).andExpect(status().isForbidden());
    mvc.perform(get("/api/courses/"+free.getCourseId()+"/students?sort=invalid").session(login(teacher))).andExpect(status().isBadRequest());
  }
}
