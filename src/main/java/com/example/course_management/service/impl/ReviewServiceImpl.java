package com.example.course_management.service.impl;

import com.example.course_management.dto.request.ReviewRequest;
import com.example.course_management.dto.response.ReviewResponse;
import com.example.course_management.entity.Course;
import com.example.course_management.entity.Review;
import com.example.course_management.entity.Role;
import com.example.course_management.exception.ConflictException;
import com.example.course_management.exception.ForbiddenException;
import com.example.course_management.exception.ResourceNotFoundException;
import com.example.course_management.repository.CourseRepository;
import com.example.course_management.repository.EnrollmentRepository;
import com.example.course_management.repository.ReviewRepository;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.ReviewService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReviewServiceImpl implements ReviewService {

  private final com.example.course_management.service.ContentPolicy policy;
  private final ReviewRepository reviewRepository;
  private final CourseRepository courseRepository;
  private final EnrollmentRepository enrollmentRepository;

  public ReviewServiceImpl(
      ReviewRepository reviewRepository,
      CourseRepository courseRepository,
      EnrollmentRepository enrollmentRepository,
      com.example.course_management.service.ContentPolicy policy) {
    this.policy = policy;
    this.reviewRepository = reviewRepository;
    this.courseRepository = courseRepository;
    this.enrollmentRepository = enrollmentRepository;
  }

  @Override
  public List<ReviewResponse> getReviews(Integer courseId, CustomUserDetails actor) {
    policy.visible(findCourseOrThrow(courseId), actor);
    return reviewRepository.findByCourse_CourseIdOrderByCreatedAtDesc(courseId).stream()
        .map(this::toResponse)
        .toList();
  }

  @Override
  public ReviewResponse create(Integer courseId, ReviewRequest req, CustomUserDetails actor) {
    Course course = findCourseOrThrow(courseId);
    Integer studentId = actor.getUser().getUserId();

    // Luật: chỉ sinh viên ĐÃ ĐĂNG KÝ khóa học mới được đánh giá ("khóa học đã học")
    if (!policy.enrolled(course, actor)) {
      throw new ForbiddenException("Bạn cần đăng ký khóa học này trước khi đánh giá");
    }
    // Luật: mỗi sinh viên chỉ đánh giá 1 lần (khớp UNIQUE(course_id, student_id) trong DB)
    if (reviewRepository.existsByCourse_CourseIdAndStudent_UserId(courseId, studentId)) {
      throw new ConflictException("Bạn đã đánh giá khóa học này rồi");
    }

    Review r = new Review();
    r.setCourse(course);
    r.setStudent(actor.getUser());
    r.setRating(req.getRating());
    r.setComment(req.getComment());
    r.setCreatedAt(LocalDateTime.now());
    r.setUpdatedAt(LocalDateTime.now());
    return toResponse(reviewRepository.save(r));
  }

  @Override
  public ReviewResponse update(Integer reviewId, ReviewRequest req, CustomUserDetails actor) {
    Review r = findReviewOrThrow(reviewId);
    requireOwnerOrAdmin(r, actor);
    r.setRating(req.getRating());
    r.setComment(req.getComment());
    r.setUpdatedAt(LocalDateTime.now());
    return toResponse(reviewRepository.save(r));
  }

  @Override
  public void delete(Integer reviewId, CustomUserDetails actor) {
    Review r = findReviewOrThrow(reviewId);
    requireOwnerOrAdmin(r, actor);
    reviewRepository.delete(r);
  }

  private void requireOwnerOrAdmin(Review r, CustomUserDetails actor) {
    boolean isAdmin = actor.getUser().getRole() == Role.ADMIN;
    boolean isOwner = r.getStudent().getUserId().equals(actor.getUser().getUserId());
    if (!isAdmin && !isOwner) {
      throw new ForbiddenException("Bạn không có quyền với đánh giá này");
    }
  }

  private Course findCourseOrThrow(Integer id) {
    return courseRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học id=" + id));
  }

  private Review findReviewOrThrow(Integer id) {
    return reviewRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá id=" + id));
  }

  private ReviewResponse toResponse(Review r) {
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
}
