package com.example.course_management.service;

import com.example.course_management.dto.response.*;
import com.example.course_management.entity.*;
import com.example.course_management.exception.*;
import com.example.course_management.repository.*;
import com.example.course_management.security.CustomUserDetails;
import java.time.LocalDateTime;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LessonQuestionService {
  private final LessonQuestionRepository questions;
  private final LessonRepository lessons;
  private final CourseRepository courses;
  private final NotificationRepository notifications;
  private final ContentPolicy policy;

  public LessonQuestionService(
      LessonQuestionRepository questions,
      LessonRepository lessons,
      CourseRepository courses,
      NotificationRepository notifications,
      ContentPolicy policy) {
    this.questions = questions;
    this.lessons = lessons;
    this.courses = courses;
    this.notifications = notifications;
    this.policy = policy;
  }

  @Transactional(readOnly = true)
  public PageResponse<LessonQuestionResponse> list(
      Integer lessonId, int page, int size, CustomUserDetails actor) {
    if (page < 0 || size < 1 || size > 50)
      throw new BadRequestException("Trang hoặc kích thước trang không hợp lệ");
    var lesson = lesson(lessonId);
    access(lesson, actor);
    var result =
        questions.findVisible(
            lessonId,
            policy.manages(lesson.getCourse(), actor),
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "questionId")));
    return new PageResponse<>(
        result.getContent().stream().map(this::response).toList(),
        page,
        size,
        result.getTotalElements(),
        result.getTotalPages());
  }

  @Transactional(readOnly = true)
  public LessonQuestionResponse get(Integer id, CustomUserDetails actor) {
    var q = question(id);
    access(q.getLesson(), actor);
    if (q.getIsHidden() && !policy.manages(q.getLesson().getCourse(), actor)) throw missing();
    return response(q);
  }

  public LessonQuestionResponse ask(Integer lessonId, String body, CustomUserDetails actor) {
    var courseId =
        lessons
            .findCourseId(lessonId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học"));
    lockCourse(courseId);
    var lesson = lesson(lessonId);
    access(lesson, actor);
    if (actor.getUser().getRole() != Role.STUDENT)
      throw new ForbiddenException("Chỉ học viên được đặt câu hỏi");
    var q = new LessonQuestion();
    q.setLesson(lesson);
    q.setStudent(actor.getUser());
    q.setBody(text(body));
    questions.saveAndFlush(q);
    notify(lesson.getCourse().getTeacher(), "Có câu hỏi mới trong bài: " + lesson.getTitle(), q);
    return response(q);
  }

  public LessonQuestionResponse answer(Integer id, String body, CustomUserDetails actor) {
    lockQuestionCourse(id);
    var q = question(id);
    policy.manager(q.getLesson().getCourse(), actor);
    if (q.getIsHidden()) throw new BadRequestException("Hiện lại câu hỏi trước khi trả lời");
    q.setAnswer(text(body));
    q.setAnsweredBy(actor.getUser());
    q.setAnsweredAt(LocalDateTime.now());
    questions.save(q);
    notify(
        q.getStudent(),
        "Câu hỏi của bạn đã được phản hồi trong bài: " + q.getLesson().getTitle(),
        q);
    return response(q);
  }

  public LessonQuestionResponse visibility(Integer id, boolean hidden, CustomUserDetails actor) {
    lockQuestionCourse(id);
    var q = question(id);
    policy.manager(q.getLesson().getCourse(), actor);
    q.setIsHidden(hidden);
    return response(questions.save(q));
  }

  private void access(Lesson lesson, CustomUserDetails actor) {
    policy.visible(lesson.getCourse(), actor);
    if (policy.manages(lesson.getCourse(), actor)) return;
    if (!lesson.getIsPublished()) throw new ResourceNotFoundException("Không tìm thấy bài học");
    if (!policy.enrolled(lesson.getCourse(), actor))
      throw new ForbiddenException("Đăng ký khóa học để tham gia hỏi đáp");
  }

  private void lockQuestionCourse(Integer id) {
    lockCourse(questions.findCourseId(id).orElseThrow(this::missing));
  }

  private void lockCourse(Integer id) {
    courses
        .findLockedById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học"));
  }

  private Lesson lesson(Integer id) {
    return lessons
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học"));
  }

  private LessonQuestion question(Integer id) {
    return questions.findById(id).orElseThrow(this::missing);
  }

  private ResourceNotFoundException missing() {
    return new ResourceNotFoundException("Không tìm thấy câu hỏi");
  }

  private String text(String value) {
    if (value == null || value.isBlank() || value.length() > 5000)
      throw new BadRequestException("Nội dung cần từ 1 đến 5000 ký tự");
    return value.strip();
  }

  private void notify(User recipient, String message, LessonQuestion q) {
    var n = new Notification();
    n.setUser(recipient);
    n.setMessage(message);
    n.setType("LESSON_QUESTION");
    n.setTargetUrl(
        "/course-detail.html?id="
            + q.getLesson().getCourse().getCourseId()
            + "&lessonId="
            + q.getLesson().getLessonId()
            + "&questionId="
            + q.getQuestionId());
    notifications.save(n);
  }

  private LessonQuestionResponse response(LessonQuestion q) {
    return new LessonQuestionResponse(
        q.getQuestionId(),
        q.getLesson().getLessonId(),
        q.getStudent().getUserId(),
        q.getStudent().getFullName(),
        q.getBody(),
        q.getAnswer(),
        q.getAnsweredBy() == null ? null : q.getAnsweredBy().getFullName(),
        q.getAnsweredAt(),
        q.getIsHidden(),
        q.getCreatedAt());
  }
}
