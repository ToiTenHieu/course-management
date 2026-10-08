package com.example.course_management.service;

import com.example.course_management.entity.*;
import com.example.course_management.exception.*;
import com.example.course_management.repository.EnrollmentRepository;
import com.example.course_management.security.CustomUserDetails;
import java.net.URI;
import org.springframework.stereotype.Component;

@Component
public class ContentPolicy {
  private final EnrollmentRepository enrollments;

  public ContentPolicy(EnrollmentRepository enrollments) {
    this.enrollments = enrollments;
  }

  public boolean manages(Course course, CustomUserDetails actor) {
    return actor.getUser().getRole() == Role.ADMIN
        || (actor.getUser().getRole() == Role.TEACHER
            && course.getTeacher().getUserId().equals(actor.getUser().getUserId()));
  }

  public boolean enrolled(Course course, CustomUserDetails actor) {
    return enrollments
        .findByStudent_UserIdAndCourse_CourseId(actor.getUser().getUserId(), course.getCourseId())
        .filter(e -> e.getStatus() != EnrollmentStatus.DROPPED)
        .isPresent();
  }

  public void visible(Course course, CustomUserDetails actor) {
    if (!manages(course, actor)
        && course.getStatus() != CourseStatus.PUBLISHED
        && !enrolled(course, actor)) throw new ResourceNotFoundException("Không tìm thấy khóa học");
  }

  public void manager(Course course, CustomUserDetails actor) {
    if (!manages(course, actor)) throw new ForbiddenException("Bạn không phụ trách khóa học này");
  }

  public void contentUrl(String value) {
    if (value == null || value.isBlank()) return;
    try {
      var uri = URI.create(value);
      if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
          || uri.getHost() == null) throw new IllegalArgumentException();
    } catch (IllegalArgumentException ex) {
      throw new BadRequestException("Liên kết bài học phải là URL http hoặc https hợp lệ");
    }
  }

  public void targetUrl(String value) {
    if (value == null || value.isBlank()) return;
    try {
      var uri = URI.create(value);
      if (uri.isAbsolute() || uri.getRawAuthority() != null
          || !uri.getRawPath().matches("/[a-zA-Z0-9_.-]+\\.html")
          || value.contains("\\") || value.chars().anyMatch(Character::isISOControl)
          || (uri.getRawFragment() != null && !uri.getRawFragment().matches("[a-zA-Z0-9_-]+")))
        throw new IllegalArgumentException();
    } catch (IllegalArgumentException ex) {
      throw new BadRequestException("Liên kết thông báo phải trỏ tới một trang trong hệ thống");
    }
  }
}
