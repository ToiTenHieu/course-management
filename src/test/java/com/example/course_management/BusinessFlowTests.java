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
          "lesson_questions",
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

  void enrollForQuestions(User u) {
    var e = new Enrollment();
    e.setStudent(u);
    e.setCourse(free);
    enrollments.saveAndFlush(e);
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
}
