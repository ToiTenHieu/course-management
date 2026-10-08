package com.example.course_management.exception;

import com.example.course_management.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
  public ResponseEntity<ApiResponse<Void>> integrity(Exception ex) {
    return ResponseEntity.status(409)
        .body(
            ApiResponse.error(
                "Dữ liệu bị trùng hoặc đang được sử dụng. Vui lòng tải lại và kiểm tra"));
  }

  @ExceptionHandler({
    org.springframework.http.converter.HttpMessageNotReadableException.class,
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<ApiResponse<Void>> invalidInput(Exception ex) {
    return ResponseEntity.badRequest().body(ApiResponse.error("Dữ liệu hoặc tham số không hợp lệ"));
  }

  @ExceptionHandler(org.springframework.dao.PessimisticLockingFailureException.class)
  public ResponseEntity<ApiResponse<Void>> busy(Exception ex) {
    return ResponseEntity.status(409)
        .body(ApiResponse.error("Dữ liệu đang được xử lý. Vui lòng thử lại"));
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ex.getMessage()));
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ApiResponse<Void>> handleAuthException(AuthenticationException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ApiResponse.error("Sai username hoặc password"));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
    String message = ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
    return ResponseEntity.badRequest().body(ApiResponse.error(message));
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ApiResponse<Void>> handleConflict(ConflictException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(ex.getMessage()));
  }

  @ExceptionHandler(ForbiddenException.class)
  public ResponseEntity<ApiResponse<Void>> handleForbidden(ForbiddenException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(ex.getMessage()));
  }

  @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
  public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
      org.springframework.security.access.AccessDeniedException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ApiResponse.error("Bạn không có quyền thực hiện thao tác này"));
  }

  @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
  public ResponseEntity<ApiResponse<Void>> uploadTooLarge(Exception ex) {
    return ResponseEntity.status(413).body(ApiResponse.error("Mỗi tài liệu tối đa 5 MB"));
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ApiResponse<Void>> handleBadRequest(BadRequestException ex) {
    return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
  }
}
