package com.example.petcare_management.entity.enums;

public enum BookingStatus {
    PENDING_APPROVAL, // 🌟 THÊM: Đơn chờ Admin duyệt tay (Dành cho khách vi phạm >= 3 lần)
    WAITING,          // Đang chờ (Khách đặt lịch thành công hoặc vừa đến tiệm)
    PROCESSING,       // Đang làm
    COMPLETED,        // Hoàn thành
    CANCELLED,        // Đã hủy ❌ (Khách chủ động hủy hoặc nhân viên bấm hủy)
    REJECTED          // 🌟 THÊM: Bị từ chối (Admin chủ động bấm từ chối đơn của khách spam)
}