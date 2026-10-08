package com.example.course_management.service;

import com.example.course_management.dto.response.*;
import com.example.course_management.entity.*;
import com.example.course_management.exception.*;
import com.example.course_management.repository.*;
import com.example.course_management.security.CustomUserDetails;
import jakarta.persistence.criteria.Predicate;
import java.util.*;
import java.util.function.Function;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PagedListService {
  private final UserRepository users;
  private final PaymentRepository payments;
  private final EnrollmentRepository enrollments;
  private final NotificationRepository notifications;
  private final CourseRepository courses;
  private final CourseService courseService;
  private final ReviewRepository reviews;
  private final ContentPolicy policy;

  public PagedListService(
      UserRepository users,
      PaymentRepository payments,
      EnrollmentRepository enrollments,
      NotificationRepository notifications,
      CourseRepository courses,
      CourseService courseService,
      ReviewRepository reviews,
      ContentPolicy policy) {
    this.users = users;
    this.payments = payments;
    this.enrollments = enrollments;
    this.notifications = notifications;
    this.courses = courses;
    this.courseService = courseService;
    this.reviews = reviews;
    this.policy = policy;
  }

  public PageResponse<UserResponse> users(
      String search, Role role, String status, int page, int size, CustomUserDetails actor) {
    require(actor, Role.ADMIN);
    Boolean active = activeStatus(status);
    var pattern = pattern(search);
    Specification<User> filter =
        (root, q, cb) -> {
          var terms = new ArrayList<Predicate>();
          if (role != null) terms.add(cb.equal(root.get("role"), role));
          if (active != null) terms.add(cb.equal(root.get("isActive"), active));
          terms.add(
              cb.or(
                  cb.like(cb.lower(root.get("fullName")), pattern, '\\'),
                  cb.like(cb.lower(root.get("username")), pattern, '\\'),
                  cb.like(cb.lower(root.get("email")), pattern, '\\')));
          return cb.and(terms.toArray(Predicate[]::new));
        };
    return page(
        users.findAll(filter, request(page, size, Sort.by("fullName", "userId"))),
        u ->
            UserResponse.builder()
                .userId(u.getUserId())
                .username(u.getUsername())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .role(u.getRole().name())
                .isActive(u.getIsActive())
                .createdAt(u.getCreatedAt())
                .build());
  }

  public PageResponse<PaymentResponse> payments(
      String search,
      PaymentStatus status,
      String sort,
      int page,
      int size,
      CustomUserDetails actor) {
    if (actor.getUser().getRole() == Role.TEACHER)
      throw new ForbiddenException("Bạn không có quyền xem thanh toán");
    if (!List.of("new", "old").contains(sort))
      throw new BadRequestException("Thứ tự thanh toán không hợp lệ");
    var result =
        payments.findAll(
            paymentFilter(actor, status, pattern(search)),
            request(
                page,
                size,
                Sort.by(
                    sort.equals("old") ? Sort.Direction.ASC : Sort.Direction.DESC,
                    "createdAt",
                    "paymentId")));
    return page(result, this::paymentResponse);
  }

  private PaymentResponse paymentResponse(Payment p) {
    return PaymentResponse.builder()
        .paymentId(p.getPaymentId())
        .courseId(p.getCourse().getCourseId())
        .courseTitle(p.getCourse().getTitle())
        .studentId(p.getStudent().getUserId())
        .studentName(p.getStudent().getFullName())
        .amount(p.getAmount())
        .status(p.getStatus().name())
        .transferNote(p.getTransferNote())
        .createdAt(p.getCreatedAt())
        .confirmedAt(p.getConfirmedAt())
        .build();
  }

  public PageResponse<LearningCourseResponse> learning(
      String search,
      EnrollmentStatus status,
      String sort,
      int page,
      int size,
      CustomUserDetails actor) {
    require(actor, Role.STUDENT);
    Sort ordering =
        switch (sort) {
          case "new" -> Sort.by(Sort.Direction.DESC, "enrollmentId");
          case "progress" -> Sort.by(Sort.Direction.DESC, "progressPercentage", "enrollmentId");
          case "title" -> Sort.by("course.title").and(Sort.by(Sort.Direction.DESC, "enrollmentId"));
          default -> throw new BadRequestException("Thứ tự khóa học không hợp lệ");
        };
    var result =
        enrollments.findAll(
            learningFilter(actor.getUser().getUserId(), status, pattern(search)),
            request(page, size, ordering));
    var summaries =
        courseService.summarizeCourses(
            result.getContent().stream().map(Enrollment::getCourse).toList());
    var byId = new HashMap<Integer, CourseResponse>();
    summaries.forEach(c -> byId.put(c.getCourseId(), c));
    return page(
        result,
        e ->
            new LearningCourseResponse(
                e.getEnrollmentId(),
                e.getCourse().getCourseId(),
                e.getCourse().getTitle(),
                e.getStatus().name(),
                e.getProgressPercentage(),
                e.getEnrollmentDate(),
                byId.get(e.getCourse().getCourseId())));
  }

  public PageResponse<NotificationResponse> notifications(
      String status, int page, int size, CustomUserDetails actor) {
    Boolean read =
        switch (status) {
          case "", "all" -> null;
          case "read" -> true;
          case "unread" -> false;
          default -> throw new BadRequestException("Trạng thái thông báo không hợp lệ");
        };
    return page(
        notifications.findAll(
            noticeFilter(actor.getUser().getUserId(), read),
            request(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "notificationId"))),
        n ->
            NotificationResponse.builder()
                .notificationId(n.getNotificationId())
                .message(n.getMessage())
                .type(n.getType())
                .targetUrl(n.getTargetUrl())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build());
  }

  public Map<String, Long> summary(CustomUserDetails actor) {
    var role = actor.getUser().getRole();
    var id = actor.getUser().getUserId();
    var stats = new LinkedHashMap<String, Long>();
    stats.put("unread", notifications.count(noticeFilter(id, false)));
    stats.put("notifications", notifications.count(noticeFilter(id, null)));
    if (role == Role.STUDENT) {
      stats.put("enrollments", enrollments.count(learningFilter(id, null, "%")));
      for (var s : EnrollmentStatus.values())
        stats.put(s.name(), enrollments.count(learningFilter(id, s, "%")));
    } else {
      Specification<Course> scope =
          (r, q, cb) ->
              role == Role.ADMIN ? cb.conjunction() : cb.equal(r.get("teacher").get("userId"), id);
      stats.put("courses", courses.count(scope));
      stats.put(
          "drafts",
          courses.count(scope.and((r, q, cb) -> cb.equal(r.get("status"), CourseStatus.DRAFT))));
      stats.put(
          "enrollments",
          enrollments.count(
              (r, q, cb) ->
                  role == Role.ADMIN
                      ? cb.conjunction()
                      : cb.equal(r.get("course").get("teacher").get("userId"), id)));
    }
    if (role != Role.TEACHER)
      for (var s : PaymentStatus.values())
        stats.put(s.name(), payments.count(paymentFilter(actor, s, "%")));
    if (role == Role.ADMIN) {
      stats.put("users", users.count());
      stats.put("activeUsers", users.count((r, q, cb) -> cb.isTrue(r.get("isActive"))));
    }
    return stats;
  }

  @Transactional
  public int markAllRead(CustomUserDetails actor) {
    return notifications.markAllRead(actor.getUser().getUserId());
  }

  public Map<String, Object> courseState(Integer courseId, CustomUserDetails actor) {
    require(actor, Role.STUDENT);
    var result = new LinkedHashMap<String, Object>();
    result.put(
        "enrollment",
        enrollments
            .findByStudent_UserIdAndCourse_CourseId(actor.getUser().getUserId(), courseId)
            .map(
                e ->
                    EnrollmentResponse.builder()
                        .enrollmentId(e.getEnrollmentId())
                        .courseId(courseId)
                        .courseTitle(e.getCourse().getTitle())
                        .status(e.getStatus().name())
                        .enrollmentDate(e.getEnrollmentDate())
                        .progressPercentage(e.getProgressPercentage())
                        .build())
            .orElse(null));
    var found =
        payments.findAll(
            paymentFilter(actor, PaymentStatus.PENDING, "%")
                .and((r, q, cb) -> cb.equal(r.get("course").get("courseId"), courseId)),
            request(0, 1, Sort.by(Sort.Direction.DESC, "paymentId")));
    result.put("payment", found.isEmpty() ? null : paymentResponse(found.getContent().getFirst()));
    return result;
  }

  public PageResponse<ReviewResponse> reviews(
      Integer courseId, int page, int size, CustomUserDetails actor) {
    var course =
        courses
            .findById(courseId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học"));
    policy.visible(course, actor);
    return page(
        reviews.findAll(
            (r, q, cb) -> cb.equal(r.get("course").get("courseId"), courseId),
            request(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "reviewId"))),
        this::reviewResponse);
  }

  public ReviewResponse ownReview(Integer courseId, CustomUserDetails actor) {
    require(actor, Role.STUDENT);
    return reviews
        .findByCourse_CourseIdAndStudent_UserId(courseId, actor.getUser().getUserId())
        .map(this::reviewResponse)
        .orElse(null);
  }

  private ReviewResponse reviewResponse(Review r) {
    return ReviewResponse.builder()
        .reviewId(r.getReviewId())
        .courseId(r.getCourse().getCourseId())
        .studentId(r.getStudent().getUserId())
        .studentName(r.getStudent().getFullName())
        .rating(r.getRating())
        .comment(r.getComment())
        .createdAt(r.getCreatedAt())
        .build();
  }

  public Map<String, Object> studentReport(
      Integer id, int page, int size, CustomUserDetails actor) {
    require(actor, Role.ADMIN);
    var target = reportUser(id, Role.STUDENT);
    var summary = new LinkedHashMap<String, Object>();
    summary.put("totalEnrollments", enrollments.count(learningFilter(id, null, "%")));
    summary.put(
        "completedCount", enrollments.count(learningFilter(id, EnrollmentStatus.COMPLETED, "%")));
    var avg = enrollments.averageProgress(id);
    summary.put("averageProgress", avg == null ? 0 : Math.round(avg * 100.0) / 100.0);
    return Map.of(
        "summary",
        summary,
        "courses",
        learning("", null, "new", page, size, new CustomUserDetails(target)));
  }

  public Map<String, Object> teacherReport(
      Integer id, int page, int size, CustomUserDetails actor) {
    require(actor, Role.ADMIN);
    reportUser(id, Role.TEACHER);
    Specification<Course> scope = (r, q, cb) -> cb.equal(r.get("teacher").get("userId"), id);
    var summary =
        Map.of(
            "totalCourses",
            courses.count(scope),
            "totalEnrollments",
            enrollments.count(
                (r, q, cb) -> cb.equal(r.get("course").get("teacher").get("userId"), id)));
    request(page, size, Sort.unsorted());
    return Map.of(
        "summary",
        summary,
        "courses",
        courseService.getCatalog("", id, null, "", false, "new", page, size, actor));
  }

  private User reportUser(Integer id, Role role) {
    var user =
        users
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
    if (user.getRole() != role) throw new BadRequestException("Vai trò không phù hợp với báo cáo");
    return user;
  }

  private Specification<Payment> paymentFilter(
      CustomUserDetails actor, PaymentStatus status, String pattern) {
    return (r, q, cb) -> {
      var terms = new ArrayList<Predicate>();
      if (actor.getUser().getRole() != Role.ADMIN)
        terms.add(cb.equal(r.get("student").get("userId"), actor.getUser().getUserId()));
      if (status != null) terms.add(cb.equal(r.get("status"), status));
      terms.add(
          cb.or(
              cb.like(cb.lower(r.get("course").get("title")), pattern, '\\'),
              cb.like(cb.lower(r.get("student").get("fullName")), pattern, '\\'),
              cb.like(cb.lower(r.get("transferNote")), pattern, '\\')));
      return cb.and(terms.toArray(Predicate[]::new));
    };
  }

  private Specification<Enrollment> learningFilter(
      Integer id, EnrollmentStatus status, String pattern) {
    return (r, q, cb) ->
        cb.and(
            cb.equal(r.get("student").get("userId"), id),
            status == null ? cb.conjunction() : cb.equal(r.get("status"), status),
            cb.like(cb.lower(r.get("course").get("title")), pattern, '\\'));
  }

  private Specification<Notification> noticeFilter(Integer id, Boolean read) {
    return (r, q, cb) ->
        cb.and(
            cb.equal(r.get("user").get("userId"), id),
            read == null ? cb.conjunction() : cb.equal(r.get("isRead"), read));
  }

  private Boolean activeStatus(String status) {
    if (status == null || status.isBlank()) return null;
    return switch (status) {
      case "active" -> true;
      case "inactive" -> false;
      default -> throw new BadRequestException("Trạng thái phải là active hoặc inactive");
    };
  }

  private String pattern(String search) {
    if (search == null) return "%";
    if (search.length() > 255) throw new BadRequestException("Từ khóa tối đa 255 ký tự");
    return "%"
        + search
            .strip()
            .toLowerCase(Locale.ROOT)
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
        + "%";
  }

  private Pageable request(int page, int size, Sort sort) {
    if (page < 0 || page >= 1000000 || size < 1 || size > 100)
      throw new BadRequestException("Trang hoặc kích thước trang không hợp lệ");
    return PageRequest.of(page, size, sort);
  }

  private <T, R> PageResponse<R> page(Page<T> result, Function<T, R> map) {
    return new PageResponse<>(
        result.getContent().stream().map(map).toList(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }

  private void require(CustomUserDetails actor, Role role) {
    if (actor.getUser().getRole() != role)
      throw new ForbiddenException("Bạn không có quyền truy cập danh sách này");
  }
}
