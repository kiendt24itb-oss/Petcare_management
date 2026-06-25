package com.example.petcare_management.controller;

import com.example.petcare_management.dto.CustomerProfileResponse;
import com.example.petcare_management.dto.CustomerRequest;
import com.example.petcare_management.dto.CustomerResponse;
import com.example.petcare_management.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    /**
     * 📥 API: Khách hàng tự điền hoặc cập nhật thông tin cá nhân của chính mình
     * ĐƯỜNG DẪN: PUT http://localhost:8080/api/customers/profile
     */
    @PutMapping("/profile")
    public ResponseEntity<CustomerResponse> createOrUpdateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CustomerRequest request) {

        if (userDetails == null) {
            return ResponseEntity.status(401).build(); // Trả về Unauthorized nếu mất session/token
        }

        String username = userDetails.getUsername();
        CustomerResponse response = customerService.createOrUpdateProfile(username, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 📤 API: Lấy thông tin cá nhân hiện tại để đổ ngược lên Form (Pre-fill) ở Frontend khi vừa load trang
     * ĐƯỜNG DẪN: GET http://localhost:8080/api/customers/profile
     */
    @GetMapping("/profile")
    public ResponseEntity<CustomerResponse> getProfile(@AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        String username = userDetails.getUsername();
        CustomerResponse response = customerService.getProfileByUsername(username);
        return ResponseEntity.ok(response);
    }

    /**
     * 📅 API: Lấy thông tin Tên, Mã khách hàng kèm danh sách Thú cưng phục vụ Form Đặt Lịch (Booking)
     * ĐƯỜNG DẪN: GET http://localhost:8080/api/customers/booking-info
     */
    @GetMapping("/booking-info")
    public ResponseEntity<CustomerProfileResponse> getBookingInfo(@AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        String username = userDetails.getUsername();
        CustomerProfileResponse response = customerService.getProfileWithPetsByUsername(username);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // 👑 👑 CÁC ENDPOINT BỔ SUNG DÀNH RIÊNG CHO ADMIN QUẢN LÝ KHÁCH HÀNG
    // =========================================================================

    /**
     * 👥 API: Admin bốc toàn bộ danh sách khách hàng trong hệ thống để đổ lên bảng
     * ĐƯỜNG DẪN: GET http://localhost:8080/api/customers
     */
    @GetMapping
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {
        List<CustomerResponse> list = customerService.getAllCustomers();
        return ResponseEntity.ok(list);
    }

    /**
     * 🔍 API: Tìm kiếm khách hàng liên thông đa năng (Mã KH, Tên, Số điện thoại) từ Dashboard cha bắn xuống
     * ĐƯỜNG DẪN: GET http://localhost:8080/api/customers/search?keyword=...
     */
    @GetMapping("/search")
    public ResponseEntity<List<CustomerResponse>> searchCustomers(@RequestParam(value = "keyword", required = false) String keyword) {
        List<CustomerResponse> result = customerService.searchCustomers(keyword);
        return ResponseEntity.ok(result);
    }
}