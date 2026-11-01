package com.example.course_management;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.course_management.config.AiSettings;
import com.example.course_management.dto.request.AiTutorRequest;
import com.example.course_management.entity.*;
import com.example.course_management.exception.*;
import com.example.course_management.repository.*;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.*;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(properties = {
    "app.ai.provider=gemini", "app.ai.gemini.api-key=test-key", "app.ai.requests-per-minute=3"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiFeatureTests {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired UserRepository users;
  @Autowired CourseRepository courses;
  @Autowired LessonRepository lessons;
  @Autowired EnrollmentRepository enrollments;
  @Autowired LessonQuestionRepository questions;
  @Autowired QuestionReplyRepository replies;
  @Autowired NotificationRepository notifications;
  @Autowired LessonQuestionService questionService;
  @Autowired AiAssistantService ai;
  @Autowired AssignmentService assignments;
  @Autowired PasswordEncoder encoder;
  @MockitoBean AiClient client;
  User teacher, student, outsider;
  Course course;
  Lesson lesson, hiddenLesson;

  @BeforeEach
  void setup() {
    for (String table : new String[] {"audit_logs", "demo_seed_runs", "quiz_answers", "quiz_attempts",
        "quiz_questions", "lesson_quiz_versions", "lesson_questions", "notifications", "reviews",
        "lesson_progress", "payments", "enrollments", "lesson_drafts", "lesson_content_versions",
        "lesson_resources", "lessons", "courses", "users"}) jdbc.update("DELETE FROM " + table);
    teacher = user("teacher", Role.TEACHER);
    student = user("student", Role.STUDENT);
    outsider = user("outsider", Role.STUDENT);
    course = new Course();
    course.setCategory("Lập trình");
    course.setLevel("Cơ bản");
    course.setTitle("Java");
    course.setTeacher(teacher);
    course.setPrice(BigDecimal.ZERO);
    course.setStatus(CourseStatus.PUBLISHED);
    courses.saveAndFlush(course);
    lesson = lesson("Biến", true, "Biến lưu giá trị. Kiểu int chứa số nguyên.");
    hiddenLesson = lesson("Nháp", false, "Bản nháp");
    var e = new Enrollment();
    e.setStudent(student);
    e.setCourse(course);
    enrollments.saveAndFlush(e);
  }

  @Test
  void tutorAnswersFromLessonContentOnlyForEnrolledLearners() throws Exception {
    when(client.generate(anyString(), anyList(), isNull())).thenReturn("  Biến dùng để lưu giá trị.  ");
    var history = List.of(new AiTutorRequest.Turn(false, "Bài nói gì?"), new AiTutorRequest.Turn(true, "Về biến."));
    var answer = ai.tutor(lesson.getLessonId(), new AiTutorRequest("int là gì?", history), actor(student));
    assertEquals("Biến dùng để lưu giá trị.", answer.answer());
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<AiClient.Turn>> turns = ArgumentCaptor.forClass(List.class);
    verify(client).generate(contains("<lesson>"), turns.capture(), isNull());
    assertTrue(turns.getValue().getFirst().text().contains("Kiểu int chứa số nguyên."));
    assertEquals("model", turns.getValue().get(3).role());
    assertEquals(new AiClient.Turn("user", "int là gì?"), turns.getValue().getLast());
    assertThrows(ForbiddenException.class,
        () -> ai.tutor(lesson.getLessonId(), new AiTutorRequest("?", null), actor(outsider)));
    assertThrows(ResourceNotFoundException.class,
        () -> ai.tutor(hiddenLesson.getLessonId(), new AiTutorRequest("?", null), actor(student)));
    verifyNoMoreInteractions(client);
    mvc.perform(post("/api/lessons/" + lesson.getLessonId() + "/ai/tutor").with(csrf())
        .contentType("application/json").content("{\"question\":\"x\"}")).andExpect(status().isUnauthorized());
    mvc.perform(post("/api/lessons/" + lesson.getLessonId() + "/ai/tutor").with(csrf()).session(login(student))
        .contentType("application/json").content("{\"question\":\"  \"}")).andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Hãy nhập câu hỏi cho trợ giảng AI"));
  }

  @Test
  void tutorIsRateLimitedPerUserAndReportsProviderFailures() {
    when(client.generate(anyString(), anyList(), isNull()))
        .thenThrow(new AiClient.AiUnavailableException("AI đang quá tải"));
    var request = new AiTutorRequest("Hỏi", null);
    for (int i = 0; i < 3; i++) {
      var error = assertThrows(ResponseStatusException.class, () -> ai.tutor(lesson.getLessonId(), request, actor(student)));
      assertEquals(503, error.getStatusCode().value());
      assertEquals("AI đang quá tải", error.getReason());
    }
    var limited = assertThrows(ResponseStatusException.class, () -> ai.tutor(lesson.getLessonId(), request, actor(student)));
    assertEquals(429, limited.getStatusCode().value());
    verify(client, times(3)).generate(anyString(), anyList(), isNull());
  }

  @Test
  void quizDraftKeepsOnlyValidQuestionsAndPreservesTheCorrectAnswer() throws Exception {
    when(client.generate(anyString(), anyList(), notNull())).thenReturn("""
        {"questions":[
          {"prompt":"int chứa gì?","options":["Số nguyên","Chuỗi","Ảnh","Âm thanh"],"correctIndex":0,"explanation":"Theo bài."},
          {"prompt":"Trùng","options":["A","a","B","C"],"correctIndex":0,"explanation":"x"},
          {"prompt":"Thiếu","options":["A","B","C"],"correctIndex":0,"explanation":"x"},
          {"prompt":"Sai chỉ số","options":["A","B","C","D"],"correctIndex":7,"explanation":"x"}]}""");
    var drafted = ai.quizDraft(lesson.getLessonId(), 3, actor(teacher));
    assertEquals(1, drafted.size());
    var q = drafted.getFirst();
    assertEquals("Số nguyên", q.options().get(q.correctIndex()));
    assertEquals(4, q.options().size());
    assertThrows(ForbiddenException.class, () -> ai.quizDraft(lesson.getLessonId(), 3, actor(outsider)));
    assertThrows(BadRequestException.class, () -> ai.quizDraft(lesson.getLessonId(), 11, actor(teacher)));
    mvc.perform(post("/api/lessons/" + lesson.getLessonId() + "/quiz/ai-draft").with(csrf()).session(login(student))
        .contentType("application/json").content("{\"count\":3}")).andExpect(status().isForbidden());
    mvc.perform(post("/api/lessons/" + lesson.getLessonId() + "/quiz/ai-draft").with(csrf()).session(login(teacher))
        .contentType("application/json").content("{\"count\":11}")).andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Số câu cần từ 1 đến 10"));
    when(client.generate(anyString(), anyList(), notNull())).thenReturn("không phải JSON");
    var error = assertThrows(ResponseStatusException.class, () -> ai.quizDraft(lesson.getLessonId(), 3, actor(teacher)));
    assertEquals(502, error.getStatusCode().value());
    lesson.setTextContent(" ");
    lessons.saveAndFlush(lesson);
    assertThrows(BadRequestException.class, () -> ai.quizDraft(lesson.getLessonId(), 3, actor(teacher)));
  }

  @Test
  void flaggedPostsAreHiddenForReviewAndOutagesDoNotBlockLearners() {
    when(client.generate(anyString(), anyList(), notNull()))
        .thenReturn("{\"flagged\":true,\"reason\":\"Quảng cáo\"}");
    var flagged = questionService.ask(lesson.getLessonId(), "Mua khóa giá rẻ tại ...", key(), actor(student));
    assertTrue(flagged.hidden());
    assertTrue(notifications.findAll().stream().anyMatch(n ->
        n.getUser().getUserId().equals(teacher.getUserId()) && n.getMessage().contains("Quảng cáo")));
    when(client.generate(anyString(), anyList(), notNull())).thenReturn("{\"flagged\":false,\"reason\":\"\"}");
    var clean = questionService.ask(lesson.getLessonId(), "int là gì?", key(), actor(student));
    assertFalse(clean.hidden());
    when(client.generate(anyString(), anyList(), notNull()))
        .thenReturn("{\"flagged\":true,\"reason\":\"Spam\"}");
    var reply = questionService.reply(clean.questionId(), "spam", key(), actor(student));
    assertTrue(reply.hidden());
    assertNull(questions.findById(clean.questionId()).orElseThrow().getAnswer());
    reset(client);
    var official = questionService.reply(clean.questionId(), "Trả lời", key(), actor(teacher));
    assertFalse(official.hidden());
    verifyNoInteractions(client);
    when(client.generate(anyString(), anyList(), notNull())).thenThrow(new AiClient.AiUnavailableException("down"));
    assertFalse(questionService.ask(lesson.getLessonId(), "Vẫn gửi được", key(), actor(student)).hidden());
    assertThrows(ForbiddenException.class,
        () -> questionService.ask(lesson.getLessonId(), "x", key(), actor(outsider)));
    verify(client, times(1)).generate(anyString(), anyList(), notNull());
  }

  @Test
  void summariesAreSharedPerLessonVersionAndRespectAccess() {
    when(client.generate(contains("tóm tắt"), anyList(), isNull())).thenReturn("- Ý chính", "- Ý mới");
    var other = user("other", Role.STUDENT);
    var e = new Enrollment();
    e.setStudent(other);
    e.setCourse(course);
    enrollments.saveAndFlush(e);
    assertEquals("- Ý chính", ai.summary(lesson.getLessonId(), actor(student)).answer());
    assertEquals("- Ý chính", ai.summary(lesson.getLessonId(), actor(other)).answer());
    verify(client, times(1)).generate(anyString(), anyList(), isNull());
    lesson.setTextContent("Nội dung đã sửa");
    lesson.setContentRevision(lesson.getContentRevision() + 1);
    lessons.saveAndFlush(lesson);
    assertEquals("- Ý mới", ai.summary(lesson.getLessonId(), actor(student)).answer());
    assertThrows(ForbiddenException.class, () -> ai.summary(lesson.getLessonId(), actor(outsider)));
  }

  @Test
  void gradeSuggestionReadsOnlyTheSubmissionTextAndNeverSavesTheGrade() {
    assignments.save(lesson.getLessonId(), new AssignmentService.Save(0, "Viết hàm", "Viết hàm cộng hai số", true), actor(teacher));
    var submitted = assignments.submit(lesson.getLessonId(), 1, key(), "int add(int a,int b){return a+b;}", null, actor(student));
    when(client.generate(anyString(), anyList(), notNull())).thenReturn("{\"score\":85,\"feedback\":\"Đúng yêu cầu.\"}");
    var suggestion = ai.gradeSuggestion(submitted.submissionId(), actor(teacher));
    assertEquals(new AiAssistantService.GradeSuggestion(85, "Đúng yêu cầu."), suggestion);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<AiClient.Turn>> turns = ArgumentCaptor.forClass(List.class);
    verify(client).generate(anyString(), turns.capture(), notNull());
    assertTrue(turns.getValue().getFirst().text().contains("return a+b;"));
    assertNull(assignments.managedSubmission(submitted.submissionId(), actor(teacher)).score());
    assertThrows(ForbiddenException.class, () -> ai.gradeSuggestion(submitted.submissionId(), actor(user("t2", Role.TEACHER))));
    when(client.generate(anyString(), anyList(), notNull())).thenReturn("{\"score\":140,\"feedback\":\"x\"}");
    var invalid = assertThrows(ResponseStatusException.class, () -> ai.gradeSuggestion(submitted.submissionId(), actor(teacher)));
    assertEquals(502, invalid.getStatusCode().value());
  }

  @Test
  void classInsightsUseCodesInsteadOfNamesAndMapThemBack() {
    when(client.generate(anyString(), anyList(), isNull())).thenReturn("[HV1] cần được nhắc học.");
    var insight = ai.classInsights(course.getCourseId(), actor(teacher));
    assertEquals("student cần được nhắc học.", insight.answer());
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<AiClient.Turn>> turns = ArgumentCaptor.forClass(List.class);
    verify(client).generate(anyString(), turns.capture(), isNull());
    var sent = turns.getValue().getFirst().text();
    assertTrue(sent.contains("[HV1]"));
    assertFalse(sent.contains("student"));
    // BUG-009: a course without a published quiz must not ask the model to chase quiz attempts.
    assertTrue(sent.contains("trong đó có quiz: 0"));
    assertFalse(sent.contains("quiz chưa làm"));
    jdbc.update("INSERT INTO lesson_quiz_versions(lesson_id,revision,title,pass_percentage,is_published) VALUES (?,1,'Quiz',70,TRUE)",
        lesson.getLessonId());
    ai.classInsights(course.getCourseId(), actor(teacher));
    verify(client, times(2)).generate(anyString(), turns.capture(), isNull());
    var withQuiz = turns.getValue().getFirst().text();
    assertTrue(withQuiz.contains("trong đó có quiz: 1"));
    assertTrue(withQuiz.contains("quiz chưa làm"));
    assertThrows(ForbiddenException.class, () -> ai.classInsights(course.getCourseId(), actor(user("t3", Role.TEACHER))));
  }

  @Test
  void recommendationsOnlyReturnOpenCoursesTheLearnerHasNotJoined() throws Exception {
    var other = new Course();
    other.setCategory("Dữ liệu");
    other.setLevel("Cơ bản");
    other.setTitle("SQL");
    other.setTeacher(teacher);
    other.setPrice(BigDecimal.ZERO);
    other.setStatus(CourseStatus.PUBLISHED);
    courses.saveAndFlush(other);
    when(client.generate(anyString(), anyList(), notNull())).thenReturn("{\"courses\":["
        + "{\"courseId\":" + course.getCourseId() + ",\"reason\":\"Đã học\"},"
        + "{\"courseId\":999999,\"reason\":\"Bịa\"},"
        + "{\"courseId\":" + other.getCourseId() + ",\"reason\":\"Hợp mục tiêu dữ liệu\"}]}");
    var result = ai.recommendations("Phân tích dữ liệu", actor(student));
    assertEquals(1, result.size());
    assertEquals(other.getCourseId(), result.getFirst().courseId());
    assertEquals("Hợp mục tiêu dữ liệu", result.getFirst().reason());
    assertThrows(ForbiddenException.class, () -> ai.recommendations(null, actor(teacher)));
    mvc.perform(post("/api/ai/course-recommendations").with(csrf()).session(login(teacher))
        .contentType("application/json").content("{}")).andExpect(status().isForbidden());
  }

  @Test
  void configurationRejectsMissingKeysAndPublishesOnlyTheEnabledFlag() throws Exception {
    assertThrows(IllegalArgumentException.class,
        () -> new AiSettings("gemini", " ", "gemini-3.5-flash-lite", "https://example.com", 30, 10));
    assertThrows(IllegalArgumentException.class,
        () -> new AiSettings("openai", "k", "m", "https://example.com", 30, 10));
    assertThrows(IllegalArgumentException.class,
        () -> new AiSettings("gemini", "k", "m", "http://example.com", 30, 10));
    assertFalse(new AiSettings("disabled", "", "gemini-3.5-flash-lite", "https://example.com", 30, 10).enabled());
    mvc.perform(get("/api/auth/config")).andExpect(status().isOk())
        .andExpect(jsonPath("$.data.ai.enabled").value(true))
        .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("test-key"))));
  }

  String key() {
    return java.util.UUID.randomUUID().toString();
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

  Lesson lesson(String title, boolean published, String text) {
    var l = new Lesson();
    l.setCourse(course);
    l.setTitle(title);
    l.setOrderIndex(lessons.findByCourse_CourseIdOrderByOrderIndex(course.getCourseId()).size() + 1);
    l.setIsPublished(published);
    l.setTextContent(text);
    return lessons.saveAndFlush(l);
  }

  CustomUserDetails actor(User u) {
    return new CustomUserDetails(u);
  }

  MockHttpSession login(User u) throws Exception {
    return (MockHttpSession) mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json")
            .content("{\"username\":\"" + u.getUsername() + "\",\"password\":\"Password123!\"}"))
        .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
  }
}
