package com.example.petcare_management.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceSummaryDTO {
    private Long totalStaff;       // Tổng số nhân sự hệ thống
    private Long presentToday;     // Số người đã đi làm hôm nay (Tính cả đúng giờ + đi muộn)
    private Long lateThisMonth;    // Tổng số lượt đi muộn tích lũy của TOÀN HỆ THỐNG trong tháng này
    private Long absentToday;      // Số người vắng mặt hôm nay
}