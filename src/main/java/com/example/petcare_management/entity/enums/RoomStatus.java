package com.example.petcare_management.entity.enums;

public enum RoomStatus {
    AVAILABLE,   // 🟢 Còn trống
    BUSY,        // 🔴 Có khách ở / Đang khám
    BOOKED,      // 🟠 Đã được chọn gán gói dịch vụ (MỚI THÊM VÀO ĐÂY)
    MAINTENANCE  // 🛠️ Bảo trì
}