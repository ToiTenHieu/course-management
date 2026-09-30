package com.example.course_management.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter @Builder
public class TeacherOverviewResponse {
    private Integer teacherId;
    private String teacherName;
    private Integer totalCourses;
    private Long totalEnrollments;
    private List<CourseStat> courses;

    @Getter @Builder
    public static class CourseStat {
        private Integer courseId;
        private String title;
        private String status;
        private Long enrollmentCount;
        private Long lessonCount;
        private Double averageRating; // null nếu chưa có đánh giá
    }
}