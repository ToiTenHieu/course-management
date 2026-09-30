package com.example.course_management.exception;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) { super(message); }
}