package com.example.course_management.controller;

import com.example.course_management.dto.response.ApiResponse;
import com.example.course_management.service.ActivityReportService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/reports/activity") @PreAuthorize("hasRole('ADMIN')")
public class ActivityReportController {
  private final ActivityReportService service;
  public ActivityReportController(ActivityReportService service) { this.service=service; }
  @GetMapping public ApiResponse<ActivityReportService.Report> get(
      @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
    return ApiResponse.success("OK",service.get(from,to,page,size));
  }
  @GetMapping("/export") public ResponseEntity<byte[]> export(
      @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) {
    return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
        .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename("bao-cao-"+from+"-"+to+".csv").build().toString())
        .header("X-Content-Type-Options","nosniff").cacheControl(CacheControl.noStore()).body(service.export(from,to));
  }
}
