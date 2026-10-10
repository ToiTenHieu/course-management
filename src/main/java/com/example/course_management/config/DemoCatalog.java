package com.example.course_management.config;

import com.example.course_management.dto.request.SaveQuizRequest;
import com.example.course_management.dto.request.SaveQuizRequest.Question;
import com.example.course_management.entity.*;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Bootstrap data only: live course and lesson content is read from the database. */
@Component
public class DemoCatalog {
  private final Resource resource;
  private final JsonMapper mapper;
  private final Validator validator;
  private Data loaded;

  public DemoCatalog(@Value("classpath:demo/catalog.json") Resource resource, JsonMapper mapper, Validator validator) {
    this.resource = resource; this.mapper = mapper; this.validator = validator;
  }

  public synchronized Data data() {
    if (loaded != null) return loaded;
    try (var input = resource.getInputStream()) {
      var data = mapper.readValue(input, Data.class);
      var errors = validator.validate(data);
      if (!errors.isEmpty()) throw new IllegalArgumentException(errors.toString());
      if (data.accounts().stream().map(Account::key).distinct().count() != data.accounts().size()
          || data.accounts().stream().map(Account::username).distinct().count() != data.accounts().size())
        throw new IllegalArgumentException("Duplicate demo account key or username");
      var s = data.scenario();
      if (s.enrolledCourses() > s.pendingCoursesEnd() || s.pendingCoursesEnd() > data.scenarioCourses().size()
          || s.sharedCourseIndex() >= data.scenarioCourses().size() || s.paymentCourseIndex() >= data.scenarioCourses().size()
          || s.activeStudents() > s.studentCount() || s.fullyCompletedCourses() > s.enrolledCourses())
        throw new IllegalArgumentException("Demo scenario ranges exceed the configured dataset");
      loaded = data;
      return loaded;
    } catch (Exception error) {
      throw new IllegalStateException("Cannot read demo catalog " + resource.getDescription(), error);
    }
  }

  public Account account(String key) {
    return data().accounts().stream().filter(a -> a.key().equals(key)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Missing demo account: " + key));
  }

  public record Data(@NotEmpty List<@Valid Account> accounts, @NotEmpty List<@Valid CourseSample> baseCourses,
      @NotNull @Valid JavaQuiz javaQuiz, @NotEmpty List<@Valid CourseSample> youtubeCourses,
      @NotEmpty List<@Valid CourseSample> scenarioCourses, @NotNull @Valid Scenario scenario,
      @NotEmpty List<@Valid CourseFitSample> courseFitDefaults) {}
  public record Account(@NotBlank String key, @NotBlank String username, @NotBlank String fullName,
      @NotNull Role role, boolean quickLogin, @Size(max = 10000) String biography,
      @Size(max = 2000) String expertise) {}
  public record CourseSample(@NotBlank String title, @NotBlank String category, @NotBlank String level,
      @NotNull @DecimalMin("0") BigDecimal price, @Positive int durationHours, @NotBlank String teacher,
      @NotNull CourseStatus status, @NotBlank String description, @NotBlank String learningOutcomes,
      @NotBlank @Size(max = 10000) String targetAudience, @NotBlank @Size(max = 10000) String prerequisites,
      @NotEmpty List<@Valid LessonSample> lessons, @Valid Quiz quiz) {}
  public record CourseFitSample(@NotNull String category,
      @NotBlank @Size(max = 10000) String targetAudience, @NotBlank @Size(max = 10000) String prerequisites) {}
  public record LessonSample(@NotBlank String title, @NotBlank String textContent, @Positive int orderIndex,
      String videoUrl, boolean published, @NotNull @Pattern(regexp = "TEXT|MARKDOWN") String contentFormat) {}
  public record Quiz(@NotBlank String title, @Min(1) @Max(100) int passPercentage,
      @NotEmpty List<@Valid Question> questions) {
    public SaveQuizRequest request() { return new SaveQuizRequest(0, title, passPercentage, true, questions); }
  }
  public record JavaQuiz(@NotBlank String teacher, @NotBlank String courseTitle, @NotBlank String lessonTitle,
      @NotBlank String title, @Min(1) @Max(100) int passPercentage,
      @NotEmpty List<@Valid Question> questions) {
    public SaveQuizRequest request() { return new SaveQuizRequest(0, title, passPercentage, true, questions); }
  }
  public record Scenario(@Positive int enrolledCourses, @Positive int pendingCoursesEnd,
      @PositiveOrZero int fullyCompletedCourses, @PositiveOrZero int sharedCourseIndex,
      @PositiveOrZero int paymentCourseIndex, @Positive int studentCount, @PositiveOrZero int activeStudents,
      @Positive int notificationsPerUser, @NotBlank String studentUsernamePrefix, @NotBlank String studentNamePrefix,
      @NotBlank String note, @NotBlank String review, @NotBlank String question, @NotBlank String answer,
      @NotBlank String notification, @NotBlank String notificationType, @NotBlank String notificationTarget,
      @NotNull @Valid Quiz quiz) {}
}
