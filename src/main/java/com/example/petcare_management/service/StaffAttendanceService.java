package com.example.petcare_management.service;

import com.example.petcare_management.dto.AttendanceTodayResponse;
import com.example.petcare_management.dto.AttendanceSummaryDTO;
import com.example.petcare_management.dto.StaffAttendanceDashboardDTO;
import com.example.petcare_management.dto.StaffAttendanceHistoryDTO;
import com.example.petcare_management.entity.StaffAttendance;
import java.util.List;

public interface StaffAttendanceService {
    void autoCheckAbsentForFinishedShifts();
    AttendanceTodayResponse getTodayAttendanceStatus(String username);
    StaffAttendance checkIn(String username);
    StaffAttendance checkOut(String username);
    // Sửa dòng này:
    List<StaffAttendanceHistoryDTO> getAttendanceHistory(String username);    AttendanceSummaryDTO getAdminWidgetsSummary();
    List<StaffAttendanceDashboardDTO> getAllStaffAttendanceDashboard();
    List<StaffAttendanceHistoryDTO> getAttendanceHistoryByStaffId(Integer staffId);

    // 🔥 Hàm quét bù tự động khi xem dữ liệu
    void syncAbsentRecordsUpToYesterday(Integer staffId);
}