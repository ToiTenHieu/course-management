package com.example.course_management.service;

import com.example.course_management.entity.*;
import com.example.course_management.repository.*;
import com.example.course_management.exception.*;
import com.example.course_management.security.CustomUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class CurriculumService {
  public record Group(Integer chapterId, @NotNull @Size(max=255) String title,
      @NotNull @Size(max=2000) List<@NotNull Integer> lessonIds) {}
  public record Plan(@NotNull @Min(0) Integer expectedRevision,
      @NotNull @Size(min=1,max=101) List<@Valid Group> groups) {}
  public record Curriculum(int revision, List<Group> groups) {}
  private final CourseRepository courses;
  private final CourseChapterRepository chapters;
  private final LessonRepository lessons;
  private final ContentPolicy policy;
  public CurriculumService(CourseRepository courses, CourseChapterRepository chapters,
      LessonRepository lessons, ContentPolicy policy) {
    this.courses=courses; this.chapters=chapters; this.lessons=lessons; this.policy=policy;
  }
  public Curriculum get(int courseId, CustomUserDetails actor) {
    var course = courses.findLockedById(courseId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học"));
    policy.manager(course,actor);
    return response(course);
  }
  public Curriculum save(int courseId, Plan plan, CustomUserDetails actor) {
    var course = courses.findLockedById(courseId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học"));
    policy.manager(course,actor);
    if (!plan.expectedRevision().equals(course.getCurriculumRevision()))
      throw new ConflictException("Chương trình đã thay đổi. Tải lại trước khi sắp xếp; bản sắp xếp hiện tại vẫn được giữ trên trang.");
    var existing = new HashMap<Integer,CourseChapter>();
    chapters.findByCourse_CourseIdOrderByOrderIndexAscChapterIdAsc(courseId).forEach(c -> existing.put(c.getChapterId(),c));
    var lessonMap = new HashMap<Integer,Lesson>();
    lessons.findByCourse_CourseIdOrderByOrderIndex(courseId).forEach(l -> lessonMap.put(l.getLessonId(),l));
    var seen = new HashSet<Integer>(); var chapterIds = new HashSet<Integer>();
    int ungrouped=0;
    for (var group:plan.groups()) {
      if (group.title().isBlank()) {
        if (group.chapterId()!=null || ++ungrouped>1) throw new BadRequestException("Chỉ có một nhóm bài chưa chia chương");
      } else if (group.chapterId()!=null && (!existing.containsKey(group.chapterId()) || !chapterIds.add(group.chapterId())))
        throw new BadRequestException("Chương không thuộc khóa hoặc bị lặp");
      for (var id:group.lessonIds()) if (!lessonMap.containsKey(id) || !seen.add(id))
        throw new BadRequestException("Bài học không thuộc khóa hoặc bị lặp");
    }
    if (!seen.equals(lessonMap.keySet())) throw new BadRequestException("Chương trình phải chứa đúng tất cả bài học của khóa");
    int chapterOrder=0, lessonOrder=0;
    for (var group:plan.groups()) {
      CourseChapter chapter=null;
      if (!group.title().isBlank()) {
        chapter=group.chapterId()==null ? new CourseChapter() : existing.get(group.chapterId());
        chapter.setCourse(course); chapter.setTitle(group.title().strip()); chapter.setOrderIndex(++chapterOrder);
        chapter=chapters.saveAndFlush(chapter);
        chapterIds.add(chapter.getChapterId());
      }
      for (var id:group.lessonIds()) {
        var l=lessonMap.get(id); l.setChapter(chapter); l.setOrderIndex(++lessonOrder);
        l.setContentRevision(l.getContentRevision()+1);
      }
    }
    lessons.saveAllAndFlush(lessonMap.values());
    for (var c:existing.values()) if (!chapterIds.contains(c.getChapterId())) chapters.delete(c);
    course.setCurriculumRevision(course.getCurriculumRevision()+1);
    return response(course);
  }
  private Curriculum response(Course course) {
    var list=lessons.findByCourse_CourseIdOrderByOrderIndex(course.getCourseId());
    var groups=new ArrayList<Group>();
    for (var c:chapters.findByCourse_CourseIdOrderByOrderIndexAscChapterIdAsc(course.getCourseId()))
      groups.add(new Group(c.getChapterId(),c.getTitle(),list.stream().filter(l -> l.getChapter()!=null && l.getChapter().getChapterId().equals(c.getChapterId())).map(Lesson::getLessonId).toList()));
    groups.add(new Group(null,"",list.stream().filter(l -> l.getChapter()==null).map(Lesson::getLessonId).toList()));
    return new Curriculum(course.getCurriculumRevision(),groups);
  }
}
