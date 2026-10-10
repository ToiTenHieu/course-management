package com.example.course_management.service.impl;

import com.example.course_management.time.ApplicationTime;
import com.example.course_management.dto.request.*;
import com.example.course_management.dto.response.*;
import com.example.course_management.entity.*;
import com.example.course_management.exception.*;
import com.example.course_management.repository.*;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.*;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LessonServiceImpl implements LessonService {
  private final LessonRepository lessons;
  private final CourseRepository courses;
  private final LessonProgressRepository progress;
  private final ContentPolicy policy;
  private final ProgressCalculator calculator;
  private final LessonDraftService drafts;

  public LessonServiceImpl(
      LessonRepository lessons,
      CourseRepository courses,
      LessonProgressRepository progress,
      ContentPolicy policy,
      ProgressCalculator calculator, LessonDraftService drafts) {
    this.lessons = lessons;
    this.courses = courses;
    this.progress = progress;
    this.policy = policy;
    this.calculator = calculator;
    this.drafts = drafts;
  }

  public List<LessonResponse> getLessonsByCourse(Integer id, CustomUserDetails actor) {
    var c = course(id);
    policy.visible(c, actor);
    boolean manager = policy.manages(c, actor);
    boolean full = manager || policy.enrolled(c, actor);
    var list =
        manager
            ? lessons.findByCourse_CourseIdOrderByOrderIndex(id)
            : lessons.findByCourse_CourseIdAndIsPublishedTrueOrderByOrderIndex(id);
    return list.stream().map(l -> response(l, full)).toList();
  }

  public LessonResponse getLessonById(Integer id, CustomUserDetails actor) {
    var l = lesson(id);
    visible(l, actor);
    if (!policy.manages(l.getCourse(), actor) && !policy.enrolled(l.getCourse(), actor))
      throw new ForbiddenException("Đăng ký khóa học để xem nội dung đầy đủ");
    return response(l, true);
  }

  public LessonResponse createLesson(
      Integer courseId, CreateLessonRequest r, CustomUserDetails actor) {
    var c = lockedCourse(courseId);
    policy.manager(c, actor);
    policy.contentUrl(r.getContentUrl());
    policy.contentUrl(r.getVideoUrl());
    var l = new Lesson();
    l.setCourse(c);
    l.setTitle(r.getTitle().trim());
    l.setContentUrl(r.getContentUrl());
    l.setTextContent(r.getTextContent());
    if (r.getContentFormat() != null) l.setContentFormat(r.getContentFormat());
    l.setVideoUrl(r.getVideoUrl());
    l.setOrderIndex(r.getOrderIndex());
    c.setCurriculumRevision(c.getCurriculumRevision() + 1);
    drafts.consume(courseId, 0, r.getDraftRevision(), actor);
    return response(lessons.save(l), true);
  }

  public LessonResponse updateLesson(Integer id, UpdateLessonRequest r, CustomUserDetails actor) {
    var courseId = lessons.findCourseId(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học"));
    var c = lockedCourse(courseId);
    policy.manager(c, actor);
    var l = lesson(id);
    if (r.getExpectedRevision() != null && !r.getExpectedRevision().equals(l.getContentRevision()))
      throw new ConflictException("Bài học đã được cập nhật. Dùng nội dung hiện tại hoặc mở lại trình soạn trước khi lưu; bản nháp của bạn vẫn được giữ.");
    drafts.consume(courseId, id, r.getDraftRevision(), actor);
    drafts.snapshot(l, actor);
    l.setContentRevision(l.getContentRevision() + 1);
    policy.contentUrl(r.getContentUrl());
    policy.contentUrl(r.getVideoUrl());
    l.setTitle(r.getTitle().trim());
    l.setContentUrl(r.getContentUrl());
    l.setTextContent(r.getTextContent());
    if (r.getContentFormat() != null) l.setContentFormat(r.getContentFormat());
    l.setVideoUrl(r.getVideoUrl());
    l.setOrderIndex(r.getOrderIndex());
    c.setCurriculumRevision(c.getCurriculumRevision() + 1);
    l.setUpdatedAt(ApplicationTime.now());
    return response(lessons.save(l), true);
  }

  public LessonResponse updatePublishStatus(
      Integer id, UpdateLessonPublishRequest r, CustomUserDetails actor) {
    var courseId = lessons.findCourseId(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học"));
    var c = lockedCourse(courseId);
    policy.manager(c, actor);
    var l = lesson(id);
    l.setIsPublished(r.getIsPublished());
    l.setUpdatedAt(ApplicationTime.now());
    lessons.saveAndFlush(l);
    calculator.recalculateCourse(l.getCourse().getCourseId());
    return response(l, true);
  }

  public void deleteLesson(Integer id, CustomUserDetails actor) {
    var l = lesson(id);
    var c = lockedCourse(l.getCourse().getCourseId());
    policy.manager(c, actor);
    c.setCurriculumRevision(c.getCurriculumRevision() + 1);
    progress.deleteByLesson_LessonId(id);
    progress.flush();
    lessons.delete(l);
    lessons.flush();
    calculator.recalculateCourse(c.getCourseId());
  }

  public LessonPreviewResponse getContentPreview(Integer id, CustomUserDetails actor) {
    var l = lesson(id);
    visible(l, actor);
    var text = l.getTextContent() == null ? "" : l.getTextContent();
    return LessonPreviewResponse.builder()
        .lessonId(id)
        .title(l.getTitle())
        .orderIndex(l.getOrderIndex())
        .chapterId(l.getChapter() == null ? null : l.getChapter().getChapterId())
        .chapterTitle(l.getChapter() == null ? null : l.getChapter().getTitle())
        .preview(text.length() > 150 ? text.substring(0, 150) + "…" : text)
        .hasVideoOrDocument(
            (l.getContentUrl() != null && !l.getContentUrl().isBlank())
                || (l.getVideoUrl() != null && !l.getVideoUrl().isBlank()))
        .build();
  }

  private void visible(Lesson l, CustomUserDetails actor) {
    policy.visible(l.getCourse(), actor);
    if (!Boolean.TRUE.equals(l.getIsPublished()) && !policy.manages(l.getCourse(), actor))
      throw new ResourceNotFoundException("Không tìm thấy bài học");
  }

  private Course course(Integer id) {
    return courses
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học"));
  }

  private Course lockedCourse(Integer id) {
    return courses
        .findLockedById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học"));
  }

  private Lesson lesson(Integer id) {
    return lessons
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học"));
  }

  private LessonResponse response(Lesson l, boolean full) {
    return LessonResponse.builder()
        .lessonId(l.getLessonId())
        .courseId(l.getCourse().getCourseId())
        .title(l.getTitle())
        .contentUrl(full ? l.getContentUrl() : null)
        .textContent(full ? l.getTextContent() : null)
        .contentFormat(full ? l.getContentFormat() : null)
        .videoUrl(full ? l.getVideoUrl() : null)
        .orderIndex(l.getOrderIndex())
        .chapterId(l.getChapter() == null ? null : l.getChapter().getChapterId())
        .chapterTitle(l.getChapter() == null ? null : l.getChapter().getTitle())
        .contentRevision(full ? l.getContentRevision() : null)
        .isPublished(l.getIsPublished())
        .build();
  }
}
