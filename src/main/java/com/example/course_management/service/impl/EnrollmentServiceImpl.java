package com.example.course_management.service.impl;

import com.example.course_management.dto.response.EnrollmentDetailResponse;
import com.example.course_management.dto.response.EnrollmentResponse;
import com.example.course_management.entity.*;
import com.example.course_management.exception.BadRequestException;
import com.example.course_management.exception.ConflictException;
import com.example.course_management.exception.ForbiddenException;
import com.example.course_management.exception.ResourceNotFoundException;
import com.example.course_management.repository.*;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.EnrollmentService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;

    public EnrollmentServiceImpl(EnrollmentRepository enrollmentRepository, CourseRepository courseRepository,
                                 LessonRepository lessonRepository, LessonProgressRepository lessonProgressRepository) {
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
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học id=" + courseId));

        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new BadRequestException("Chỉ có thể đăng ký khóa học đã xuất bản");
        }

        // Luật mới: khóa học có phí phải thanh toán trước, không cho đăng ký thẳng
        if (course.getPrice() != null && course.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            throw new BadRequestException("Khóa học này có phí, vui lòng thanh toán trước khi đăng ký");
        }

        if (enrollmentRepository.existsByStudent_UserIdAndCourse_CourseId(actor.getUser().getUserId(), courseId)) {
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
    public EnrollmentDetailResponse getEnrollmentDetail(Integer enrollmentId, CustomUserDetails actor) {
        Enrollment enrollment = findEnrollmentOrThrow(enrollmentId);
        requireOwner(enrollment, actor);
        return toDetailResponse(enrollment);
    }

    @Override
    public EnrollmentDetailResponse completeLesson(Integer enrollmentId, Integer lessonId, CustomUserDetails actor) {
        Enrollment enrollment = findEnrollmentOrThrow(enrollmentId);
        requireOwner(enrollment, actor);

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học id=" + lessonId));

        if (!lesson.getCourse().getCourseId().equals(enrollment.getCourse().getCourseId())) {
            throw new BadRequestException("Bài học không thuộc khóa học đã đăng ký này");
        }

        LessonProgress progress = lessonProgressRepository
                .findByEnrollment_EnrollmentIdAndLesson_LessonId(enrollmentId, lessonId)
                .orElseGet(() -> {
                    LessonProgress p = new LessonProgress();
                    p.setEnrollment(enrollment);
                    p.setLesson(lesson);
                    return p;
                });

        progress.setIsCompleted(true);
        progress.setCompletedAt(LocalDateTime.now());
        progress.setLastAccessedAt(LocalDateTime.now());
        lessonProgressRepository.save(progress);

        recalculateProgress(enrollment);

        return toDetailResponse(enrollment);
    }

    /** Tính lại % tiến độ = số bài đã hoàn thành / tổng số bài PUBLISHED của khóa học */
    private void recalculateProgress(Enrollment enrollment) {
        Integer courseId = enrollment.getCourse().getCourseId();
        List<Lesson> publishedLessons = lessonRepository
                .findByCourse_CourseIdAndIsPublishedTrueOrderByOrderIndex(courseId);
        List<LessonProgress> allProgress = lessonProgressRepository
                .findByEnrollment_EnrollmentId(enrollment.getEnrollmentId());

        long completedCount = allProgress.stream().filter(LessonProgress::getIsCompleted).count();
        int totalLessons = publishedLessons.size();

        BigDecimal percentage = totalLessons == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(completedCount)
                .divide(BigDecimal.valueOf(totalLessons), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);

        enrollment.setProgressPercentage(percentage);

        if (percentage.compareTo(BigDecimal.valueOf(100)) >= 0) {
            enrollment.setStatus(EnrollmentStatus.COMPLETED);
            enrollment.setCompletionDate(LocalDateTime.now());
        }

        enrollmentRepository.save(enrollment);
    }

    private void requireOwner(Enrollment enrollment, CustomUserDetails actor) {
        if (!enrollment.getStudent().getUserId().equals(actor.getUser().getUserId())) {
            throw new ForbiddenException("Bạn không có quyền truy cập lượt đăng ký này");
        }
    }

    private Enrollment findEnrollmentOrThrow(Integer enrollmentId) {
        return enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt đăng ký id=" + enrollmentId));
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
        List<Lesson> lessons = lessonRepository
                .findByCourse_CourseIdAndIsPublishedTrueOrderByOrderIndex(e.getCourse().getCourseId());
        List<LessonProgress> progressList = lessonProgressRepository
                .findByEnrollment_EnrollmentId(e.getEnrollmentId());

        List<EnrollmentDetailResponse.LessonProgressItem> items = lessons.stream().map(l -> {
            boolean completed = progressList.stream()
                    .anyMatch(p -> p.getLesson().getLessonId().equals(l.getLessonId()) && p.getIsCompleted());
            return EnrollmentDetailResponse.LessonProgressItem.builder()
                    .lessonId(l.getLessonId())
                    .title(l.getTitle())
                    .orderIndex(l.getOrderIndex())
                    .isCompleted(completed)
                    .build();
        }).toList();

        return EnrollmentDetailResponse.builder()
                .enrollmentId(e.getEnrollmentId())
                .courseId(e.getCourse().getCourseId())
                .courseTitle(e.getCourse().getTitle())
                .status(e.getStatus().name())
                .progressPercentage(e.getProgressPercentage())
                .lessons(items)
                .build();
    }
}