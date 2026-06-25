package com.example.petcare_management.service;

import com.example.petcare_management.dto.CustomerProfileResponse;
import com.example.petcare_management.dto.CustomerRequest;
import com.example.petcare_management.dto.CustomerResponse;
import java.util.List;

public interface CustomerService {
    // Cho Khách hàng tự xử lý Profile của mình
    CustomerResponse createOrUpdateProfile(String username, CustomerRequest request);
    CustomerResponse getProfileByUsername(String username);
    CustomerProfileResponse getProfileWithPetsByUsername(String username);

    // 👑 TÍNH NĂNG THÊM MỚI CHO ADMIN QUẢN LÝ
    List<CustomerResponse> getAllCustomers(); // Lấy toàn bộ danh sách đổ lên bảng
    List<CustomerResponse> searchCustomers(String keyword); // Phục vụ ô tìm kiếm Dashboard cha
}