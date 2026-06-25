package com.example.petcare_management.controller;

import com.example.petcare_management.dto.StaffRequest;
import com.example.petcare_management.dto.StaffResponse;
import com.example.petcare_management.service.StaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staffs")
@RequiredArgsConstructor
@CrossOrigin("*")
public class StaffController {

    private final StaffService staffService;

    // 📥 1. Admin tuyển nhân viên mới (Tự sinh mã NV-Axxx + Tự tạo Account login)
    @PostMapping
    public ResponseEntity<?> createStaff(@RequestBody StaffRequest request) {
        try {
            StaffResponse response = staffService.createStaff(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 📝 2. Admin / Nhân viên tự cập nhật hồ sơ cá nhân
    @PutMapping("/{staffId}")
    public ResponseEntity<?> updateStaff(
            @PathVariable Integer staffId,
            @RequestBody StaffRequest request) {
        try {
            StaffResponse response = staffService.updateStaff(staffId, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 📋 3. Lấy danh sách toàn bộ nhân viên trong tiệm (Chỉ dành cho trang Admin)
    @GetMapping
    public ResponseEntity<List<StaffResponse>> getAllStaffs() {
        return ResponseEntity.ok(staffService.getAllStaffs());
    }

    // 🌟 6. CẬP NHẬT CHUẨN: Staff tự lấy hồ sơ cá nhân + TỰ ĐỘNG GÁN TÀI KHỎN AN TOÀN TRANSACTION
    // URL: GET http://localhost:8080/api/staffs/me
    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile() {
        try {
            // 🔑 1. Bốc ngầm username từ SecurityContext do Jwt Filter giải mã sẵn
            org.springframework.security.core.Authentication auth =
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = auth.getName();

            // 🎯 2. Đẩy thẳng xuống Service xử lý luồng tích hợp tập trung, tránh lỗi Rollback-only của JPA
            StaffResponse response = staffService.getStaffByUsernameOrAutoLink(currentUsername);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("🚨 Lỗi hồ sơ: " + e.getMessage());
        }
    }

    // 🔍 4. Xem chi tiết hồ sơ của 1 nhân viên cụ thể (Admin check)
    @GetMapping("/{staffId}")
    public ResponseEntity<?> getStaffById(@PathVariable Integer staffId) {
        try {
            StaffResponse response = staffService.getStaffById(staffId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // 🌟 5. Khách hàng lọc nhân viên theo chuyên môn (SPA, HEALTH, HOTEL) khi bấm chuyển Tab
    @GetMapping("/active")
    public ResponseEntity<?> getActiveStaffs(@RequestParam String department) {
        try {
            List<StaffResponse> responses = staffService.getStaffsByDepartment(department);
            return ResponseEntity.ok(responses);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    // ================= 🌟 BỔ SUNG: API TÌM KIẾM NÂNG CAO CHO FRONT-END =================
    // URL mẫu: GET http://localhost:8080/api/staffs/search?keyword=Nguyen&positionId=1
    @GetMapping("/search")
    public ResponseEntity<List<StaffResponse>> searchStaffs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer positionId) {
        List<StaffResponse> responses = staffService.searchStaffs(keyword, positionId);
        return ResponseEntity.ok(responses);
    }
}