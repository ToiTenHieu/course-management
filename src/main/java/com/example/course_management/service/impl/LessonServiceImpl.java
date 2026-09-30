package com.example.course_management.service.impl;

import com.example.course_management.dto.request.*;
import com.example.course_management.dto.response.LessonPreviewResponse;
import com.example.course_management.dto.response.LessonResponse;
import com.example.course_management.entity.Course;
import com.example.course_management.entity.Lesson;
import com.example.course_management.entity.Role;
import com.example.course_management.exception.ForbiddenException;
import com.example.course_management.exception.ResourceNotFoundException;
import com.example.course_management.repository.CourseRepository;
import com.example.course_management.repository.EnrollmentRepository;
import com.example.course_management.repository.LessonRepository;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.LessonService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LessonServiceImpl implements LessonService {

    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    public LessonServiceImpl(LessonRepository lessonRepository, CourseRepository courseRepository,
                             EnrollmentRepository enrollmentRepository) {
        this.lessonRepository = lessonRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Override
    public List<LessonResponse> getLessonsByCourse(Integer courseId, CustomUserDetails actor) {
        Course course = findCourseOrThrow(courseId);
        boolean canSeeUnpublished = isAdminOrOwnerTeacher(course, actor);

        List<Lesson> lessons = canSeeUnpublished
                ? lessonRepository.findByCourse_CourseIdOrderByOrderIndex(courseId)
                : lessonRepository.findByCourse_CourseIdAndIsPublishedTrueOrderByOrderIndex(courseId);

        return lessons.stream().map(this::toResponse).toList();
    }

    @Override
    public LessonResponse getLessonById(Integer lessonId, CustomUserDetails actor) {
        Lesson lesson = findLessonOrThrow(lessonId);
        boolean canSeeUnpublished = isAdminOrOwnerTeacher(lesson.getCourse(), actor);

        if (!lesson.getIsPublished() && !canSeeUnpublished) {
            throw new ResourceNotFoundException("Không tìm thấy bài học id=" + lessonId);
        }

        // Luật mới: STUDENT phải đã đăng ký khóa học này mới xem được NỘI DUNG bài học
        if (actor.getUser().getRole() == Role.STUDENT) {
            boolean enrolled = enrollmentRepository.existsByStudent_UserIdAndCourse_CourseId(
                    actor.getUser().getUserId(), lesson.getCourse().getCourseId());
            if (!enrolled) {
                throw new ForbiddenException("Bạn cần đăng ký khóa học này trước khi xem nội dung bài học");
            }
        }

        return toResponse(lesson);
    }

    @Override
    public LessonResponse createLesson(Integer courseId, CreateLessonRequest req, CustomUserDetails actor) {
        Course course = findCourseOrThrow(courseId);
        requireOwnerTeacherOrAdmin(course, actor); // luật: TEACHER phải là người phụ trách

        Lesson lesson = new Lesson();
        lesson.setCourse(course);
        lesson.setTitle(req.getTitle());
        lesson.setContentUrl(req.getContentUrl());
        lesson.setTextContent(req.getTextContent());
        lesson.setOrderIndex(req.getOrderIndex());
        lesson.setIsPublished(false);
        lesson.setCreatedAt(LocalDateTime.now());
        lesson.setUpdatedAt(LocalDateTime.now());

        return toResponse(lessonRepository.save(lesson));
    }

    @Override
    public LessonResponse updateLesson(Integer lessonId, UpdateLessonRequest req, CustomUserDetails actor) {
        Lesson lesson = findLessonOrThrow(lessonId);
        requireOwnerTeacherOrAdmin(lesson.getCourse(), actor);

        lesson.setTitle(req.getTitle());
        lesson.setContentUrl(req.getContentUrl());
        lesson.setTextContent(req.getTextContent());
        lesson.setOrderIndex(req.getOrderIndex());
        lesson.setUpdatedAt(LocalDateTime.now());

        return toResponse(lessonRepository.save(lesson));
    }

    @Override
    public LessonResponse updatePublishStatus(Integer lessonId, UpdateLessonPublishRequest req, CustomUserDetails actor) {
        Lesson lesson = findLessonOrThrow(lessonId);
        requireOwnerTeacherOrAdmin(lesson.getCourse(), actor);

        lesson.setIsPublished(req.getIsPublished());
        lesson.setUpdatedAt(LocalDateTime.now());

        return toResponse(lessonRepository.save(lesson));
    }

    @Override
    public void deleteLesson(Integer lessonId, CustomUserDetails actor) {
        Lesson lesson = findLessonOrThrow(lessonId);
        requireOwnerTeacherOrAdmin(lesson.getCourse(), actor);
        lessonRepository.delete(lesson);
    }

    // ---- Helper: luật nghiệp vụ dùng chung ----

    private boolean isAdminOrOwnerTeacher(Course course, CustomUserDetails actor) {
        boolean isAdmin = actor.getUser().getRole() == Role.ADMIN;
        boolean isOwnerTeacher = course.getTeacher().getUserId().equals(actor.getUser().getUserId());
        return isAdmin || isOwnerTeacher;
    }

    private void requireOwnerTeacherOrAdmin(Course course, CustomUserDetails actor) {
        if (!isAdminOrOwnerTeacher(course, actor)) {
            throw new ForbiddenException("Bạn không phải giáo viên phụ trách khóa học này");
        }
    }

    private Course findCourseOrThrow(Integer courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học id=" + courseId));
    }

    private Lesson findLessonOrThrow(Integer lessonId) {
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học id=" + lessonId));
    }
    @Override
    public LessonPreviewResponse getContentPreview(Integer lessonId, CustomUserDetails actor) {
        Lesson lesson = findLessonOrThrow(lessonId);
        boolean canSeeUnpublished = isAdminOrOwnerTeacher(lesson.getCourse(), actor);

        if (!lesson.getIsPublished() && !canSeeUnpublished) {
            throw new ResourceNotFoundException("Không tìm thấy bài học id=" + lessonId);
        }

        String text = lesson.getTextContent();
        String preview = (text == null) ? "" : (text.length() > 150 ? text.substring(0, 150) + "..." : text);

        return LessonPreviewResponse.builder()
                .lessonId(lesson.getLessonId())
                .title(lesson.getTitle())
                .orderIndex(lesson.getOrderIndex())
                .preview(preview)
                .hasVideoOrDocument(lesson.getContentUrl() != null && !lesson.getContentUrl().isBlank())
                .build();
    }

    private LessonResponse toResponse(Lesson l) {
        return LessonResponse.builder()
                .lessonId(l.getLessonId())
                .courseId(l.getCourse().getCourseId())
                .title(l.getTitle())
                .contentUrl(l.getContentUrl())
                .textContent(l.getTextContent())
                .orderIndex(l.getOrderIndex())
                .isPublished(l.getIsPublished())
                .build();
    }
}