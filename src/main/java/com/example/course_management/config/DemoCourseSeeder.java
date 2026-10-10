package com.example.course_management.config;

import com.example.course_management.entity.*;
import com.example.course_management.repository.*;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.LessonQuizService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoCourseSeeder {
  private final DemoAccountSeeder accounts;
  private final CourseRepository courses;
  private final LessonRepository lessons;
  private final LessonQuizService quizzes;

  public DemoCourseSeeder(DemoAccountSeeder accounts, CourseRepository courses,
      LessonRepository lessons, LessonQuizService quizzes) {
    this.accounts = accounts; this.courses = courses; this.lessons = lessons; this.quizzes = quizzes;
  }

  @Transactional
  public Course create(DemoCatalog.CourseSample sample) {
    var teacher = accounts.account(sample.teacher());
    var course = new Course();
    course.setTeacher(teacher); course.setTitle(sample.title()); course.setCategory(sample.category());
    course.setLevel(sample.level()); course.setPrice(sample.price()); course.setDurationHours(sample.durationHours());
    course.setStatus(sample.status()); course.setDescription(sample.description()); course.setLearningOutcomes(sample.learningOutcomes());
    course.setTargetAudience(sample.targetAudience()); course.setPrerequisites(sample.prerequisites());
    courses.saveAndFlush(course);
    Lesson last = null;
    for (var data : sample.lessons()) {
      var lesson = new Lesson();
      lesson.setCourse(course); lesson.setTitle(data.title()); lesson.setOrderIndex(data.orderIndex());
      lesson.setIsPublished(data.published()); lesson.setContentFormat(data.contentFormat());
      lesson.setVideoUrl(data.videoUrl()); lesson.setTextContent(data.textContent());
      last = lessons.saveAndFlush(lesson);
    }
    if (sample.quiz() != null)
      quizzes.save(last.getLessonId(), sample.quiz().request(), new CustomUserDetails(teacher));
    return course;
  }
}
