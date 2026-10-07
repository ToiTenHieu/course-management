package com.example.course_management.service.impl;

import com.example.course_management.dto.request.*;
import com.example.course_management.dto.response.*;
import com.example.course_management.entity.*;
import com.example.course_management.exception.*;
import com.example.course_management.repository.*;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.*;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

  @Transactional(readOnly = true)
  public PageResponse<CourseResponse> getCatalog(
      String search,
      Integer teacherId,
      CourseStatus status,
      String category,
      boolean freeOnly,
      String sort,
      int page,
      int size,
      CustomUserDetails actor) {
    if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE)
      throw new BadRequestException("Trang phải từ 0 và kích thước trang từ 1 đến 100");
    if ((search != null && search.length() > 255) || (category != null && category.length() > 255))
      throw new BadRequestException("Từ khóa và chủ đề không được vượt quá 255 ký tự");
    Sort ordering =
        switch (sort) {
          case "new" -> Sort.by(Sort.Direction.DESC, "courseId");
          case "price" -> Sort.by("price").and(Sort.by(Sort.Direction.DESC, "courseId"));
          case "title" -> Sort.by("title").and(Sort.by(Sort.Direction.DESC, "courseId"));
          default -> throw new BadRequestException("Cách sắp xếp không hợp lệ");
        };
    var result =
        courses.findAll(
            catalogFilter(search, teacherId, status, category, freeOnly, actor),
            PageRequest.of(page, size, ordering));
    return new PageResponse<>(
        toResponses(result.getContent()),
        page,
        size,
        result.getTotalElements(),
        result.getTotalPages());
  }

  @Transactional(readOnly = true)
  public List<String> getCategories(Integer teacherId, CustomUserDetails actor) {
    var u = actor.getUser();
    return courses.findVisibleCategories(
        u.getRole() == Role.ADMIN,
        CourseStatus.PUBLISHED,
        u.getRole() == Role.TEACHER ? u.getUserId() : -1,
        teacherId);
  }

  private Specification<Course> catalogFilter(
      String search,
      Integer teacherId,
      CourseStatus status,
      String category,
      boolean freeOnly,
      CustomUserDetails actor) {
    return (root, query, cb) -> {
      var predicates = new ArrayList<Predicate>();
      var u = actor.getUser();
      if (u.getRole() != Role.ADMIN) {
        var published = cb.equal(root.get("status"), CourseStatus.PUBLISHED);
        predicates.add(
            u.getRole() == Role.TEACHER
                ? cb.or(published, cb.equal(root.get("teacher").get("userId"), u.getUserId()))
                : published);
      }
      if (status != null) predicates.add(cb.equal(root.get("status"), status));
      if (teacherId != null) predicates.add(cb.equal(root.get("teacher").get("userId"), teacherId));
      if (category != null && !category.isBlank())
        predicates.add(cb.equal(root.get("category"), category.trim()));
      if (freeOnly) predicates.add(cb.equal(root.get("price"), BigDecimal.ZERO));
      if (search != null && !search.isBlank()) {
        String pattern =
            "%"
                + search
                    .trim()
                    .toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_")
                + "%";
        predicates.add(
            cb.or(
                cb.like(cb.lower(root.get("title")), pattern, '\\'),
                cb.like(cb.lower(root.get("description")), pattern, '\\'),
                cb.like(cb.lower(root.get("teacher").get("fullName")), pattern, '\\')));
      }
      return cb.and(predicates.toArray(Predicate[]::new));
    };
  }

  @Transactional(readOnly = true)
  public List<CourseResponse> getCourses(
      String search, Integer teacherId, CourseStatus status, CustomUserDetails actor) {
    return toResponses(
        courses.findAll(
            catalogFilter(search, teacherId, status, null, false, actor),
            Sort.by(Sort.Direction.DESC, "courseId")));
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
    return toResponses(List.of(c)).getFirst();
  }

  private List<CourseResponse> toResponses(List<Course> found) {
    if (found.isEmpty()) return List.of();
    var ids = found.stream().map(Course::getCourseId).toList();
    var lessonCounts =
        lessons.countPublishedByCourses(ids).stream()
            .collect(Collectors.toMap(CourseCount::getCourseId, CourseCount::getTotal));
    var enrollmentCounts =
        enrollments.countByCourses(ids).stream()
            .collect(Collectors.toMap(CourseCount::getCourseId, CourseCount::getTotal));
    var ratings =
        reviews.averageRatingByCourses(ids).stream()
            .collect(Collectors.toMap(CourseRating::getCourseId, CourseRating::getAverageRating));
    return found.stream()
        .map(
            c ->
                toResponse(
                    c,
                    lessonCounts.getOrDefault(c.getCourseId(), 0L),
                    enrollmentCounts.getOrDefault(c.getCourseId(), 0L),
                    ratings.get(c.getCourseId())))
        .toList();
  }

  private CourseResponse toResponse(
      Course c, long lessonCount, long enrollmentCount, Double rating) {
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
        .lessonCount(lessonCount)
        .enrollmentCount(enrollmentCount)
        .averageRating(rating)
        .build();
  }
}
