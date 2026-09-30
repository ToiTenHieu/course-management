package com.example.course_management.service.impl;

import com.example.course_management.dto.response.*;
import com.example.course_management.entity.*;
import com.example.course_management.exception.BadRequestException;
import com.example.course_management.exception.ResourceNotFoundException;
import com.example.course_management.repository.*;
import com.example.course_management.service.ReportService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {

    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final ReviewRepository reviewRepository;

    public ReportServiceImpl(EnrollmentRepository enrollmentRepository, UserRepository userRepository,
                             CourseRepository courseRepository, LessonRepository lessonRepository,
                             ReviewRepository reviewRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
        this.reviewRepository = reviewRepository;
    }

    @Override
    public List<TopCourseResponse> getTopCourses(int limit) {
        return enrollmentRepository.findTopCourses(PageRequest.of(0, Math.max(1, limit))).stream()
                .map(row -> TopCourseResponse.builder()
                        .courseId((Integer) row[0])
                        .title((String) row[1])
                        .enrollmentCount((Long) row[2])
                        .build())
                .toList();
    }

    @Override
    public StudentProgressReport getStudentProgress(Integer studentId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user id=" + studentId));
        if (student.getRole() != Role.STUDENT) {
            throw new BadRequestException("User này không phải sinh viên");
        }

        List<Enrollment> enrollments = enrollmentRepository.findByStudent_UserId(studentId);
        int completed = (int) enrollments.stream().filter(e -> e.getStatus() == EnrollmentStatus.COMPLETED).count();
        double avg = enrollments.stream()
                .map(Enrollment::getProgressPercentage)
                .mapToDouble(BigDecimal::doubleValue)
                .average().orElse(0.0);

        List<EnrollmentResponse> courses = enrollments.stream().map(e -> EnrollmentResponse.builder()
                .enrollmentId(e.getEnrollmentId())
                .courseId(e.getCourse().getCourseId())
                .courseTitle(e.getCourse().getTitle())
                .enrollmentDate(e.getEnrollmentDate())
                .status(e.getStatus().name())
                .progressPercentage(e.getProgressPercentage())
                .build()).toList();

        return StudentProgressReport.builder()
                .studentId(studentId)
                .studentName(student.getFullName())
                .totalEnrollments(enrollments.size())
                .completedCount(completed)
                .averageProgress(Math.round(avg * 100.0) / 100.0)
                .courses(courses)
                .build();
    }

    @Override
    public TeacherOverviewResponse getTeacherOverview(Integer teacherId) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user id=" + teacherId));
        if (teacher.getRole() != Role.TEACHER) {
            throw new BadRequestException("User này không phải giảng viên");
        }

        List<TeacherOverviewResponse.CourseStat> stats = courseRepository.findByTeacher_UserId(teacherId).stream()
                .map(c -> TeacherOverviewResponse.CourseStat.builder()
                        .courseId(c.getCourseId())
                        .title(c.getTitle())
                        .status(c.getStatus().name())
                        .enrollmentCount(enrollmentRepository.countByCourse_CourseId(c.getCourseId()))
                        .lessonCount(lessonRepository.countByCourse_CourseId(c.getCourseId()))
                        .averageRating(reviewRepository.averageRating(c.getCourseId()))
                        .build())
                .toList();

        long totalEnrollments = stats.stream().mapToLong(TeacherOverviewResponse.CourseStat::getEnrollmentCount).sum();

        return TeacherOverviewResponse.builder()
                .teacherId(teacherId)
                .teacherName(teacher.getFullName())
                .totalCourses(stats.size())
                .totalEnrollments(totalEnrollments)
                .courses(stats)
                .build();
    }
}