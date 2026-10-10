package com.example.course_management.service;

import com.example.course_management.time.ApplicationTime;
import com.example.course_management.dto.request.SaveTeacherProfileRequest;
import com.example.course_management.dto.response.TeacherProfileResponse;
import com.example.course_management.entity.Role;
import com.example.course_management.entity.User;
import com.example.course_management.exception.ResourceNotFoundException;
import com.example.course_management.exception.ForbiddenException;
import com.example.course_management.repository.UserRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TeacherProfileService {
  private final UserRepository users;

  public TeacherProfileService(UserRepository users) {
    this.users = users;
  }

  @Transactional(readOnly = true)
  public TeacherProfileResponse getPublicProfile(Integer teacherId) {
    var teacher = users.findById(teacherId)
        .filter(u -> u.getRole() == Role.TEACHER && Boolean.TRUE.equals(u.getIsActive()))
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giảng viên"));
    return response(teacher);
  }

  public TeacherProfileResponse save(Integer teacherId, SaveTeacherProfileRequest request) {
    var teacher = users.findLockedById(teacherId)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giảng viên"));
    if (teacher.getRole() != Role.TEACHER || !Boolean.TRUE.equals(teacher.getIsActive()))
      throw new ForbiddenException("Chỉ giảng viên đang hoạt động có thể sửa hồ sơ công khai");
    if (request.getBiography() != null) teacher.setBiography(request.getBiography().trim());
    if (request.getExpertise() != null) teacher.setExpertise(request.getExpertise().trim());
    teacher.setUpdatedAt(ApplicationTime.now());
    return response(users.save(teacher));
  }

  private TeacherProfileResponse response(User teacher) {
    return new TeacherProfileResponse(teacher.getUserId(), teacher.getFullName(),
        teacher.getBiography(), teacher.getExpertise());
  }
}
