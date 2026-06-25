package com.example.petcare_management.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffAttendanceDashboardDTO {
    private Integer staffId;
    private String todayCheckIn;        // Giờ check-in hôm nay (Ví dụ: "08:05" hoặc "--:--")
    private String todayStatus;         // Trạng thái hôm nay (Ví dụ: "ON_TIME", "LATE", "ABSENT", hoặc "CHUA_CHECKIN")
    private Long totalLateInMonth;      // Tổng số ngày đi muộn của nhân viên này trong tháng
    private Long totalAbsentInMonth;    // Tổng số ngày vắng mặt của nhân viên này trong tháng
}