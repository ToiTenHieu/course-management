package com.example.course_management.service.impl;

import com.example.course_management.dto.response.EnrollmentDetailResponse;
import com.example.course_management.dto.response.EnrollmentResponse;
import com.example.course_management.dto.response.LessonNoteResponse;
import com.example.course_management.entity.*;
import com.example.course_management.exception.BadRequestException;
import com.example.course_management.exception.ConflictException;
import com.example.course_management.exception.ForbiddenException;
import com.example.course_management.exception.ResourceNotFoundException;
import com.example.course_management.repository.*;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.EnrollmentService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EnrollmentServiceImpl implements EnrollmentService {

  private final UserRepository userRepository;
  private final com.example.course_management.service.ProgressCalculator calculator;
  private final EnrollmentRepository enrollmentRepository;
  private final CourseRepository courseRepository;
  private final LessonRepository lessonRepository;
  private final LessonProgressRepository lessonProgressRepository;

  public EnrollmentServiceImpl(
      EnrollmentRepository enrollmentRepository,
      CourseRepository courseRepository,
      LessonRepository lessonRepository,
      LessonProgressRepository lessonProgressRepository,
      UserRepository userRepository,
      com.example.course_management.service.ProgressCalculator calculator) {
    this.userRepository = userRepository;
    this.calculator = calculator;
    this.enrollmentRepository = enrollmentRepository;
    this.courseRepository = courseRepository;
    this.lessonRepository = lessonRepository;
    this.lessonProgressRepository = lessonProgressRepository;
  }

  @Override
  public List<EnrollmentResponse> getMyEnrollments(CustomUserDetails actor) {
    return enrollmentRepository.findByStudent_UserId(actor.getUser().getUserId()).stream()
        .map(this::toResponse)
        .toList();
  }

  @Override
  public EnrollmentResponse enroll(Integer courseId, CustomUserDetails actor) {
    userRepository
        .findLockedById(actor.getUser().getUserId())
        .orElseThrow(() -> new ForbiddenException("Tài khoản không tồn tại"));
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy khóa học id=" + courseId));

    if (course.getStatus() != CourseStatus.PUBLISHED) {
      throw new BadRequestException("Chỉ có thể đăng ký khóa học đã xuất bản");
    }

    // Luật mới: khóa học có phí phải thanh toán trước, không cho đăng ký thẳng
    if (course.getPrice() != null && course.getPrice().compareTo(BigDecimal.ZERO) > 0) {
      throw new BadRequestException("Khóa học này có phí, vui lòng thanh toán trước khi đăng ký");
    }

    if (enrollmentRepository.existsByStudent_UserIdAndCourse_CourseId(
        actor.getUser().getUserId(), courseId)) {
      throw new ConflictException("Bạn đã đăng ký khóa học này rồi");
    }

    Enrollment enrollment = new Enrollment();
    enrollment.setStudent(actor.getUser());
    enrollment.setCourse(course);
    enrollment.setEnrollmentDate(LocalDateTime.now());
    enrollment.setStatus(EnrollmentStatus.ENROLLED);
    enrollment.setProgressPercentage(BigDecimal.ZERO);

    return toResponse(enrollmentRepository.save(enrollment));
  }

  @Override
  public EnrollmentDetailResponse getEnrollmentDetail(
      Integer enrollmentId, CustomUserDetails actor) {
    Enrollment enrollment = findEnrollmentOrThrow(enrollmentId);
    requireOwner(enrollment, actor);
    if (enrollment.getStatus() == EnrollmentStatus.DROPPED)
      throw new ForbiddenException("Lượt đăng ký không còn hiệu lực");
    return toDetailResponse(enrollment);
  }

  @Override
  public EnrollmentDetailResponse completeLesson(
      Integer enrollmentId, Integer lessonId, CustomUserDetails actor) {
    Enrollment initial = findEnrollmentOrThrow(enrollmentId);
    courseRepository
        .findLockedById(initial.getCourse().getCourseId())
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học"));
    Enrollment enrollment =
        enrollmentRepository
            .findLockedById(enrollmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt đăng ký"));
    requireOwner(enrollment, actor);

    Lesson lesson =
        lessonRepository
            .findById(lessonId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy bài học id=" + lessonId));

    if (!lesson.getCourse().getCourseId().equals(enrollment.getCourse().getCourseId())) {
      throw new BadRequestException("Bài học không thuộc khóa học đã đăng ký này");
    }

    if (!Boolean.TRUE.equals(lesson.getIsPublished()))
      throw new BadRequestException("Bài học chưa được xuất bản");
    if (enrollment.getStatus() == EnrollmentStatus.DROPPED)
      throw new ForbiddenException("Lượt đăng ký không còn hiệu lực");

    LessonProgress progress =
        lessonProgressRepository
            .findByEnrollment_EnrollmentIdAndLesson_LessonId(enrollmentId, lessonId)
            .orElseGet(
                () -> {
                  LessonProgress p = new LessonProgress();
                  p.setEnrollment(enrollment);
                  p.setLesson(lesson);
                  return p;
                });

    progress.setIsCompleted(true);
    if (progress.getCompletedAt() == null) progress.setCompletedAt(LocalDateTime.now());
    progress.setLastAccessedAt(LocalDateTime.now());
    lessonProgressRepository.save(progress);

    recalculateProgress(enrollment);

    return toDetailResponse(enrollment);
  }

  private void recalculateProgress(Enrollment enrollment) {
    calculator.recalculate(enrollment);
  }

  @Transactional(readOnly = true)
  public LessonNoteResponse getNote(
      Integer enrollmentId, Integer lessonId, CustomUserDetails actor) {
    var enrollment = findEnrollmentOrThrow(enrollmentId);
    requireAccessibleLesson(enrollment, lessonId, actor);
    return new LessonNoteResponse(
        lessonId,
        lessonProgressRepository
            .findByEnrollment_EnrollmentIdAndLesson_LessonId(enrollmentId, lessonId)
            .map(p -> p.getNote() == null ? "" : p.getNote())
            .orElse(""));
  }

  public LessonNoteResponse saveNote(
      Integer enrollmentId, Integer lessonId, String note, CustomUserDetails actor) {
    if (note == null || note.length() > 10000)
      throw new BadRequestException("Ghi chú tối đa 10000 ký tự");
    var p = writableProgress(enrollmentId, lessonId, actor);
    p.setNote(note);
    lessonProgressRepository.save(p);
    return new LessonNoteResponse(lessonId, note);
  }

  public void accessLesson(Integer enrollmentId, Integer lessonId, CustomUserDetails actor) {
    var p = writableProgress(enrollmentId, lessonId, actor);
    p.setLastAccessedAt(LocalDateTime.now());
    lessonProgressRepository.save(p);
  }

  private LessonProgress writableProgress(
      Integer enrollmentId, Integer lessonId, CustomUserDetails actor) {
    var initial = findEnrollmentOrThrow(enrollmentId);
    requireOwner(initial, actor);
    // Same lock ordering as completion/publication protects against simultaneous progress writes.
    courseRepository
        .findLockedById(initial.getCourse().getCourseId())
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học"));
    var enrollment =
        enrollmentRepository
            .findLockedById(enrollmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt đăng ký"));
    var lesson = requireAccessibleLesson(enrollment, lessonId, actor);
    return lessonProgressRepository
        .findByEnrollment_EnrollmentIdAndLesson_LessonId(enrollmentId, lessonId)
        .orElseGet(
            () -> {
              var p = new LessonProgress();
              p.setEnrollment(enrollment);
              p.setLesson(lesson);
              return p;
            });
  }

  private Lesson requireAccessibleLesson(
      Enrollment enrollment, Integer lessonId, CustomUserDetails actor) {
    requireOwner(enrollment, actor);
    if (enrollment.getStatus() == EnrollmentStatus.DROPPED)
      throw new ForbiddenException("Lượt đăng ký không còn hiệu lực");
    var lesson =
        lessonRepository
            .findById(lessonId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học"));
    if (!lesson.getCourse().getCourseId().equals(enrollment.getCourse().getCourseId())
        || !Boolean.TRUE.equals(lesson.getIsPublished()))
      throw new ForbiddenException("Bài học không thuộc chương trình đã đăng ký");
    return lesson;
  }

  private void requireOwner(Enrollment enrollment, CustomUserDetails actor) {
    if (!enrollment.getStudent().getUserId().equals(actor.getUser().getUserId())) {
      throw new ForbiddenException("Bạn không có quyền truy cập lượt đăng ký này");
    }
  }

  private Enrollment findEnrollmentOrThrow(Integer enrollmentId) {
    return enrollmentRepository
        .findById(enrollmentId)
        .orElseThrow(
            () -> new ResourceNotFoundException("Không tìm thấy lượt đăng ký id=" + enrollmentId));
  }

  private EnrollmentResponse toResponse(Enrollment e) {
    return EnrollmentResponse.builder()
        .enrollmentId(e.getEnrollmentId())
        .courseId(e.getCourse().getCourseId())
        .courseTitle(e.getCourse().getTitle())
        .enrollmentDate(e.getEnrollmentDate())
        .status(e.getStatus().name())
        .progressPercentage(e.getProgressPercentage())
        .build();
  }

  private EnrollmentDetailResponse toDetailResponse(Enrollment e) {
    List<Lesson> lessons =
        lessonRepository.findByCourse_CourseIdAndIsPublishedTrueOrderByOrderIndex(
            e.getCourse().getCourseId());
    List<LessonProgress> progressList =
        lessonProgressRepository.findByEnrollment_EnrollmentId(e.getEnrollmentId());

    List<EnrollmentDetailResponse.LessonProgressItem> items =
        lessons.stream()
            .map(
                l -> {
                  boolean completed =
                      progressList.stream()
                          .anyMatch(
                              p ->
                                  p.getLesson().getLessonId().equals(l.getLessonId())
                                      && p.getIsCompleted());
                  return EnrollmentDetailResponse.LessonProgressItem.builder()
                      .lessonId(l.getLessonId())
                      .title(l.getTitle())
                      .orderIndex(l.getOrderIndex())
                      .isCompleted(completed)
                      .build();
                })
            .toList();

    return EnrollmentDetailResponse.builder()
        .enrollmentId(e.getEnrollmentId())
        .courseId(e.getCourse().getCourseId())
        .courseTitle(e.getCourse().getTitle())
        .status(e.getStatus().name())
        .progressPercentage(e.getProgressPercentage())
        .lessons(items)
        .lastLessonId(
            progressList.stream()
                .filter(
                    p ->
                        lessons.stream()
                            .anyMatch(l -> l.getLessonId().equals(p.getLesson().getLessonId())))
                .max(Comparator.comparing(LessonProgress::getLastAccessedAt))
                .map(p -> p.getLesson().getLessonId())
                .orElse(null))
        .build();
  }
}
