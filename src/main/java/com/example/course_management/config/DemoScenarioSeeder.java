package com.example.course_management.config;

import com.example.course_management.dto.request.*;
import com.example.course_management.entity.*;
import com.example.course_management.repository.*;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.*;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Repeatable role scenarios, recorded once in the demo database. */
@Component
public class DemoScenarioSeeder {
  private static final String KEY = "role-scenarios-v1";
  private final JdbcTemplate jdbc;
  private final UserRepository users;
  private final CourseRepository courses;
  private final LessonRepository lessons;
  private final EnrollmentRepository enrollments;
  private final NotificationRepository notifications;
  private final DemoCatalog catalog;
  private final DemoAccountSeeder accounts;
  private final DemoCourseSeeder courseSeeder;
  private final PaymentService payments;
  private final EnrollmentService learning;
  private final ReviewService reviews;
  private final LessonQuestionService questions;
  private final LessonQuizService quizzes;

  public DemoScenarioSeeder(JdbcTemplate jdbc, UserRepository users, CourseRepository courses,
      LessonRepository lessons, EnrollmentRepository enrollments, NotificationRepository notifications,
      DemoCatalog catalog, DemoAccountSeeder accounts, DemoCourseSeeder courseSeeder,
      PaymentService payments, EnrollmentService learning,
      ReviewService reviews, LessonQuestionService questions, LessonQuizService quizzes) {
    this.jdbc = jdbc; this.users = users; this.courses = courses; this.lessons = lessons;
    this.enrollments = enrollments; this.notifications = notifications; this.catalog = catalog; this.accounts = accounts; this.courseSeeder = courseSeeder;
    this.payments = payments; this.learning = learning; this.reviews = reviews;
    this.questions = questions; this.quizzes = quizzes;
  }

  @Transactional
  public void seed() {
    if (jdbc.queryForObject("SELECT COUNT(*) FROM demo_seed_runs WHERE dataset_key = ?",
        Integer.class, KEY) > 0) return;
    var scenario = catalog.data().scenario();
    var admin = accounts.account("admin");
    var teacher = accounts.account("teacher");
    var otherTeacher = accounts.account("otherTeacher");
    var student = accounts.account("student");
    var samples = new ArrayList<Course>();
    for (var sample : catalog.data().scenarioCourses()) samples.add(courseSeeder.create(sample));
    for (int i = 0; i < scenario.enrolledCourses(); i++) {
      var c = samples.get(i); var e = enroll(student, c, admin);
      var visible = lessons.findByCourse_CourseIdAndIsPublishedTrueOrderByOrderIndex(c.getCourseId());
      for (int j = 0; j < (i < scenario.fullyCompletedCourses() ? visible.size() : i % visible.size()); j++)
        learning.completeLesson(e.getEnrollmentId(), visible.get(j).getLessonId(), actor(student));
      learning.saveNote(e.getEnrollmentId(), visible.getFirst().getLessonId(), scenario.note(), actor(student));
    }
    for (int i = scenario.enrolledCourses(); i < scenario.pendingCoursesEnd(); i++) {
      var c = samples.get(i);
      if (c.getPrice().signum() == 0) continue;
      var p = payments.createPayment(c.getCourseId(), actor(student));
      if (i % 2 == 0) payments.rejectPayment(p.getPaymentId(), actor(admin));
    }
    var shared = samples.get(scenario.sharedCourseIndex());
    var lesson = lessons.findByCourse_CourseIdAndIsPublishedTrueOrderByOrderIndex(shared.getCourseId()).getFirst();
    var quiz = quizzes.save(lesson.getLessonId(), scenario.quiz().request(), actor(shared.getTeacher()));
    for (int i = 1; i <= scenario.studentCount(); i++) {
      var u = accounts.account(scenario.studentUsernamePrefix() + i, scenario.studentNamePrefix() + String.format("%02d", i), Role.STUDENT);
      enroll(u, shared, admin);
      var p = payments.createPayment(samples.get(scenario.paymentCourseIndex()).getCourseId(), actor(u));
      if (i % 3 == 0) payments.confirmPayment(p.getPaymentId(), actor(admin));
      else if (i % 3 == 1) payments.rejectPayment(p.getPaymentId(), actor(admin));
      var review = new ReviewRequest(); review.setRating(3 + i % 3);
      review.setComment(scenario.review().formatted(i));
      reviews.create(shared.getCourseId(), review, actor(u));
      var q = questions.ask(lesson.getLessonId(), scenario.question().formatted(i), actor(u));
      if (i % 3 == 0) questions.answer(q.questionId(), scenario.answer(), actor(shared.getTeacher()));
      if (i % 7 == 0) questions.visibility(q.questionId(), true, actor(shared.getTeacher()));
      quizzes.submit(lesson.getLessonId(), new SubmitQuizRequest(quiz.quizVersionId(), java.util.UUID.randomUUID().toString(), List.of(i % 2)), actor(u));
      if (i > scenario.activeStudents()) { u.setIsActive(false); users.save(u); }
    }
    for (var u : List.of(admin, teacher, otherTeacher, student)) {
      for (int i = 1; i <= scenario.notificationsPerUser(); i++) {
        var n = new Notification(); n.setUser(u); n.setMessage(scenario.notification().formatted(i));
        n.setType(scenario.notificationType()); n.setIsRead(i % 3 == 0); n.setTargetUrl(scenario.notificationTarget()); notifications.save(n);
      }
    }
    jdbc.update("INSERT INTO demo_seed_runs (dataset_key, completed_at) VALUES (?, ?)", KEY, com.example.course_management.time.ApplicationTime.now());
  }

  private Enrollment enroll(User student, Course course, User admin) {
    if (course.getPrice().signum() == 0) learning.enroll(course.getCourseId(), actor(student));
    else {
      var p = payments.createPayment(course.getCourseId(), actor(student));
      payments.confirmPayment(p.getPaymentId(), actor(admin));
    }
    return enrollments.findByStudent_UserIdAndCourse_CourseId(student.getUserId(), course.getCourseId()).orElseThrow();
  }

  private CustomUserDetails actor(User u) { return new CustomUserDetails(u); }
}
