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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    public PaymentServiceImpl(PaymentRepository paymentRepository, CourseRepository courseRepository,
                              EnrollmentRepository enrollmentRepository) {
        this.paymentRepository = paymentRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Override
    public BankInfoResponse getBankInfo() {
        // Thông tin ngân hàng cố định — đồ án nhỏ nên khai báo tĩnh, không cần bảng riêng.
        // Muốn đổi số tài khoản, sửa trực tiếp 3 dòng dưới đây.
        return BankInfoResponse.builder()
                .bankName("Ngân hàng TMCP Á Châu (ACB)")
                .accountNumber("123456789")
                .accountHolder("NGUYEN VAN HIEU")
                .build();
    }

    @Override
    public PaymentResponse createPayment(Integer courseId, CustomUserDetails actor) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học id=" + courseId));

        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new BadRequestException("Chỉ có thể thanh toán cho khóa học đã xuất bản");
        }
        if (course.getPrice() == null || course.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Khóa học này miễn phí, không cần thanh toán — hãy đăng ký trực tiếp");
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
        return paymentRepository.findByStudent_UserIdOrderByCreatedAtDesc(actor.getUser().getUserId())
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<PaymentResponse> getAllPayments(PaymentStatus status) {
        return paymentRepository.search(status).stream().map(this::toResponse).toList();
    }

    @Override
    public PaymentResponse confirmPayment(Integer paymentId, CustomUserDetails actor) {
        Payment payment = findOrThrow(paymentId);
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException("Yêu cầu thanh toán này đã được xử lý trước đó");
        }

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
        enrollmentRepository.save(enrollment);

        return toResponse(payment);
    }

    @Override
    public PaymentResponse rejectPayment(Integer paymentId, CustomUserDetails actor) {
        Payment payment = findOrThrow(paymentId);
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException("Yêu cầu thanh toán này đã được xử lý trước đó");
        }
        payment.setStatus(PaymentStatus.REJECTED);
        payment.setConfirmedAt(LocalDateTime.now());
        payment.setConfirmedBy(actor.getUser());
        return toResponse(paymentRepository.save(payment));
    }

    private Payment findOrThrow(Integer id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu thanh toán id=" + id));
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