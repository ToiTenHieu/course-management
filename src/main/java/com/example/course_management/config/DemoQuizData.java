package com.example.course_management.config;

import com.example.course_management.repository.*;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.LessonQuizService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(1)
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
public class DemoQuizData implements CommandLineRunner {
  private final UserRepository users;
  private final CourseRepository courses;
  private final LessonRepository lessons;
  private final LessonQuizRepository quizzes;
  private final LessonQuizService service;
  private final DemoCatalog catalog;

  public DemoQuizData(UserRepository users, CourseRepository courses, LessonRepository lessons,
      LessonQuizRepository quizzes, LessonQuizService service, DemoCatalog catalog) {
    this.users = users; this.courses = courses; this.lessons = lessons;
    this.quizzes = quizzes; this.service = service; this.catalog = catalog;
  }

  @Override
  @Transactional
  public void run(String... args) {
    var sample = catalog.data().javaQuiz();
    var teacher = users.findByUsername(catalog.account(sample.teacher()).username()).orElse(null);
    if (teacher == null) return;
    var course = courses.findByTeacher_UserId(teacher.getUserId()).stream()
        .filter(c -> sample.courseTitle().equals(c.getTitle())).findFirst().orElse(null);
    if (course == null) return;
    var lesson = lessons.findByCourse_CourseIdAndIsPublishedTrueOrderByOrderIndex(course.getCourseId()).stream()
        .filter(l -> sample.lessonTitle().equals(l.getTitle())).findFirst().orElse(null);
    if (lesson == null || quizzes.findFirstByLesson_LessonIdOrderByRevisionDesc(lesson.getLessonId()).isPresent()) return;
    service.save(lesson.getLessonId(), sample.request(), new CustomUserDetails(teacher));
  }
}
