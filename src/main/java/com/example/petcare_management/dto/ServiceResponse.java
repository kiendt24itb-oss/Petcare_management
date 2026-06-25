package com.example.petcare_management.dto;

import com.example.petcare_management.entity.enums.ServiceCategory;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceResponse {
    private Integer serviceId;
    private String serviceCode;       // DV-H001, DV-S001...
    private String serviceName;
    private ServiceCategory category;
    private BigDecimal price;
    private Integer durationMinutes;

    // 🚪 Trả về mã phòng phẳng lỳ để JS quét trực tiếp srv.roomCode
    private String roomCode;

    private String description;
    private Boolean status;           // Biết dịch vụ này còn mở hay đã đóng
}