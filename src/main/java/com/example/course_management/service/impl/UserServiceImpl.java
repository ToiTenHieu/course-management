package com.example.course_management.service.impl;

import com.example.course_management.dto.request.*;
import com.example.course_management.dto.response.UserResponse;
import com.example.course_management.entity.Role;
import com.example.course_management.entity.User;
import com.example.course_management.exception.ConflictException;
import com.example.course_management.exception.ForbiddenException;
import com.example.course_management.exception.ResourceNotFoundException;
import com.example.course_management.repository.UserRepository;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.UserService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public List<UserResponse> getUsers(Role role, Boolean isActive) {
    return userRepository.search(role, isActive).stream().map(this::toResponse).toList();
  }

  @Override
  public UserResponse getUserById(Integer userId) {
    return toResponse(findUserOrThrow(userId));
  }

  @Override
  public UserResponse createUser(CreateUserRequest req) {
    if (userRepository.existsByUsername(req.getUsername())) {
      throw new ConflictException("Username đã tồn tại");
    }
    if (userRepository.existsByEmail(req.getEmail())) {
      throw new ConflictException("Email đã tồn tại");
    }

    User user = new User();
    user.setUsername(req.getUsername());
    user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
    user.setEmail(req.getEmail());
    user.setFullName(req.getFullName());
    user.setRole(req.getRole());
    user.setIsActive(true);
    user.setCreatedAt(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());

    return toResponse(userRepository.save(user));
  }

  @Override
  public UserResponse updateRole(Integer userId, UpdateRoleRequest req) {
    User user = lockedUserOrThrow(userId);

    // Luật nghiệp vụ endpoint 7: ADMIN không được đổi role của một ADMIN khác
    if (user.getRole() == Role.ADMIN) {
      throw new ForbiddenException("Không được phép thay đổi role của một ADMIN khác");
    }

    user.setRole(req.getRole());
    user.setAuthVersion(user.getAuthVersion() + 1);
    user.setUpdatedAt(LocalDateTime.now());
    return toResponse(userRepository.save(user));
  }

  @Override
  public UserResponse updateStatus(Integer userId, UpdateStatusRequest req) {
    User user = lockedUserOrThrow(userId);
    user.setIsActive(req.getIsActive());
    user.setAuthVersion(user.getAuthVersion() + 1);
    user.setUpdatedAt(LocalDateTime.now());
    return toResponse(userRepository.save(user));
  }

  @Override
  public void deleteUser(Integer userId) {
    User user = findUserOrThrow(userId);
    userRepository.delete(user);
  }

  @Override
  public UserResponse updateProfile(Integer userId, UpdateUserRequest req) {
    User user = lockedUserOrThrow(userId);
    if (!user.getEmail().equals(req.getEmail()) && userRepository.existsByEmail(req.getEmail())) {
      throw new com.example.course_management.exception.ConflictException("Email đã tồn tại");
    }
    user.setFullName(req.getFullName());
    user.setEmail(req.getEmail());
    user.setUpdatedAt(LocalDateTime.now());
    return toResponse(userRepository.save(user));
  }

  @Override
  public UserResponse manageUser(Integer userId, ManageUserRequest req) {
    var user = lockedUserOrThrow(userId);
    // Profile and role must succeed together, including the rule protecting administrators.
    if (user.getRole() == Role.ADMIN && req.getRole() != Role.ADMIN)
      throw new ForbiddenException("Không được phép thay đổi vai trò của quản trị viên");
    updateProfile(userId, req);
    if (user.getRole() != req.getRole()) {
      var role = new UpdateRoleRequest();
      role.setRole(req.getRole());
      return updateRole(userId, role);
    }
    return toResponse(user);
  }

  @Override
  public void changePassword(Integer userId, ChangePasswordRequest req, CustomUserDetails actor) {
    User user = lockedUserOrThrow(userId);

    boolean isSelf = actor.getUser().getUserId().equals(userId);
    // Nếu tự đổi mật khẩu của chính mình (không phải admin đổi hộ) → bắt buộc xác nhận mật khẩu cũ
    if (isSelf) {
      if (req.getOldPassword() == null
          || !passwordEncoder.matches(req.getOldPassword(), user.getPasswordHash())) {
        throw new ForbiddenException("Mật khẩu hiện tại không đúng");
      }
    }

    user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
    user.setAuthVersion(user.getAuthVersion() + 1);
    user.setUpdatedAt(LocalDateTime.now());
    userRepository.save(user);
  }

  private User findUserOrThrow(Integer userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user id=" + userId));
  }

  private User lockedUserOrThrow(Integer userId) {
    return userRepository.findLockedById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
  }

  private UserResponse toResponse(User u) {
    return UserResponse.builder()
        .userId(u.getUserId())
        .username(u.getUsername())
        .email(u.getEmail())
        .fullName(u.getFullName())
        .role(u.getRole().name())
        .isActive(u.getIsActive())
        .createdAt(u.getCreatedAt())
        .build();
  }
}
