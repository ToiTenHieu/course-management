package com.example.course_management.controller;

import com.example.course_management.dto.response.*;
import com.example.course_management.service.ReportService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasRole('ADMIN')")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    // Endpoint 37
    @GetMapping("/top_courses")
    public ApiResponse<List<TopCourseResponse>> topCourses(@RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.success("OK", reportService.getTopCourses(limit));
    }

    // Endpoint 38
    @GetMapping("/student_progress/{studentId}")
    public ApiResponse<StudentProgressReport> studentProgress(@PathVariable Integer studentId) {
        return ApiResponse.success("OK", reportService.getStudentProgress(studentId));
    }

    // Endpoint 39
    @GetMapping("/teacher_courses_overview/{teacherId}")
    public ApiResponse<TeacherOverviewResponse> teacherOverview(@PathVariable Integer teacherId) {
        return ApiResponse.success("OK", reportService.getTeacherOverview(teacherId));
    }
}