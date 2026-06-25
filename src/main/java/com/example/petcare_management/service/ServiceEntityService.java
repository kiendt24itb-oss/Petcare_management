package com.example.petcare_management.service;

import com.example.petcare_management.dto.ServiceRequest;
import com.example.petcare_management.dto.ServiceResponse;
import com.example.petcare_management.entity.enums.ServiceCategory;

import java.util.List;

public interface ServiceEntityService {
    // 1. Admin thêm mới dịch vụ (Tự động sinh mã phân loại dựa trên CodeGenerator)
    ServiceResponse createService(ServiceRequest request);

    // 2. Admin cập nhật thông tin hoặc giá tiền dịch vụ
    ServiceResponse updateService(Integer serviceId, ServiceRequest request);

    // 3. Lấy toàn bộ dịch vụ (Cả đang mở lẫn đã đóng - Dành cho Admin quản lý)
    List<ServiceResponse> getAllServices();

    // 4. Lấy danh sách dịch vụ đang mở theo danh mục (Dành cho Khách chọn lịch)
    List<ServiceResponse> getActiveServicesByCategory(ServiceCategory category);

    // 5. Đổi trạng thái kinh doanh (Bật/Tắt dịch vụ thay vì xóa cứng khỏi DB)
    void toggleServiceStatus(Integer serviceId);

    // 🌟 6. Tìm kiếm nâng cao theo Danh mục và Từ khóa tên dịch vụ (Gọn gàng chuẩn mẫu Staff)
    List<ServiceResponse> searchServices(ServiceCategory category, String keyword);

}