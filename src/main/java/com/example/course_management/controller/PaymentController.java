package com.example.course_management.controller;

import com.example.course_management.dto.request.CreatePaymentRequest;
import com.example.course_management.dto.response.ApiResponse;
import com.example.course_management.dto.response.BankInfoResponse;
import com.example.course_management.dto.response.PaymentResponse;
import com.example.course_management.entity.PaymentStatus;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // Thông tin tài khoản để chuyển khoản — AUTH
    @GetMapping("/bank-info")
    public ApiResponse<BankInfoResponse> getBankInfo() {
        return ApiResponse.success("OK", paymentService.getBankInfo());
    }

    // STUDENT tạo yêu cầu thanh toán cho 1 khóa học có phí
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<PaymentResponse> create(
            @Valid @RequestBody CreatePaymentRequest request,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ApiResponse.success("Đã tạo yêu cầu thanh toán, vui lòng chuyển khoản theo hướng dẫn",
                paymentService.createPayment(request.getCourseId(), actor));
    }

    // STUDENT xem lịch sử thanh toán của chính mình
    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<PaymentResponse>> getMine(@AuthenticationPrincipal CustomUserDetails actor) {
        return ApiResponse.success("OK", paymentService.getMyPayments(actor));
    }

    // ADMIN xem toàn bộ yêu cầu thanh toán, lọc theo trạng thái
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<PaymentResponse>> getAll(@RequestParam(required = false) PaymentStatus status) {
        return ApiResponse.success("OK", paymentService.getAllPayments(status));
    }

    // ADMIN xác nhận đã nhận tiền -> tự động tạo Enrollment
    @PutMapping("/{paymentId}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PaymentResponse> confirm(
            @PathVariable Integer paymentId,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ApiResponse.success("Đã xác nhận thanh toán và kích hoạt khóa học",
                paymentService.confirmPayment(paymentId, actor));
    }

    // ADMIN từ chối (chuyển khoản sai, không khớp...)
    @PutMapping("/{paymentId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PaymentResponse> reject(
            @PathVariable Integer paymentId,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ApiResponse.success("Đã từ chối yêu cầu thanh toán",
                paymentService.rejectPayment(paymentId, actor));
    }
}