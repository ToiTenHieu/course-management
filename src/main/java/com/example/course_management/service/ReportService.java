package com.example.course_management.service;

import com.example.course_management.dto.response.*;
import java.util.List;

public interface ReportService {
    List<TopCourseResponse> getTopCourses(int limit);
    StudentProgressReport getStudentProgress(Integer studentId);
    TeacherOverviewResponse getTeacherOverview(Integer teacherId);
}