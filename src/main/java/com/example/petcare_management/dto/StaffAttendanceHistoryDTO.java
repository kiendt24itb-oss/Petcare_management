package com.example.petcare_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StaffAttendanceHistoryDTO {
    private String attendanceDate; // Định dạng chuỗi "YYYY-MM-DD"
    private String checkInTime;    // Định dạng chuỗi "HH:mm:ss"
    private String checkOutTime;   // Định dạng chuỗi "HH:mm:ss"
    private String totalHours;     // Số giờ làm
    private String status;         // Trạng thái: ON_TIME, LATE, EARLY, ABSENT
    private String shiftName;      // Tên ca: CA_SANG, CA_CHIEU
}