package com.example.course_management.service.impl;

import com.example.course_management.dto.response.BankInfoResponse;
import com.example.course_management.dto.response.PaymentResponse;
import com.example.course_management.entity.*;
import com.example.course_management.exception.BadRequestException;
import com.example.course_management.exception.ConflictException;
import com.example.course_management.exception.ResourceNotFoundException;
import com.example.course_management.repository.CourseRepository;
import com.example.course_management.repository.EnrollmentRepository;
import com.example.course_management.repository.PaymentRepository;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.PaymentService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

  private final com.example.course_management.repository.UserRepository users;
  private final com.example.course_management.repository.NotificationRepository notifications;

  private final com.example.course_management.config.LearningSettings settings;

  private final PaymentRepository paymentRepository;
  private final CourseRepository courseRepository;
  private final EnrollmentRepository enrollmentRepository;

  public PaymentServiceImpl(
      PaymentRepository paymentRepository,
      CourseRepository courseRepository,
      EnrollmentRepository enrollmentRepository,
      com.example.course_management.repository.UserRepository users,
      com.example.course_management.repository.NotificationRepository notifications,
      com.example.course_management.config.LearningSettings settings) {
    this.users = users;
    this.notifications = notifications;
    this.paymentRepository = paymentRepository;
    this.courseRepository = courseRepository;
    this.enrollmentRepository = enrollmentRepository;
    this.settings = settings;
  }

  @Override
  public BankInfoResponse getBankInfo() {
    var bank = settings.current();
    return BankInfoResponse.builder()
        .bankName(bank.bankName())
        .bankBin(bank.bankBin())
        .accountNumber(bank.bankAccount())
        .accountHolder(bank.bankHolder())
        .build();
  }

  @Override
  public PaymentResponse createPayment(Integer courseId, CustomUserDetails actor) {
    users
        .findLockedById(actor.getUser().getUserId())
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học viên"));
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy khóa học id=" + courseId));

    if (course.getStatus() != CourseStatus.PUBLISHED) {
      throw new BadRequestException("Chỉ có thể thanh toán cho khóa học đã xuất bản");
    }
    if (course.getPrice() == null || course.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
      throw new BadRequestException(
          "Khóa học này miễn phí, không cần thanh toán — hãy đăng ký trực tiếp");
    }

    Integer studentId = actor.getUser().getUserId();

    if (enrollmentRepository.existsByStudent_UserIdAndCourse_CourseId(studentId, courseId)) {
      throw new ConflictException("Bạn đã đăng ký khóa học này rồi");
    }
    if (paymentRepository.existsByStudent_UserIdAndCourse_CourseIdAndStatusIn(
        studentId, courseId, List.of(PaymentStatus.PENDING, PaymentStatus.CONFIRMED))) {
      throw new ConflictException("Bạn đã có yêu cầu thanh toán cho khóa học này rồi");
    }

    Payment payment = new Payment();
    payment.setStudent(actor.getUser());
    payment.setCourse(course);
    payment.setAmount(course.getPrice());
    payment.setStatus(PaymentStatus.PENDING);
    payment.setCreatedAt(LocalDateTime.now());
    payment = paymentRepository.save(payment);

    // Sinh mã nội dung chuyển khoản sau khi có paymentId, để đảm bảo duy nhất và dễ đối chiếu
    payment.setTransferNote("TT" + String.format("%06d", payment.getPaymentId()));
    payment = paymentRepository.save(payment);

    return toResponse(payment);
  }

  @Override
  public List<PaymentResponse> getMyPayments(CustomUserDetails actor) {
    return paymentRepository
        .findByStudent_UserIdOrderByCreatedAtDesc(actor.getUser().getUserId())
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Override
  public List<PaymentResponse> getAllPayments(PaymentStatus status) {
    return paymentRepository.search(status).stream().map(this::toResponse).toList();
  }

  @Override
  public PaymentResponse confirmPayment(Integer paymentId, CustomUserDetails actor) {
    Integer studentId = findStudentIdOrThrow(paymentId);
    users
        .findLockedById(studentId)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học viên"));
    Payment payment =
        paymentRepository
            .findLockedById(paymentId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thanh toán"));
    if (payment.getStatus() != PaymentStatus.PENDING) {
      throw new BadRequestException("Yêu cầu thanh toán này đã được xử lý trước đó");
    }

    if (enrollmentRepository.existsByStudent_UserIdAndCourse_CourseId(
        payment.getStudent().getUserId(), payment.getCourse().getCourseId()))
      throw new ConflictException("Học viên đã được cấp quyền học khóa này");
    payment.setStatus(PaymentStatus.CONFIRMED);
    payment.setConfirmedAt(LocalDateTime.now());
    payment.setConfirmedBy(actor.getUser());
    paymentRepository.save(payment);

    // Xác nhận thanh toán xong -> tự động tạo Enrollment cho sinh viên
    Enrollment enrollment = new Enrollment();
    enrollment.setStudent(payment.getStudent());
    enrollment.setCourse(payment.getCourse());
    enrollment.setEnrollmentDate(LocalDateTime.now());
    enrollment.setStatus(EnrollmentStatus.ENROLLED);
    enrollment.setProgressPercentage(BigDecimal.ZERO);
    enrollmentRepository.saveAndFlush(enrollment);
    Notification notification = new Notification();
    notification.setUser(payment.getStudent());
    notification.setType("ENROLLMENT_CONFIRMED");
    notification.setMessage(
        "Thanh toán đã được xác nhận. Bạn có thể bắt đầu học " + payment.getCourse().getTitle());
    notification.setTargetUrl("/course-detail.html?id=" + payment.getCourse().getCourseId());
    notifications.save(notification);

    return toResponse(payment);
  }

  @Override
  public PaymentResponse rejectPayment(Integer paymentId, CustomUserDetails actor) {
    Integer studentId = findStudentIdOrThrow(paymentId);
    users
        .findLockedById(studentId)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học viên"));
    Payment payment =
        paymentRepository
            .findLockedById(paymentId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thanh toán"));
    if (payment.getStatus() != PaymentStatus.PENDING) {
      throw new BadRequestException("Yêu cầu thanh toán này đã được xử lý trước đó");
    }
    payment.setStatus(PaymentStatus.REJECTED);
    payment.setConfirmedAt(LocalDateTime.now());
    payment.setConfirmedBy(actor.getUser());
    return toResponse(paymentRepository.save(payment));
  }

  private Integer findStudentIdOrThrow(Integer id) {
    // Chỉ đọc ID trước khi khóa: không đưa Payment cũ vào persistence context khi chờ khóa.
    return paymentRepository
        .findStudentIdByPaymentId(id)
        .orElseThrow(
            () -> new ResourceNotFoundException("Không tìm thấy yêu cầu thanh toán id=" + id));
  }

  private PaymentResponse toResponse(Payment p) {
    return PaymentResponse.builder()
        .paymentId(p.getPaymentId())
        .courseId(p.getCourse().getCourseId())
        .courseTitle(p.getCourse().getTitle())
        .studentId(p.getStudent().getUserId())
        .studentName(p.getStudent().getFullName())
        .amount(p.getAmount())
        .status(p.getStatus().name())
        .transferNote(p.getTransferNote())
        .createdAt(p.getCreatedAt())
        .confirmedAt(p.getConfirmedAt())
        .build();
  }
}
