package com.example.course_management.service;

import com.example.course_management.dto.response.*;
import com.example.course_management.entity.*;
import com.example.course_management.exception.*;
import com.example.course_management.repository.*;
import com.example.course_management.security.CustomUserDetails;
import java.util.*;
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
  private final QuestionReplyRepository replies;

  public LessonQuestionService(
      LessonQuestionRepository questions,
      LessonRepository lessons,
      CourseRepository courses,
      NotificationRepository notifications,
      ContentPolicy policy,
      QuestionReplyRepository replies) {
    this.questions = questions;
    this.lessons = lessons;
    this.courses = courses;
    this.notifications = notifications;
    this.policy = policy;
    this.replies = replies;
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
    var counts = new HashMap<Integer, Long>();
    if (!result.isEmpty())
      replies.countVisible(result.getContent().stream().map(LessonQuestion::getQuestionId).toList(),
          policy.manages(lesson.getCourse(), actor)).forEach(c -> counts.put(c.getQuestionId(), c.getTotal()));
    return new PageResponse<>(
        result.getContent().stream().map(q -> response(q, counts.getOrDefault(q.getQuestionId(), 0L))).toList(),
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
    return response(q, replies.countVisible(id, policy.manages(q.getLesson().getCourse(), actor)));
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
    append(q, text(body), null, actor);
    return response(q);
  }

  @Transactional(readOnly = true)
  public PageResponse<QuestionReplyResponse> listReplies(
      Integer id, int page, int size, CustomUserDetails actor) {
    if (page < 0 || size < 1 || size > 50)
      throw new BadRequestException("Trang hoặc kích thước trang không hợp lệ");
    var q = question(id);
    access(q.getLesson(), actor);
    boolean manager = policy.manages(q.getLesson().getCourse(), actor);
    if (q.getIsHidden() && !manager) throw missing();
    var result = replies.findVisible(id, manager,
        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "replyId")));
    return new PageResponse<>(result.getContent().stream().map(this::replyResponse).toList(),
        page, size, result.getTotalElements(), result.getTotalPages());
  }

  public QuestionReplyResponse reply(Integer id, String body, String clientRequestId, CustomUserDetails actor) {
    lockQuestionCourse(id);
    var q = question(id);
    access(q.getLesson(), actor);
    if (q.getIsHidden()) {
      if (!policy.manages(q.getLesson().getCourse(), actor)) throw missing();
      throw new BadRequestException("Hiện lại câu hỏi trước khi trả lời");
    }
    if (actor.getUser().getRole() != Role.STUDENT && !policy.manages(q.getLesson().getCourse(), actor))
      throw new ForbiddenException("Bạn không phụ trách khóa học này");
    String content = text(body);
    if (clientRequestId == null || !clientRequestId.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))
      throw new BadRequestException("Mã gửi phản hồi không hợp lệ");
    String key = clientRequestId.toLowerCase(Locale.ROOT);
    var existing = replies.findByAuthor_UserIdAndClientRequestId(actor.getUser().getUserId(), key);
    if (existing.isPresent()) {
      var saved = existing.get();
      if (!saved.getQuestion().getQuestionId().equals(id) || !saved.getBody().equals(content))
        throw new ConflictException("Mã gửi đã được sử dụng cho phản hồi khác");
      if (saved.getIsHidden() && !policy.manages(q.getLesson().getCourse(), actor)) throw missing();
      return replyResponse(saved);
    }
    return replyResponse(append(q, content, key, actor));
  }

  public QuestionReplyResponse replyVisibility(Integer id, boolean hidden, CustomUserDetails actor) {
    lockCourse(replies.findCourseId(id).orElseThrow(this::missing));
    var r = replies.findById(id).orElseThrow(this::missing);
    var q = r.getQuestion();
    policy.manager(q.getLesson().getCourse(), actor);
    r.setIsHidden(hidden);
    replies.saveAndFlush(r);
    // Compatibility summary and teacher reports must never expose a hidden official message.
    var official = replies.findFirstByQuestion_QuestionIdAndIsHiddenFalseAndAuthorRoleInOrderByReplyIdDesc(
        q.getQuestionId(), List.of(Role.TEACHER, Role.ADMIN)).orElse(null);
    q.setAnswer(official == null ? null : official.getBody());
    q.setAnsweredBy(official == null ? null : official.getAuthor());
    q.setAnsweredAt(official == null ? null : official.getCreatedAt());
    questions.save(q);
    return replyResponse(r);
  }

  private QuestionReply append(LessonQuestion q, String body, String key, CustomUserDetails actor) {
    if (q.getIsHidden()) throw new BadRequestException("Hiện lại câu hỏi trước khi trả lời");
    var r = new QuestionReply();
    r.setQuestion(q);
    r.setAuthor(actor.getUser());
    r.setAuthorRole(actor.getUser().getRole());
    r.setBody(body);
    r.setClientRequestId(key);
    replies.saveAndFlush(r);
    if (policy.manages(q.getLesson().getCourse(), actor)) {
      q.setAnswer(body);
      q.setAnsweredBy(actor.getUser());
      q.setAnsweredAt(r.getCreatedAt());
      questions.save(q);
    }
    var recipients = new LinkedHashMap<Integer, User>();
    recipients.put(q.getStudent().getUserId(), q.getStudent());
    var teacher = q.getLesson().getCourse().getTeacher();
    recipients.put(teacher.getUserId(), teacher);
    recipients.remove(actor.getUser().getUserId());
    recipients.values().stream().filter(u -> Boolean.TRUE.equals(u.getIsActive())).forEach(u ->
        notify(u, "Có phản hồi mới trong bài: " + q.getLesson().getTitle(), q));
    return r;
  }

  private QuestionReplyResponse replyResponse(QuestionReply r) {
    return new QuestionReplyResponse(r.getReplyId(), r.getQuestion().getQuestionId(),
        r.getAuthor().getUserId(), r.getAuthor().getFullName(), r.getAuthorRole(),
        r.getBody(), r.getIsHidden(), r.getCreatedAt());
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
    return response(q, replies.countVisible(q.getQuestionId(), true));
  }

  private LessonQuestionResponse response(LessonQuestion q, long replyCount) {
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
        q.getCreatedAt(),
        replyCount);
  }
}
