package com.example.petcare_management.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice // Đánh dấu đây là bộ bắt lỗi tập trung cho toàn bộ Controller
public class GlobalExceptionHandler {

    // 1. Bắt lỗi khi không tìm thấy tài nguyên (Sai ID khách, sai ID phòng...)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleResourceNotFound(ResourceNotFoundException ex) {
        Map<String, String> errors = new HashMap<>();
        errors.put("error", "Not Found");
        errors.put("message", ex.getMessage()); // Lấy câu Tiếng Việt mình viết ở Service
        return new ResponseEntity<>(errors, HttpStatus.NOT_FOUND);
    }

    // 2. Bắt các lỗi xử lý logic nghiệp vụ chung (Hủy lịch sát giờ, tài khoản bị khóa, trùng username...)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        Map<String, String> errors = new HashMap<>();
        errors.put("error", "Bad Request");
        errors.put("message", ex.getMessage()); // Lấy câu Tiếng Việt thông báo lỗi nghiệp vụ
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }
}