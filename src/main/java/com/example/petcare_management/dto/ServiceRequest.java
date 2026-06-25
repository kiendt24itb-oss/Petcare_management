package com.example.petcare_management.dto;

import com.example.petcare_management.entity.enums.ServiceCategory;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceRequest {
    private String serviceName;
    private ServiceCategory category; // HEALTH, SPA, HOTEL
    private BigDecimal price;
    private Integer durationMinutes;  // Thời gian ước tính làm ca dịch vụ

    // 🚪 Nhận mã phòng chỉ định từ FE chọn trên Form (Ví dụ: "R-O001")
    private String roomCode;

    private String description;

    @Builder.Default
    private Boolean status = true;    // Mặc định tạo mới là TRUE (Đang kinh doanh)
}