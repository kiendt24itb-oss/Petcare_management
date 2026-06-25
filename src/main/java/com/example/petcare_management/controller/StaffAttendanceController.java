package com.example.petcare_management.controller;

import com.example.petcare_management.dto.AttendanceTodayResponse;
import com.example.petcare_management.dto.AttendanceSummaryDTO;
import com.example.petcare_management.dto.StaffAttendanceDashboardDTO;
import com.example.petcare_management.dto.StaffAttendanceHistoryDTO;
import com.example.petcare_management.service.StaffAttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StaffAttendanceController {

    private final StaffAttendanceService attendanceService;

    /**
     * 🕒 Lấy trạng thái điểm danh hôm nay của Nhân viên
     * (Kích hoạt quét bù vắng mặt ngầm trong quá khứ khi truy cập)
     */
    @GetMapping("/today")
    public ResponseEntity<AttendanceTodayResponse> getTodayStatus(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(attendanceService.getTodayAttendanceStatus(userDetails.getUsername()));
    }

    /**
     * 🟢 Ghi nhận vào ca làm việc (Check-in)
     */
    @PostMapping("/check-in")
    public ResponseEntity<Map<String, String>> doCheckIn(@AuthenticationPrincipal UserDetails userDetails) {
        attendanceService.checkIn(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "🎉 Check-in thành công!"));
    }

    /**
     * 🔴 Ghi nhận kết thúc ca làm việc (Check-out)
     */
    @PostMapping("/check-out")
    public ResponseEntity<Map<String, String>> doCheckOut(@AuthenticationPrincipal UserDetails userDetails) {
        attendanceService.checkOut(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "🎉 Check-out thành công!"));
    }

    /**
     * 📜 Lấy lịch sử chấm công của chính Nhân viên đang đăng nhập
     * 🛠️ ĐÃ NÂNG CẤP: Trả về StaffAttendanceHistoryDTO thay vì Entity để chặn lỗi vòng lặp JSON
     * (Kích hoạt quét bù vắng mặt ngầm khi xem)
     */
    @GetMapping("/history")
    public ResponseEntity<List<StaffAttendanceHistoryDTO>> getHistory(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(attendanceService.getAttendanceHistory(userDetails.getUsername()));
    }

    // =========================================================================
    // 📊 CÁC ENDPOINTS DÀNH CHO QUẢN TRỊ VIÊN (ADMIN MANAGEMENT)
    // =========================================================================

    /**
     * 🎛️ Lấy số liệu tổng hợp cho các Widget thống kê của Admin
     */
    @GetMapping("/admin/widgets-summary")
    public ResponseEntity<AttendanceSummaryDTO> getAdminWidgets() {
        return ResponseEntity.ok(attendanceService.getAdminWidgetsSummary());
    }

    /**
     * 📈 Lấy trạng thái chấm công của toàn bộ nhân viên hiển thị trên Dashboard Admin
     */
    @GetMapping("/admin/dashboard-status")
    public ResponseEntity<List<StaffAttendanceDashboardDTO>> getAdminDashboardStatus() {
        return ResponseEntity.ok(attendanceService.getAllStaffAttendanceDashboard());
    }

    /**
     * 🚨 Chủ động kích hoạt chạy quét chốt vắng mặt bằng tay cho các ca đã kết thúc hôm nay
     */
    @PostMapping("/admin/trigger-absent")
    public ResponseEntity<Map<String, String>> triggerAbsentCheckManually() {
        try {
            attendanceService.autoCheckAbsentForFinishedShifts();
            return ResponseEntity.ok(Map.of("message", "🎉 Kích hoạt quét và chốt vắng mặt thành công!"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "🚨 Lỗi khi quét: " + e.getMessage()));
        }
    }

    /**
     * 🔍 Admin xem lịch sử chi tiết của một nhân viên bất kỳ qua ID
     * (Kích hoạt quét bù vắng mặt ngầm của nhân viên đó khi Admin nhấn xem)
     */
    @GetMapping("/admin/history/{staffId}")
    public ResponseEntity<List<StaffAttendanceHistoryDTO>> getStaffHistoryForAdmin(@PathVariable("staffId") Integer staffId) {
        return ResponseEntity.ok(attendanceService.getAttendanceHistoryByStaffId(staffId));
    }
}