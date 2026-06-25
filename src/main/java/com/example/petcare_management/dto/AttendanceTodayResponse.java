package com.example.petcare_management.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceTodayResponse {
    private boolean hasCheckIn;
    private boolean hasCheckOut;
    private String checkInTime;
    private String checkOutTime;
    private String shiftName;          // 🔥 Thêm cái này: Trả về "CA_SANG" hoặc "CA_CHIEU"
    private String statusBannerMessage;
    private boolean isExpired; // 🚀 THÊM TRƯỜNG NÀY ĐỂ BÁO HẾT CA
}