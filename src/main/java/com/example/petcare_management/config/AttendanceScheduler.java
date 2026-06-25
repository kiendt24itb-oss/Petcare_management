/*
package com.example.petcare_management.config;

import com.example.petcare_management.service.StaffAttendanceService;
import com.example.petcare_management.service.StaffLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class AttendanceScheduler {

    private final StaffAttendanceService attendanceService;
    private final StaffLogService staffLogService;

    */
/**
     * 🌅 1. Cron Job: Chào ngày mới hệ thống (08:00:00 sáng hằng ngày)
     *//*

    @Scheduled(cron = "0 0 8 * * ?")
    public void welcomeNewDay() {
        System.out.println("🌅 [HỆ THỐNG] Xin chào ngày mới!");
        try {
            staffLogService.addLog(1, "🌅 [HỆ THỐNG] Cửa hàng PetCare mở cửa phiên làm việc mới. Chúc đội ngũ một ngày làm việc tràn đầy năng lượng! 🐾");
        } catch (Exception e) {
            System.err.println("🚨 Lỗi ghi log chào ngày mới: " + e.getMessage());
        }
    }

    */
/**
     * 🕒 2. Cron Job: Quét và tự động chốt sổ vắng mặt thực tế (Mỗi tiếng một lần)
     *//*

    @Scheduled(cron = "0 5 * * * ?")
    public void scanAndCheckAbsent() {
        try {
            attendanceService.autoCheckAbsentForFinishedShifts();
        } catch (Exception e) {
            System.err.println("🚨 [Cron Job] Gặp lỗi khi tự động chốt vắng mặt: " + e.getMessage());
        }
    }

    */
/**
     * 🚀 3. HÀM ĐẶC BIỆT: TEST TỰ ĐỘNG CHẤM CÔNG BÙ QUÁ KHỨ VÀO LÚC 12:02:00 TRƯA NAY
     * Ý nghĩa Cron: Giây 0, Phút 2, Giờ 12, Ngày *, Tháng *, Thứ ?
     *//*

    // 🔥 Đã sửa mốc chạy thành 12:12:00 trưa nay để ní test nóng luôn!
    @Scheduled(cron = "0 12 12 * * ?")
    public void testAutoCheckAbsentCompensation() {
        System.out.println("=========================================================");
        System.out.println("🚀 [🔥 HỆ THỐNG CRON JOB] Bắt đầu tự động quét chấm công bù từ ngày 01/06...");
        System.out.println("=========================================================");

        try {
            // 1. Đút một câu log thông báo phiên quét bù lên Timeline Frontend
            staffLogService.addLog(1, "⚙️ [HỆ THỐNG] Kích hoạt phiên quét tự động và chấm công bù cho các ngày vắng mặt trong tháng! 🐾");

            // 2. Kích hoạt hàm quét bù (Thay số 7 bằng ID nhân viên ní đang dùng để test nha)
            attendanceService.syncAbsentRecordsUpToYesterday(7);

            System.out.println("✅ [HỆ THỐNG] Chúc mừng ní! Đã tự động chấm công bù hoàn tất, không còn lỗi trùng khóa DB nữa!");
            System.out.println("=========================================================");
        } catch (Exception e) {
            System.err.println("🚨 [HỆ THỐNG TEST BÙ] Gặp lỗi nghiêm trọng: " + e.getMessage());
        }
    }
}*/

package com.example.petcare_management.config;

import com.example.petcare_management.service.StaffAttendanceService;
import com.example.petcare_management.service.StaffLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AttendanceScheduler {

    private final StaffAttendanceService attendanceService;
    private final StaffLogService staffLogService;

    /**
     * 🌅 1. Cron Job: Chào ngày mới hệ thống (Đúng 08:00:00 sáng hằng ngày)
     * Thức dậy khi ca ngày bắt đầu, bắn câu chào lên Timeline Admin.
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void welcomeNewDay() {
        System.out.println("🌅 [HỆ THỐNG] Xin chào ngày mới!");
        try {
            staffLogService.addLog(1, "🌅 [HỆ THỐNG] Cửa hàng PetCare mở cửa phiên làm việc mới. Chúc đội ngũ một ngày làm việc tràn đầy năng lượng! 🐾");
        } catch (Exception e) {
            System.err.println("🚨 Lỗi ghi log chào ngày mới: " + e.getMessage());
        }
    }

    /**
     * 🕒 2. Cron Job: Quét tự động chốt sổ vắng mặt thực tế (Chạy vào phút thứ 05 của mỗi tiếng)
     * Quét liên tục trong ngày để đứa nào bỏ ca là chốt ABSENT luôn.
     */
    @Scheduled(cron = "0 5 * * * ?")
    public void scanAndCheckAbsent() {
        try {
            attendanceService.autoCheckAbsentForFinishedShifts();
        } catch (Exception e) {
            System.err.println("🚨 [Cron Job] Gặp lỗi khi tự động chốt vắng mặt: " + e.getMessage());
        }
    }

    /**
     * 🌌 3. Cron Job: Kết thúc phiên làm việc trong ngày (Đúng 22:00:00 đêm hằng ngày)
     * Chạy khi ca tối kết thúc. Chốt sổ toàn bộ ca cuối ngày và ghi nhận đóng phiên.
     */
    @Scheduled(cron = "0 0 22 * * ?")
    public void goodbyeEndDay() {
        System.out.println("🌌 [HỆ THỐNG] Đang tiến hành đóng phiên và chốt sổ toàn bộ ca làm việc lúc 22:00...");
        try {
            // 1. Ghi log kết thúc ngày làm việc lên Timeline Dashboard
            staffLogService.addLog(1, "🌌 [HỆ THỐNG] Cửa hàng PetCare chính thức đóng cửa phiên làm việc ngày hôm nay. Chúc Admin và đội ngũ ngủ ngon! ✨");

            // 2. Ép chốt công ca tối (hoặc các ca còn sót) của ngày hôm nay thành ABSENT nếu chưa check-in
            attendanceService.autoCheckAbsentForFinishedShifts();

            System.out.println("✅ [HỆ THỐNG] Đã tự động đóng phiên làm việc ngày hôm nay thành công!");
        } catch (Exception e) {
            System.err.println("🚨 Lỗi chốt sổ cuối ngày lúc 22:00: " + e.getMessage());
        }
    }
}