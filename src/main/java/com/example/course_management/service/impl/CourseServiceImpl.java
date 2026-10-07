package com.example.course_management.service.impl;

import com.example.course_management.dto.request.*;
import com.example.course_management.dto.response.*;
import com.example.course_management.entity.*;
import com.example.course_management.exception.*;
import com.example.course_management.repository.*;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CourseServiceImpl implements CourseService {
  private final CourseRepository courses;
  private final UserRepository users;
  private final LessonRepository lessons;
  private final EnrollmentRepository enrollments;
  private final ReviewRepository reviews;
  private final ContentPolicy policy;

  public CourseServiceImpl(
      CourseRepository courses,
      UserRepository users,
      LessonRepository lessons,
      EnrollmentRepository enrollments,
      ReviewRepository reviews,
      ContentPolicy policy) {
    this.courses = courses;
    this.users = users;
    this.lessons = lessons;
    this.enrollments = enrollments;
    this.reviews = reviews;
    this.policy = policy;
  }

  public List<CourseResponse> getCourses(
      String search, Integer teacherId, CourseStatus status, CustomUserDetails actor) {
    boolean admin = actor.getUser().getRole() == Role.ADMIN;
    var found =
        courses.search(admin ? status : null, teacherId, search == null ? "" : search.trim());
    return found.stream()
        .filter(c -> admin || c.getStatus() == CourseStatus.PUBLISHED || policy.manages(c, actor))
        .filter(c -> admin || status == null || c.getStatus() == status)
        .sorted(Comparator.comparing(Course::getCourseId).reversed())
        .map(this::toResponse)
        .toList();
  }

  public CourseResponse getCourseById(Integer id, CustomUserDetails actor) {
    var course = find(id);
    policy.visible(course, actor);
    var response = toResponse(course);
    var items =
        policy.manages(course, actor)
            ? lessons.findByCourse_CourseIdOrderByOrderIndex(id)
            : lessons.findByCourse_CourseIdAndIsPublishedTrueOrderByOrderIndex(id);
    response.setLessons(
        items.stream()
            .map(
                l ->
                    LessonSummaryResponse.builder()
                        .lessonId(l.getLessonId())
                        .title(l.getTitle())
                        .orderIndex(l.getOrderIndex())
                        .build())
            .toList());
    return response;
  }

  public CourseResponse createCourse(CreateCourseRequest r) {
    var c = new Course();
    c.setTeacher(teacher(r.getTeacherId()));
    c.setTitle(r.getTitle().trim());
    c.setDescription(r.getDescription());
    c.setPrice(r.getPrice() == null ? BigDecimal.ZERO : r.getPrice());
    c.setDurationHours(r.getDurationHours());
    c.setCategory(value(r.getCategory(), "Lập trình"));
    c.setLevel(value(r.getLevel(), "Cơ bản"));
    c.setLearningOutcomes(r.getLearningOutcomes());
    return toResponse(courses.save(c));
  }

  public CourseResponse updateCourse(Integer id, UpdateCourseRequest r) {
    var c =
        courses
            .findLockedById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học"));
    c.setTeacher(teacher(r.getTeacherId()));
    c.setTitle(r.getTitle().trim());
    c.setDescription(r.getDescription());
    if (r.getPrice() != null) c.setPrice(r.getPrice());
    c.setDurationHours(r.getDurationHours());
    c.setCategory(value(r.getCategory(), c.getCategory()));
    c.setLevel(value(r.getLevel(), c.getLevel()));
    c.setLearningOutcomes(r.getLearningOutcomes());
    c.setUpdatedAt(LocalDateTime.now());
    return toResponse(courses.save(c));
  }

  public CourseResponse updateStatus(Integer id, UpdateCourseStatusRequest r) {
    var c =
        courses
            .findLockedById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học"));
    if (r.getStatus() == CourseStatus.PUBLISHED
        && lessons.findByCourse_CourseIdAndIsPublishedTrueOrderByOrderIndex(id).isEmpty())
      throw new BadRequestException("Cần ít nhất một bài học đã xuất bản trước khi mở khóa học");
    c.setStatus(r.getStatus());
    c.setUpdatedAt(LocalDateTime.now());
    return toResponse(courses.save(c));
  }

  public void deleteCourse(Integer id) {
    var c = find(id);
    if (lessons.countByCourse_CourseId(id) > 0 || enrollments.countByCourse_CourseId(id) > 0)
      throw new ConflictException(
          "Khóa học có bài học hoặc học viên. Hãy chuyển sang trạng thái lưu trữ");
    courses.delete(c);
    courses.flush();
  }

  private User teacher(Integer id) {
    var u =
        users
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giảng viên"));
    if (u.getRole() != Role.TEACHER || !Boolean.TRUE.equals(u.getIsActive()))
      throw new BadRequestException("Giảng viên phải đang hoạt động");
    return u;
  }

  private Course find(Integer id) {
    return courses
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học"));
  }

  private String value(String s, String fallback) {
    return s == null || s.isBlank() ? fallback : s.trim();
  }

  private CourseResponse toResponse(Course c) {
    return CourseResponse.builder()
        .courseId(c.getCourseId())
        .title(c.getTitle())
        .description(c.getDescription())
        .teacherId(c.getTeacher().getUserId())
        .teacherName(c.getTeacher().getFullName())
        .price(c.getPrice())
        .durationHours(c.getDurationHours())
        .status(c.getStatus().name())
        .createdAt(c.getCreatedAt())
        .category(c.getCategory())
        .level(c.getLevel())
        .learningOutcomes(c.getLearningOutcomes())
        .lessonCount(
            (long)
                lessons
                    .findByCourse_CourseIdAndIsPublishedTrueOrderByOrderIndex(c.getCourseId())
                    .size())
        .enrollmentCount(enrollments.countByCourse_CourseId(c.getCourseId()))
        .averageRating(reviews.averageRating(c.getCourseId()))
        .build();
  }
}
