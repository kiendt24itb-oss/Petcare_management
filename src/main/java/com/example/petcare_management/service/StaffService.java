package com.example.petcare_management.service;

import com.example.petcare_management.dto.StaffRequest;
import com.example.petcare_management.dto.StaffResponse;
import java.util.List;

public interface StaffService {

    // 📥 1. Admin tuyển nhân viên mới (Tự sinh mã NV-Axxx + Tự tạo Account login)
    StaffResponse createStaff(StaffRequest request);

    // 📝 2. Admin / Nhân viên tự cập nhật hồ sơ cá nhân
    StaffResponse updateStaff(Integer staffId, StaffRequest request);

    // 📋 3. Lấy danh sách toàn bộ nhân viên trong tiệm (Chỉ dành cho trang Admin)
    List<StaffResponse> getAllStaffs();

    // 🔍 4. Xem chi tiết hồ sơ của 1 nhân viên cụ thể (Admin check)
    StaffResponse getStaffById(Integer staffId);

    // 🌟 5. Khách hàng lọc nhân viên theo chuyên môn (SPA, HEALTH, HOTEL) khi bấm chuyển Tab
    List<StaffResponse> getStaffsByDepartment(String department);

    // 👤 6. Tìm staff theo username (Dùng cho luồng nội bộ hệ thống hoặc các logic cũ nếu cần)
    StaffResponse getStaffByUsername(String username);

    // ================= 🌟 PHẦN BỔ SUNG VÀ CHUẨN HÓA MỚI =================

    // 🔍 7. Tìm kiếm nâng cao cho Admin lọc danh sách nhân viên
    List<StaffResponse> searchStaffs(String keyword, Integer positionId);

    // 🌟 8. HÀM HỢP NHẤT MỚI: Staff tự lấy hồ sơ cá nhân + TỰ ĐỘNG GÁN TÀI KHOẢN (Auto-Link)
    // Thay thế hoàn toàn cho hàm checkAndLinkStaffAccount cũ để tránh lỗi gãy Transaction ngầm.
    StaffResponse getStaffByUsernameOrAutoLink(String username);
}