package com.example.course_management.service.impl;

import com.example.course_management.dto.request.*;
import com.example.course_management.dto.response.CourseResponse;
import com.example.course_management.dto.response.LessonSummaryResponse;
import com.example.course_management.entity.*;
import com.example.course_management.exception.BadRequestException;
import com.example.course_management.exception.ResourceNotFoundException;
import com.example.course_management.repository.CourseRepository;
import com.example.course_management.repository.LessonRepository;
import com.example.course_management.repository.UserRepository;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.CourseService;
import org.springframework.stereotype.Service;
import com.example.course_management.dto.response.LessonSummaryResponse;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;

    public CourseServiceImpl(CourseRepository courseRepository, UserRepository userRepository, LessonRepository lessonRepository) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.lessonRepository = lessonRepository;
    }

    @Override
    public List<CourseResponse> getCourses(
            String search,
            Integer teacherId,
            CourseStatus status,
            CustomUserDetails actor) {

        boolean isAdmin = actor.getUser().getRole() == Role.ADMIN;

        // STUDENT/TEACHER chỉ được thấy PUBLISHED
        CourseStatus effectiveStatus =
                isAdmin ? status : CourseStatus.PUBLISHED;

        // Không để search = null
        String effectiveSearch = search == null
                ? ""
                : search.trim();

        return courseRepository.search(
                        effectiveStatus,
                        teacherId,
                        effectiveSearch
                ).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public CourseResponse getCourseById(Integer courseId, CustomUserDetails actor) {
        Course course = findCourseOrThrow(courseId);
        boolean isAdmin = actor.getUser().getRole() == Role.ADMIN;
        boolean isOwnerTeacher = course.getTeacher().getUserId().equals(actor.getUser().getUserId());

        if (!isAdmin && !isOwnerTeacher && course.getStatus() != CourseStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Không tìm thấy khóa học id=" + courseId);
        }

        CourseResponse response = toResponse(course);
        List<com.example.course_management.entity.Lesson> lessons =
                lessonRepository.findByCourse_CourseIdAndIsPublishedTrueOrderByOrderIndex(courseId);
        response.setLessons(lessons.stream()
                .map(l -> LessonSummaryResponse.builder()
                        .lessonId(l.getLessonId())
                        .title(l.getTitle())
                        .orderIndex(l.getOrderIndex())
                        .build())
                .toList());
        return response;
    }

    @Override
    public CourseResponse createCourse(CreateCourseRequest req) {
        User teacher = validateTeacher(req.getTeacherId());

        Course course = new Course();
        course.setTitle(req.getTitle());
        course.setDescription(req.getDescription());
        course.setTeacher(teacher);
        course.setPrice(req.getPrice() != null ? req.getPrice() : java.math.BigDecimal.ZERO);
        course.setDurationHours(req.getDurationHours());
        course.setStatus(CourseStatus.DRAFT); // luật nghiệp vụ endpoint 12: trạng thái ban đầu luôn DRAFT
        course.setCreatedAt(LocalDateTime.now());
        course.setUpdatedAt(LocalDateTime.now());

        return toResponse(courseRepository.save(course));
    }

    @Override
    public CourseResponse updateCourse(Integer courseId, UpdateCourseRequest req) {
        Course course = findCourseOrThrow(courseId);
        User teacher = validateTeacher(req.getTeacherId());

        course.setTitle(req.getTitle());
        course.setDescription(req.getDescription());
        course.setTeacher(teacher);
        if (req.getPrice() != null) course.setPrice(req.getPrice());
        course.setDurationHours(req.getDurationHours());
        course.setUpdatedAt(LocalDateTime.now());

        return toResponse(courseRepository.save(course));
    }

    @Override
    public CourseResponse updateStatus(Integer courseId, UpdateCourseStatusRequest req) {
        Course course = findCourseOrThrow(courseId);
        course.setStatus(req.getStatus());
        course.setUpdatedAt(LocalDateTime.now());
        return toResponse(courseRepository.save(course));
    }

    @Override
    public void deleteCourse(Integer courseId) {
        Course course = findCourseOrThrow(courseId);
        courseRepository.delete(course);
    }

    private User validateTeacher(Integer teacherId) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user id=" + teacherId));
        if (teacher.getRole() != Role.TEACHER) {
            throw new BadRequestException("teacherId phải là user có role TEACHER");
        }
        return teacher;
    }

    private Course findCourseOrThrow(Integer courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học id=" + courseId));
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
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
                .build();
    }
}