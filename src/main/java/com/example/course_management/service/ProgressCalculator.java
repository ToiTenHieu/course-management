package com.example.course_management.service;

import com.example.course_management.time.ApplicationTime;
import com.example.course_management.entity.*;
import com.example.course_management.repository.*;
import java.math.*;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ProgressCalculator {
  private final LessonRepository lessons;
  private final LessonProgressRepository progress;
  private final EnrollmentRepository enrollments;

  public ProgressCalculator(
      LessonRepository lessons,
      LessonProgressRepository progress,
      EnrollmentRepository enrollments) {
    this.lessons = lessons;
    this.progress = progress;
    this.enrollments = enrollments;
  }

  public void recalculate(Enrollment enrollment) {
    if (enrollment.getStatus() == EnrollmentStatus.DROPPED) return;
    var published =
        lessons.findByCourse_CourseIdAndIsPublishedTrueOrderByOrderIndex(
            enrollment.getCourse().getCourseId());
    var ids = published.stream().map(Lesson::getLessonId).collect(Collectors.toSet());
    long done =
        progress.findByEnrollment_EnrollmentId(enrollment.getEnrollmentId()).stream()
            .filter(
                p ->
                    Boolean.TRUE.equals(p.getIsCompleted())
                        && ids.contains(p.getLesson().getLessonId()))
            .map(p -> p.getLesson().getLessonId())
            .distinct()
            .count();
    var percentage =
        ids.isEmpty()
            ? BigDecimal.ZERO
            : BigDecimal.valueOf(done * 100)
                .divide(BigDecimal.valueOf(ids.size()), 2, RoundingMode.HALF_UP);
    enrollment.setProgressPercentage(percentage);
    if (!ids.isEmpty() && done == ids.size()) {
      enrollment.setStatus(EnrollmentStatus.COMPLETED);
      if (enrollment.getCompletionDate() == null) enrollment.setCompletionDate(ApplicationTime.now());
    } else {
      enrollment.setStatus(EnrollmentStatus.ENROLLED);
      enrollment.setCompletionDate(null);
    }
    enrollments.save(enrollment);
  }

  public void recalculateCourse(Integer courseId) {
    enrollments.findByCourse_CourseId(courseId).stream()
        .sorted(java.util.Comparator.comparing(Enrollment::getEnrollmentId))
        .forEach(e -> enrollments.findLockedById(e.getEnrollmentId()).ifPresent(this::recalculate));
  }
}
