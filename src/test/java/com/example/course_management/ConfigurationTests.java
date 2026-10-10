package com.example.course_management;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.course_management.config.*;
import com.example.course_management.dto.request.*;
import com.example.course_management.entity.*;
import com.example.course_management.exception.BadRequestException;
import com.example.course_management.repository.*;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:configuration_tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password="
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ConfigurationTests {
  @Autowired MockMvc mvc;
  @Autowired LearningSettings settings;
  @Autowired JsonMapper mapper;
  @Autowired UserRepository users;
  @Autowired CourseRepository courses;
  @Autowired LessonRepository lessons;
  @Autowired LessonResourceService resources;
  @Autowired LessonQuizService quizzes;
  @Autowired PaymentService payments;
  @Autowired CourseService courseService;
  @Autowired PasswordEncoder encoder;
  @Autowired JdbcTemplate jdbc;
  @Autowired DemoAccountSeeder demoAccounts;
  @Autowired DemoYouTubeSeeder youtube;
  @Autowired DemoCatalog catalog;
  @Autowired DemoCourseSeeder courseSeeder;
  @Autowired DemoProfileSeeder demoProfiles;

  private User account(Role role) {
    var value = new User(); value.setUsername("config_" + UUID.randomUUID());
    value.setFullName("Configuration " + role); value.setEmail(value.getUsername() + "@example.invalid");
    value.setRole(role); value.setPasswordHash(encoder.encode("Password123!"));
    return users.saveAndFlush(value);
  }

  private SaveSettingsRequest changed(long bytes, int resourceCount, int questions) {
    return new SaveSettingsRequest(settings.current().revision(), bytes, resourceCount, questions,
        60, "Chủ đề từ database", "Trình độ từ database", "Database Bank", "DB-123", "DATABASE HOLDER", "");
  }

  @Test
  void adminSettingsPersistAndProtectPermissionCsrfValidationAndConcurrentUpdates() throws Exception {
    var admin = new CustomUserDetails(account(Role.ADMIN));
    var teacher = new CustomUserDetails(account(Role.TEACHER));
    var value = changed(1024, 1, 2);
    String body = mapper.writeValueAsString(value);
    mvc.perform(get("/api/settings")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/settings").with(user(teacher))).andExpect(status().isForbidden());
    mvc.perform(put("/api/settings").with(user(teacher)).with(csrf()).contentType("application/json").content(body))
        .andExpect(status().isForbidden());
    mvc.perform(put("/api/settings").with(user(admin)).contentType("application/json").content(body))
        .andExpect(status().isForbidden());
    mvc.perform(put("/api/settings").with(user(admin)).with(csrf()).contentType("application/json")
        .content(body.replace("\"maxQuizQuestions\":2", "\"maxQuizQuestions\":0")))
        .andExpect(status().isBadRequest());
    mvc.perform(put("/api/settings").with(user(admin)).with(csrf()).contentType("application/json")
        .content(mapper.writeValueAsString(changed(settings.uploadCeilingBytes() + 1, 1, 2))))
        .andExpect(status().isBadRequest());
    assertEquals(0, settings.current().revision());
    mvc.perform(put("/api/settings").with(user(admin)).with(csrf()).contentType("application/json").content(body))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data.revision").value(1));
    mvc.perform(put("/api/settings").with(user(admin)).with(csrf()).contentType("application/json").content(body))
        .andExpect(status().isConflict());
    mvc.perform(get("/api/settings").with(user(admin))).andExpect(status().isOk())
        .andExpect(jsonPath("$.data.values.defaultCategory").value("Chủ đề từ database"));
    mvc.perform(get("/api/auth/config")).andExpect(status().isOk())
        .andExpect(jsonPath("$.data.demo").value(false))
        .andExpect(jsonPath("$.data.demoAccounts").doesNotExist())
        .andExpect(jsonPath("$.data.learning.maxFileBytes").value(1024))
        .andExpect(jsonPath("$.data.learning.maxResourcesPerLesson").value(1))
        .andExpect(jsonPath("$.data.learning.maxQuizQuestions").value(2))
        .andExpect(jsonPath("$.data.learning.defaultPassPercentage").value(60))
        .andExpect(jsonPath("$.data.learning.bankAccount").doesNotExist());
    assertEquals("Database Bank", payments.getBankInfo().getBankName());
    var freshService = new LearningSettings(jdbc, DataSize.ofMegabytes(20), DataSize.ofMegabytes(21));
    assertEquals(settings.current(), freshService.current());
    assertEquals("DB-123", jdbc.queryForObject("SELECT bank_account FROM application_settings WHERE id=1", String.class));
    var request = new CreateCourseRequest(); request.setTitle("Database defaults");
    request.setTeacherId(teacher.getUser().getUserId()); request.setDurationHours(1);
    var created = courseService.createCourse(request);
    assertEquals("Chủ đề từ database", created.getCategory());
    assertEquals("Trình độ từ database", created.getLevel());
  }

  @Test
  void bankQrConfigurationValidatesAndIsAvailableOnlyAfterSaving() throws Exception {
    var admin = new CustomUserDetails(account(Role.ADMIN));
    var student = new CustomUserDetails(account(Role.STUDENT));
    var original = changed(1024, 1, 2);
    var body = mapper.writeValueAsString(original);
    assertEquals("", payments.getBankInfo().getBankBin());
    // Legacy settings clients can omit the new optional field.
    mvc.perform(put("/api/settings").with(user(admin)).with(csrf()).contentType("application/json")
        .content(body.replace(",\"bankBin\":\"\"", ""))).andExpect(status().isOk());
    var configured = new SaveSettingsRequest(settings.current().revision(), 1024L, 1, 2,
        60, "Lập trình", "Cơ bản", "VietinBank", "113366668888", "TEST HOLDER", "970415");
    String configuredBody = mapper.writeValueAsString(configured);
    mvc.perform(put("/api/settings").with(user(admin)).with(csrf()).contentType("application/json")
        .content(configuredBody.replace("970415", "97041"))).andExpect(status().isBadRequest());
    mvc.perform(put("/api/settings").with(user(admin)).with(csrf()).contentType("application/json")
        .content(configuredBody.replace("113366668888", "BAD/ACCOUNT"))).andExpect(status().isBadRequest());
    assertEquals("", payments.getBankInfo().getBankBin());
    mvc.perform(put("/api/settings").with(user(admin)).with(csrf()).contentType("application/json")
        .content(configuredBody)).andExpect(status().isOk())
        .andExpect(jsonPath("$.data.bankBin").value("970415"));
    mvc.perform(get("/api/payments/bank-info")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/payments/bank-info").with(user(student))).andExpect(status().isOk())
        .andExpect(jsonPath("$.data.bankBin").value("970415"))
        .andExpect(jsonPath("$.data.accountNumber").value("113366668888"));
    var freshService = new LearningSettings(jdbc, DataSize.ofMegabytes(20), DataSize.ofMegabytes(21));
    assertEquals("970415", freshService.current().bankBin());
    mvc.perform(get("/api/auth/config")).andExpect(status().isOk())
        .andExpect(jsonPath("$.data.learning.bankBin").doesNotExist());
    settings.save(changed(1024, 1, 2));
    assertEquals("", payments.getBankInfo().getBankBin());
  }

  @Test
  void databaseLimitsApplyToUploadAndQuizWithoutRecreatingServices() {
    settings.save(changed(1024, 1, 2));
    var teacher = account(Role.TEACHER);
    var course = new Course(); course.setTeacher(teacher); course.setTitle("Config limits");
    course.setCategory("Test category"); course.setLevel("Test level");
    course.setPrice(BigDecimal.ZERO); course.setStatus(CourseStatus.PUBLISHED); courses.saveAndFlush(course);
    var lesson = new Lesson(); lesson.setCourse(course); lesson.setTitle("Limits");
    lesson.setOrderIndex(1); lesson.setIsPublished(true); lessons.saveAndFlush(lesson);
    var actor = new CustomUserDetails(teacher);
    assertThrows(BadRequestException.class, () -> resources.upload(lesson.getLessonId(),
        new MockMultipartFile("file", "large.pdf", "application/pdf", new byte[1025]), actor));
    var file = new MockMultipartFile("file", "small.pdf", "application/pdf", "%PDF-1.7".getBytes(StandardCharsets.US_ASCII));
    assertEquals(1, resources.upload(lesson.getLessonId(), file, actor).size());
    assertThrows(BadRequestException.class, () -> resources.upload(lesson.getLessonId(), file, actor));
    var question = new SaveQuizRequest.Question("Pick A", List.of("A", "B", "C", "D"), 0, "A is correct");
    var quiz = new SaveQuizRequest(0, "Three questions", 60, true, List.of(question, question, question));
    assertThrows(BadRequestException.class, () -> quizzes.save(lesson.getLessonId(), quiz, actor));
    settings.save(changed(2048, 2, 3));
    assertEquals(2, resources.upload(lesson.getLessonId(), file, actor).size());
    assertEquals(3, quizzes.save(lesson.getLessonId(), quiz, actor).questions().size());
  }

  @Test
  void newDemoAccountsAndCoursesIncludePublicProfilesAndPreparation() {
    var teacher = demoAccounts.account("teacher");
    assertEquals(catalog.account("teacher").biography(), teacher.getBiography());
    assertEquals(catalog.account("teacher").expertise(), teacher.getExpertise());
    var sample = catalog.data().baseCourses().getFirst();
    var course = courseSeeder.create(sample);
    assertEquals(sample.targetAudience(), course.getTargetAudience());
    assertEquals(sample.prerequisites(), course.getPrerequisites());
    teacher.setBiography("Edited introduction");
    teacher.setExpertise("");
    users.saveAndFlush(teacher);
    assertEquals("Edited introduction", demoAccounts.account("teacher").getBiography());
    assertEquals("", demoAccounts.account("teacher").getExpertise());
  }

  @Test
  void demoProfileEnrichmentFillsExistingBlanksOnceAndPreservesEditsAndUnrelatedRecords() {
    var teacher = demoAccounts.account("teacher_demo", "Existing teacher name", Role.TEACHER);
    teacher.setBiography("Personal introduction"); users.saveAndFlush(teacher);
    var sample = catalog.data().baseCourses().getFirst();
    var old = new Course(); old.setTitle(sample.title()); old.setTeacher(teacher);
    old.setCategory(sample.category()); old.setLevel(sample.level()); old.setStatus(CourseStatus.PUBLISHED); courses.saveAndFlush(old);
    var edited = new Course(); edited.setTitle("Khóa nháp E2E hiện có"); edited.setTeacher(teacher);
    edited.setCategory("Công nghệ"); edited.setLevel("Cơ bản"); edited.setTargetAudience("Audience entered by instructor");
    edited.setPrerequisites("   "); courses.saveAndFlush(edited);
    var unrelatedTeacher = account(Role.TEACHER);
    var unrelated = new Course(); unrelated.setTitle(sample.title()); unrelated.setTeacher(unrelatedTeacher);
    unrelated.setCategory(sample.category()); unrelated.setLevel(sample.level());
    courses.saveAndFlush(unrelated);
    long userCount = users.count(), courseCount = courses.count(), lessonCount = lessons.count();
    demoProfiles.seed();
    assertEquals("Personal introduction", teacher.getBiography());
    assertEquals("Existing teacher name", teacher.getFullName());
    assertEquals(catalog.account("teacher").expertise(), teacher.getExpertise());
    assertEquals(sample.targetAudience(), old.getTargetAudience());
    assertEquals(sample.prerequisites(), old.getPrerequisites());
    assertEquals("Audience entered by instructor", edited.getTargetAudience());
    assertFalse(edited.getPrerequisites().isBlank());
    assertNull(unrelated.getTargetAudience()); assertNull(unrelated.getPrerequisites());
    assertNull(unrelatedTeacher.getBiography());
    old.setTargetAudience(""); teacher.setExpertise("");
    courses.saveAndFlush(old); users.saveAndFlush(teacher);
    courses.delete(edited); courses.flush();
    demoProfiles.seed();
    assertEquals("", old.getTargetAudience()); assertEquals("", teacher.getExpertise());
    assertEquals(userCount, users.count()); assertEquals(courseCount - 1, courses.count());
    assertEquals(lessonCount, lessons.count());
    assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM demo_seed_runs WHERE dataset_key = 'demo-public-profiles-and-course-fit-v1'", Integer.class));
  }

  @Test
  void demoProfileEnrichmentDoesNotCreateMissingAccountsOrChangeTheirRoles() {
    long count = users.count();
    demoProfiles.seed();
    assertEquals(count, users.count());
    assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM demo_seed_runs WHERE dataset_key = 'demo-public-profiles-and-course-fit-v1'", Integer.class));
    var student = demoAccounts.account("teacher_demo", "Existing student", Role.STUDENT);
    demoProfiles.seed();
    assertEquals(Role.STUDENT, student.getRole()); assertNull(student.getBiography());
    assertEquals(count + 1, users.count());
    assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM demo_seed_runs WHERE dataset_key = 'demo-public-profiles-and-course-fit-v1'", Integer.class));
  }

  @Test
  void demoBootstrapPersistsContentAndLoginAccountsWithoutOverwritingDatabaseEdits() {
    demoAccounts.initializeLoginAccounts();
    demoAccounts.initializeLoginAccounts();
    assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM demo_login_accounts", Integer.class));
    youtube.seed();
    var course = courses.findAll().stream().filter(c -> c.getTitle().contains("HTML nhập môn")).findFirst().orElseThrow();
    var lesson = lessons.findByCourse_CourseIdOrderByOrderIndex(course.getCourseId()).getFirst();
    course.setTitle("Nội dung đã sửa trong database"); courses.saveAndFlush(course);
    lesson.setVideoUrl("https://www.youtube.com/watch?v=abcdefghijk&t=42"); lessons.saveAndFlush(lesson);
    var teacher = users.findById(course.getTeacher().getUserId()).orElseThrow();
    teacher.setUsername("renamed_demo_teacher"); users.saveAndFlush(teacher);
    assertTrue(demoAccounts.loginAccounts().stream().anyMatch(a -> a.username().equals("renamed_demo_teacher")));
    teacher.setPasswordHash(encoder.encode("Changed123!")); users.saveAndFlush(teacher);
    assertFalse(demoAccounts.loginAccounts().stream().anyMatch(a -> a.username().equals("renamed_demo_teacher")));
    long count = courses.count();
    var base = new DemoData(catalog, demoAccounts, courseSeeder, courses, jdbc);
    base.run(); base.run(); youtube.seed();
    assertEquals(count, courses.count());
    assertEquals("Nội dung đã sửa trong database", courses.findById(course.getCourseId()).orElseThrow().getTitle());
    assertEquals("https://www.youtube.com/watch?v=abcdefghijk&t=42", lessons.findById(lesson.getLessonId()).orElseThrow().getVideoUrl());
    assertTrue(encoder.matches("Changed123!", users.findById(teacher.getUserId()).orElseThrow().getPasswordHash()));
  }
}
