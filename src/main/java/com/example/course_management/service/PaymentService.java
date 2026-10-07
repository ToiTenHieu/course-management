package com.example.course_management.service;

import com.example.course_management.dto.response.BankInfoResponse;
import com.example.course_management.dto.response.PaymentResponse;
import com.example.course_management.entity.PaymentStatus;
import com.example.course_management.security.CustomUserDetails;
import java.util.List;

public interface PaymentService {
  BankInfoResponse getBankInfo();

  PaymentResponse createPayment(Integer courseId, CustomUserDetails actor);

  List<PaymentResponse> getMyPayments(CustomUserDetails actor);

  List<PaymentResponse> getAllPayments(PaymentStatus status);

  PaymentResponse confirmPayment(Integer paymentId, CustomUserDetails actor);

  PaymentResponse rejectPayment(Integer paymentId, CustomUserDetails actor);
}
