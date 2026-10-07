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
  @MockitoSpyBean NotificationRepository notifications;
  User admin, teacher, student, outsider;
  Course free, paid;
  Lesson first, second;

  @BeforeEach
  void setup() {
    reset(notifications);
    for (String table :
        new String[] {
          "notifications",
          "reviews",
          "lesson_progress",
          "payments",
          "enrollments",
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
}
