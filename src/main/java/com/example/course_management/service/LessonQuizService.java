package com.example.course_management.service;

import com.example.course_management.dto.request.*;
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
public class LessonQuizService {
  private final LessonRepository lessons;
  private final CourseRepository courses;
  private final LessonQuizRepository quizzes;
  private final QuizAttemptRepository attempts;
  private final ContentPolicy policy;

  public LessonQuizService(
      LessonRepository lessons,
      CourseRepository courses,
      LessonQuizRepository quizzes,
      QuizAttemptRepository attempts,
      ContentPolicy policy) {
    this.lessons = lessons;
    this.courses = courses;
    this.quizzes = quizzes;
    this.attempts = attempts;
    this.policy = policy;
  }

  @Transactional(readOnly = true)
  public QuizResponse get(Integer lessonId, CustomUserDetails actor) {
    var lesson = lesson(lessonId);
    access(lesson, actor);
    boolean manager = policy.manages(lesson.getCourse(), actor);
    return latest(lessonId)
        .filter(q -> manager || q.getIsPublished())
        .map(q -> response(q, manager))
        .orElse(null);
  }

  public QuizResponse save(Integer lessonId, SaveQuizRequest r, CustomUserDetails actor) {
    lockCourse(lessonId);
    var lesson = lesson(lessonId);
    policy.manager(lesson.getCourse(), actor);
    int revision = latest(lessonId).map(LessonQuizVersion::getRevision).orElse(0);
    if (revision != r.expectedRevision())
      throw new ConflictException("Quiz đã được sửa. Tải lại đề mới trước khi lưu");
    var version = new LessonQuizVersion();
    version.setLesson(lesson);
    version.setRevision(revision + 1);
    version.setTitle(r.title().strip());
    version.setPassPercentage(r.passPercentage());
    version.setIsPublished(r.published());
    for (int i = 0; i < r.questions().size(); i++) {
      var data = r.questions().get(i);
      var options = data.options().stream().map(String::strip).toList();
      if (options.stream().map(s -> s.toLowerCase(Locale.ROOT)).distinct().count() != 4)
        throw new BadRequestException("Bốn lựa chọn trong mỗi câu hỏi phải khác nhau");
      var question = new QuizQuestion();
      question.setQuizVersion(version);
      question.setOrderIndex(i);
      question.setPrompt(data.prompt().strip());
      question.setOptionA(options.get(0));
      question.setOptionB(options.get(1));
      question.setOptionC(options.get(2));
      question.setOptionD(options.get(3));
      question.setCorrectIndex(data.correctIndex());
      question.setExplanation(data.explanation().strip());
      version.getQuestions().add(question);
    }
    return response(quizzes.saveAndFlush(version), true);
  }

  public QuizAttemptResponse submit(
      Integer lessonId, SubmitQuizRequest r, CustomUserDetails actor) {
    lockCourse(lessonId);
    var lesson = lesson(lessonId);
    access(lesson, actor);
    if (actor.getUser().getRole() != Role.STUDENT)
      throw new ForbiddenException("Chỉ học viên được nộp quiz");
    var key = UUID.fromString(r.submissionKey()).toString();
    var previous = attempts.findByStudent_UserIdAndSubmissionKey(actor.getUser().getUserId(), key);
    if (previous.isPresent()) {
      var attempt = previous.get();
      if (!attempt.getQuizVersion().getLesson().getLessonId().equals(lessonId)
          || !attempt.getQuizVersion().getQuizVersionId().equals(r.quizVersionId())
          || !orderedAnswers(attempt).stream()
              .map(QuizAnswer::getSelectedIndex)
              .toList()
              .equals(r.answers()))
        throw new ConflictException("Mã lần làm đã dùng cho một bài làm khác");
      return result(attempt);
    }
    var quiz =
        latest(lessonId)
            .filter(LessonQuizVersion::getIsPublished)
            .orElseThrow(() -> new ResourceNotFoundException("Bài học chưa có quiz được xuất bản"));
    if (!quiz.getQuizVersionId().equals(r.quizVersionId()))
      throw new ConflictException("Đề quiz đã thay đổi. Tải đề mới trước khi nộp");
    if (r.answers().size() != quiz.getQuestions().size())
      throw new BadRequestException("Cần chọn một đáp án cho mỗi câu hỏi");
    var attempt = new QuizAttempt();
    attempt.setQuizVersion(quiz);
    attempt.setStudent(actor.getUser());
    attempt.setSubmissionKey(key);
    int correct = 0;
    for (int i = 0; i < quiz.getQuestions().size(); i++) {
      var question = quiz.getQuestions().get(i);
      int selected = r.answers().get(i);
      if (selected < 0 || selected > 3) throw new BadRequestException("Lựa chọn phải từ 0 đến 3");
      if (question.getCorrectIndex() == selected) correct++;
      var answer = new QuizAnswer();
      answer.setAttempt(attempt);
      answer.setQuizQuestion(question);
      answer.setSelectedIndex(selected);
      attempt.getAnswers().add(answer);
    }
    int count = quiz.getQuestions().size();
    attempt.setCorrectCount(correct);
    attempt.setQuestionCount(count);
    // Compare exact fractions for passing; displayed scores use integer truncation.
    attempt.setScore(correct * 100 / count);
    attempt.setPassed(correct * 100 >= quiz.getPassPercentage() * count);
    return result(attempts.saveAndFlush(attempt));
  }

  @Transactional(readOnly = true)
  public PageResponse<QuizAttemptResponse.Summary> history(
      Integer lessonId, int page, int size, CustomUserDetails actor) {
    access(lesson(lessonId), actor);
    if (actor.getUser().getRole() != Role.STUDENT)
      throw new ForbiddenException("Lịch sử này dành cho học viên");
    if (page < 0 || size < 1 || size > 50)
      throw new BadRequestException("Trang hoặc kích thước trang không hợp lệ");
    var result =
        attempts.findByStudent_UserIdAndQuizVersion_Lesson_LessonId(
            actor.getUser().getUserId(),
            lessonId,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "attemptId")));
    return new PageResponse<>(
        result.getContent().stream().map(this::summary).toList(),
        page,
        size,
        result.getTotalElements(),
        result.getTotalPages());
  }

  @Transactional(readOnly = true)
  public QuizAttemptResponse attempt(Integer id, CustomUserDetails actor) {
    var attempt =
        attempts
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lần làm"));
    var lesson = attempt.getQuizVersion().getLesson();
    if (!policy.manages(lesson.getCourse(), actor)
        && !attempt.getStudent().getUserId().equals(actor.getUser().getUserId()))
      throw new ForbiddenException("Bạn không có quyền xem bài làm này");
    access(lesson, actor);
    return result(attempt);
  }

  @Transactional(readOnly = true)
  public QuizStatisticsResponse statistics(Integer lessonId, CustomUserDetails actor) {
    policy.manager(lesson(lessonId).getCourse(), actor);
    var quiz =
        latest(lessonId).orElseThrow(() -> new ResourceNotFoundException("Bài học chưa có quiz"));
    var totals = attempts.summarize(quiz.getQuizVersionId()).getFirst();
    Map<Integer, Object[]> counts = new HashMap<>();
    for (var row : attempts.questionStatistics(quiz.getQuizVersionId()))
      counts.put(((Number) row[0]).intValue(), row);
    var rows =
        quiz.getQuestions().stream()
            .map(
                q -> {
                  var values = counts.get(q.getQuizQuestionId());
                  return new QuizStatisticsResponse.Question(
                      q.getQuizQuestionId(),
                      q.getPrompt(),
                      values == null ? 0 : ((Number) values[1]).longValue(),
                      values == null ? 0 : ((Number) values[2]).longValue());
                })
            .toList();
    return new QuizStatisticsResponse(
        quiz.getQuizVersionId(),
        quiz.getRevision(),
        ((Number) totals[0]).longValue(),
        ((Number) totals[1]).longValue(),
        totals[2] == null ? 0 : ((Number) totals[2]).longValue(),
        totals[3] == null ? null : ((Number) totals[3]).doubleValue(),
        rows);
  }

  private void lockCourse(Integer lessonId) {
    var id =
        lessons
            .findCourseId(lessonId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học"));
    courses
        .findLockedById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học"));
  }

  private Lesson lesson(Integer id) {
    return lessons
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học"));
  }

  private Optional<LessonQuizVersion> latest(Integer id) {
    return quizzes.findFirstByLesson_LessonIdOrderByRevisionDesc(id);
  }

  private void access(Lesson lesson, CustomUserDetails actor) {
    policy.visible(lesson.getCourse(), actor);
    if (policy.manages(lesson.getCourse(), actor)) return;
    if (!lesson.getIsPublished()) throw new ResourceNotFoundException("Không tìm thấy bài học");
    if (!policy.enrolled(lesson.getCourse(), actor))
      throw new ForbiddenException("Đăng ký khóa học để làm quiz");
  }

  private List<String> options(QuizQuestion q) {
    return List.of(q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD());
  }

  private QuizResponse response(LessonQuizVersion q, boolean manager) {
    return new QuizResponse(
        q.getQuizVersionId(),
        q.getLesson().getLessonId(),
        q.getRevision(),
        q.getTitle(),
        q.getPassPercentage(),
        q.getIsPublished(),
        q.getQuestions().stream()
            .map(
                question ->
                    new QuizResponse.Question(
                        question.getQuizQuestionId(),
                        question.getPrompt(),
                        options(question),
                        manager ? question.getCorrectIndex() : null,
                        manager ? question.getExplanation() : null))
            .toList());
  }

  private QuizAttemptResponse.Summary summary(QuizAttempt a) {
    var q = a.getQuizVersion();
    return new QuizAttemptResponse.Summary(
        a.getAttemptId(),
        q.getLesson().getLessonId(),
        q.getQuizVersionId(),
        q.getRevision(),
        q.getTitle(),
        a.getCorrectCount(),
        a.getQuestionCount(),
        a.getScore(),
        q.getPassPercentage(),
        a.getPassed(),
        a.getSubmittedAt());
  }

  private List<QuizAnswer> orderedAnswers(QuizAttempt a) {
    return a.getAnswers().stream()
        .sorted(Comparator.comparing(b -> b.getQuizQuestion().getOrderIndex()))
        .toList();
  }

  private QuizAttemptResponse result(QuizAttempt a) {
    return new QuizAttemptResponse(
        summary(a),
        orderedAnswers(a).stream()
            .map(
                answer -> {
                  var q = answer.getQuizQuestion();
                  return new QuizAttemptResponse.Feedback(
                      q.getPrompt(),
                      options(q),
                      answer.getSelectedIndex(),
                      q.getCorrectIndex(),
                      q.getCorrectIndex().equals(answer.getSelectedIndex()),
                      q.getExplanation());
                })
            .toList());
  }
}
