package com.example.course_management.dto.response;

import com.example.course_management.entity.Role;
import java.time.LocalDateTime;

public record QuestionReplyResponse(
    Integer replyId, Integer questionId, Integer authorId, String authorName,
    Role authorRole, String body, boolean hidden, LocalDateTime createdAt) {}
