package com.example.course_management.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter @Builder
public class StudentProgressReport {
    private Integer studentId;
    private String studentName;
    private Integer totalEnrollments;
    private Integer completedCount;
    private Double averageProgress;
    private List<EnrollmentResponse> courses;
}