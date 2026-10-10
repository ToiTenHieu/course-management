package com.example.course_management.controller;

import com.example.course_management.dto.response.*;
import com.example.course_management.service.AuditLogService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/audit-logs") @PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {
  private final AuditLogService service;
  public AuditLogController(AuditLogService service) {this.service=service;}
  @GetMapping public ApiResponse<PageResponse<AuditLogService.Row>> get(
      @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue="") String action,@RequestParam(defaultValue="") String search,
      @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
    return ApiResponse.success("OK",service.list(from,to,action,search,page,size));
  }
}
