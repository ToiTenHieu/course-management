package com.example.course_management.service;

import com.example.course_management.dto.response.CourseResponse;
import com.example.course_management.dto.response.PageResponse;
import com.example.course_management.entity.CourseStatus;
import com.example.course_management.entity.CourseWishlist;
import com.example.course_management.entity.Role;
import com.example.course_management.exception.*;
import com.example.course_management.repository.CourseRepository;
import com.example.course_management.repository.CourseWishlistRepository;
import com.example.course_management.security.CustomUserDetails;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CourseWishlistService {
  private final CourseWishlistRepository wishlist;
  private final CourseRepository courses;
  private final CourseService courseService;

  public CourseWishlistService(CourseWishlistRepository wishlist, CourseRepository courses, CourseService courseService) {
    this.wishlist = wishlist;
    this.courses = courses;
    this.courseService = courseService;
  }

  @Transactional(readOnly = true)
  public PageResponse<CourseResponse> list(String search, int page, int size, CustomUserDetails actor) {
    var studentId = studentId(actor);
    if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE)
      throw new BadRequestException("Trang phải từ 0 và kích thước trang từ 1 đến 100");
    if (search.length() > 255) throw new BadRequestException("Từ khóa không được vượt quá 255 ký tự");
    String pattern = search.isBlank() ? "" : "%" + search.trim().toLowerCase(Locale.ROOT)
        .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    var result = wishlist.findVisible(studentId, CourseStatus.PUBLISHED, pattern, PageRequest.of(page, size));
    return new PageResponse<>(courseService.summarizeCourses(result.getContent().stream()
        .map(CourseWishlist::getCourse).toList()), page, size, result.getTotalElements(), result.getTotalPages());
  }

  @Transactional(readOnly = true)
  public List<Integer> savedIds(List<Integer> ids, CustomUserDetails actor) {
    var studentId = studentId(actor);
    if (ids.size() > 100 || ids.stream().anyMatch(id -> id == null || id < 1))
      throw new BadRequestException("Chỉ kiểm tra tối đa 100 mã khóa học hợp lệ mỗi lần");
    return ids.isEmpty() ? List.of() : wishlist.savedIds(studentId, ids, CourseStatus.PUBLISHED);
  }

  public void save(Integer courseId, CustomUserDetails actor) {
    var studentId = studentId(actor);
    // Serialize saves/removals for the same course; retrying a PUT never creates a second row.
    var course = courses.findLockedById(courseId)
        .filter(c -> c.getStatus() == CourseStatus.PUBLISHED)
        .orElseThrow(() -> new ResourceNotFoundException("Khóa học hiện không được mở"));
    if (wishlist.existsByStudent_UserIdAndCourse_CourseId(studentId, courseId)) return;
    var entry = new CourseWishlist();
    entry.setCourse(course);
    entry.setStudent(actor.getUser());
    wishlist.save(entry);
  }

  public void remove(Integer courseId, CustomUserDetails actor) {
    var studentId = studentId(actor);
    // An archived or deleted course can still be removed, and repeated DELETEs succeed.
    courses.findLockedById(courseId);
    wishlist.remove(studentId, courseId);
  }

  private Integer studentId(CustomUserDetails actor) {
    if (actor == null || actor.getUser().getRole() != Role.STUDENT)
      throw new ForbiddenException("Khóa quan tâm dành cho học viên");
    return actor.getUser().getUserId();
  }
}
