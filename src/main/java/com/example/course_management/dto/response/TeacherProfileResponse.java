package com.example.course_management.dto.response;

/** Public fields only: never include contact details or account credentials. */
public record TeacherProfileResponse(
    Integer teacherId, String fullName, String biography, String expertise) {}
