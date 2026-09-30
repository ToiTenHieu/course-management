package com.example.course_management.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class BankInfoResponse {
    private String bankName;
    private String accountNumber;
    private String accountHolder;
}