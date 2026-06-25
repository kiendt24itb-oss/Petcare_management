package com.example.petcare_management.service;

import com.example.petcare_management.dto.AttendanceSummaryDTO;
import com.example.petcare_management.dto.AttendanceTodayResponse;
import com.example.petcare_management.dto.StaffAttendanceDashboardDTO;
import com.example.petcare_management.dto.StaffAttendanceHistoryDTO;
import com.example.petcare_management.entity.Staff;
import com.example.petcare_management.entity.StaffAttendance;
import com.example.petcare_management.entity.WorkShift;
import com.example.petcare_management.entity.enums.AttendanceStatus;
import com.example.petcare_management.repository.StaffAttendanceRepository;
import com.example.petcare_management.repository.StaffRepository;
import com.example.petcare_management.repository.WorkShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffAttendanceServiceImpl implements StaffAttendanceService {

    private final StaffAttendanceRepository attendanceRepository;
    private final StaffRepository staffRepository;
    private final WorkShiftRepository workShiftRepository;
    private final StaffLogService staffLogService;

    private Staff getStaffByUsername(String username) {
        return staffRepository.findByAccountUsernameWithAccount(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ nhân viên!"));
    }

    /**
     * 🔥 HÀM BỔ SUNG: Tự động quét và điền bù dữ liệu VẮNG MẶT (ABSENT) từ đầu tháng đến hết ngày hôm qua.
     * ĐỒNG THỜI: Ghi log chi tiết rơi vào ĐÚNG NGÀY NGHỈ THỰC TẾ để UI hiển thị lịch sử log chuẩn xác.
     */
    @Override
    @Transactional
    public void syncAbsentRecordsUpToYesterday(Integer staffId) {
        Staff staff = staffRepository.findById(staffId).orElse(null);
        if (staff == null) return;

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.withDayOfMonth(1); // Ngày mùng 1 đầu tháng
        LocalDate yesterday = today.minusDays(1);     // Ngày hôm qua

        if (today.getDayOfMonth() == 1) return;

        // 🎯 BƯỚC 1: Lấy TOÀN BỘ bản ghi chấm công của nhân viên lên RAM để check chéo
        List<StaffAttendance> existingAttendances = attendanceRepository
                .findByStaffStaffIdOrderByAttendanceDateDesc(staff.getStaffId());

        List<WorkShift> allShifts = workShiftRepository.findAll();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // BƯỚC 2: Duyệt tuần tự từ ngày mùng 1 đến hết ngày hôm qua
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(yesterday)) {
            final LocalDate dateToCheck = currentDate;

            for (WorkShift shift : allShifts) {

                // 🎯 BƯỚC 3: So khớp trực tiếp trên RAM để tránh nghẽn DB
                boolean isAlreadyRecorded = existingAttendances.stream().anyMatch(att ->
                        att.getAttendanceDate().equals(dateToCheck) &&
                                att.getWorkShift().getShiftId().equals(shift.getShiftId())
                );

                // Nếu trống lịch (chưa đi làm & chưa chốt vắng) -> Lưu bản ghi ABSENT
                if (!isAlreadyRecorded) {
                    StaffAttendance absentRecord = StaffAttendance.builder()
                            .staff(staff)
                            .workShift(shift)
                            .attendanceDate(dateToCheck)
                            .checkInTime(null)
                            .checkOutTime(null)
                            .status(AttendanceStatus.ABSENT)
                            .totalHours(BigDecimal.ZERO)
                            .note("Hệ thống bù: Vắng mặt (0.5 công)")
                            .build();

                    // Lưu trực tiếp xuống DB để hiển thị ở bảng theo dõi chấm công
                    attendanceRepository.save(absentRecord);

                    // Cập nhật ngược lại danh sách RAM để vòng lặp sau không bị check trùng
                    existingAttendances.add(absentRecord);

                    // ✅ ĐÃ XOÁ LOG DB - CHỈ IN CONSOLE ĐỂ THEO DÕI HỆ THỐNG
                    String formattedDate = dateToCheck.format(dateFormatter);
                    System.out.println("🔄 [Đồng bộ thành công] Đã bù ca vắng ngày " + formattedDate + " cho NV: " + staff.getFullName());
                }
            }

            // 🎯 BƯỚC 4: Tăng ngày lên hằng ngày
            currentDate = currentDate.plusDays(1);
        }
    }

    @Override
    public AttendanceTodayResponse getTodayAttendanceStatus(String username) {
        Staff staff = getStaffByUsername(username);

        // 🔥 ĐIỂM KÍCH HOẠT 1: Nhân viên vừa vào màn hình chấm công -> Tự động dọn dẹp và quét bù quá khứ
        try {
            this.syncAbsentRecordsUpToYesterday(staff.getStaffId());
        } catch (Exception e) {
            System.err.println("🚨 Lỗi đồng bộ vắng mặt ngầm: " + e.getMessage());
        }

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        Optional<WorkShift> activeShiftOpt = workShiftRepository.findActiveShiftByTime(now);
        WorkShift shift = null;
        boolean isExpired = false;

        if (activeShiftOpt.isPresent()) {
            shift = activeShiftOpt.get();
        } else {
            List<WorkShift> allShifts = workShiftRepository.findAll();
            for (WorkShift s : allShifts) {
                if (now.isAfter(s.getEndTime())) {
                    Optional<StaffAttendance> checkExist = attendanceRepository
                            .findByStaffStaffIdAndAttendanceDateAndWorkShiftShiftId(staff.getStaffId(), today, s.getShiftId());
                    if (checkExist.isEmpty()) {
                        shift = s;
                        isExpired = true;
                        break;
                    }
                }
            }
        }

        if (shift == null) {
            return AttendanceTodayResponse.builder()
                    .hasCheckIn(false).hasCheckOut(false).checkInTime("").checkOutTime("").shiftName("")
                    .statusBannerMessage("⛔ Hiện tại chưa đến giờ làm việc hoặc đã quá giờ làm việc trong ngày.")
                    .isExpired(false)
                    .build();
        }

        String displayShiftName = shift.getShiftName();

        if (isExpired) {
            return AttendanceTodayResponse.builder()
                    .hasCheckIn(false).hasCheckOut(false).checkInTime("").checkOutTime("").shiftName(shift.getShiftName())
                    .statusBannerMessage("⚠️ Khung giờ của [" + displayShiftName + "] đã kết thúc. Bạn chưa điểm danh ca này (Ghi nhận vắng: 0.5 công)!")
                    .isExpired(true)
                    .build();
        }

        Optional<StaffAttendance> attendanceOpt = attendanceRepository
                .findByStaffStaffIdAndAttendanceDateAndWorkShiftShiftId(staff.getStaffId(), today, shift.getShiftId());

        if (attendanceOpt.isEmpty()) {
            return AttendanceTodayResponse.builder()
                    .hasCheckIn(false).hasCheckOut(false).checkInTime("").checkOutTime("").shiftName(shift.getShiftName())
                    .statusBannerMessage("Bạn chưa thực hiện <b>Check-in</b> cho <b style='color:var(--primary-color)'>" + displayShiftName + "</b> hôm nay.")
                    .isExpired(false)
                    .build();
        }

        StaffAttendance att = attendanceOpt.get();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        String checkInStr = att.getCheckInTime().format(timeFormatter);
        String checkOutStr = att.getCheckOutTime() != null ? att.getCheckOutTime().format(timeFormatter) : "";

        String bannerMsg = att.getCheckOutTime() == null
                ? "Bạn đã <b>Check-in</b> " + displayShiftName + " lúc: <b style='color:var(--success-color)'>" + checkInStr + "</b>"
                : "Bạn đã hoàn thành phiên làm việc! (" + displayShiftName + " | Vào: " + checkInStr + " - Ra: " + checkOutStr + ")";

        return AttendanceTodayResponse.builder()
                .hasCheckIn(true).hasCheckOut(att.getCheckOutTime() != null).checkInTime(checkInStr).checkOutTime(checkOutStr).shiftName(shift.getShiftName())
                .statusBannerMessage(bannerMsg).isExpired(false)
                .build();
    }

    @Override
    @Transactional
    public StaffAttendance checkIn(String username) {
        Staff staff = getStaffByUsername(username);
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        WorkShift shift = workShiftRepository.findActiveShiftByTime(now)
                .orElseThrow(() -> new RuntimeException("Lỗi: Hiện tại chưa đến giờ làm việc hoặc đã quá giờ hỗ trợ Check-in!"));

        if (attendanceRepository.findByStaffStaffIdAndAttendanceDateAndWorkShiftShiftId(staff.getStaffId(), today, shift.getShiftId()).isPresent()) {
            throw new RuntimeException("Bạn đã thực hiện Check-in ca này ngày hôm nay rồi!");
        }

        String shiftDisplay = shift.getShiftName();
        LocalTime shiftStart = shift.getStartTime(); // Giờ bắt đầu ca chuẩn (08:00 hoặc 16:00)

        // Thiết lập trạng thái mặc định ban đầu là ON_TIME (Đúng giờ)
        AttendanceStatus finalStatus = AttendanceStatus.ON_TIME;
        String attendanceNote = "Đang làm việc (Tạm tính +0.5 công)";

        // Tin nhắn log mặc định khi đi làm đúng giờ (hoặc muộn dưới 15 phút)
        String logMessage = "📌 Nhân viên [" + staff.getFullName() + "] đã Check-in [" + shiftDisplay + "] thành công vào lúc " + now.format(formatter);

        // ====================================================================
        // 🔥 CƠ CẤU LOGIC KIỂM TRA ĐI MUỘN ĐỂ PHÂN LOẠI CẢNH BÁO TIMELINE ADMIN
        // ====================================================================
        if (now.isAfter(shiftStart)) {
            // Tính chính xác số phút đi muộn
            long minutesLate = java.time.temporal.ChronoUnit.MINUTES.between(shiftStart, now);

            if (minutesLate >= 15) {
                // Đổi trạng thái chấm công thành LATE (Đi muộn) để lưu xuống DB
                finalStatus = AttendanceStatus.LATE;
                attendanceNote = "Đi muộn " + minutesLate + " phút";

                if (minutesLate >= 30) {
                    // 🚨 Mốc 1: Muộn từ 30 phút trở lên -> Cảnh báo Đỏ nghiêm trọng
                    logMessage = String.format(
                            "🚨 [BÁO ĐỘNG] Nhân viên [%s] đi muộn ĐẶC BIỆT NGHIÊM TRỌNG (%d phút) tại [%s] vào lúc %s. Hãy kiểm tra nhân sự ca trực!",
                            staff.getFullName(), minutesLate, shiftDisplay, now.format(formatter)
                    );
                } else {
                    // ⚠️ Mốc 2: Muộn từ 15 đến 29 phút -> Cảnh báo Vàng nhắc nhở
                    logMessage = String.format(
                            "⚠️ [CẢNH BÁO] Nhân viên [%s] đi muộn (%d phút) tại [%s] vào lúc %s.",
                            staff.getFullName(), minutesLate, shiftDisplay, now.format(formatter)
                    );
                }
            }
        }

        // Tạo bản ghi chấm công lưu xuống database với trạng thái và note đã phân loại
        StaffAttendance attendance = StaffAttendance.builder()
                .staff(staff)
                .workShift(shift)
                .attendanceDate(today)
                .checkInTime(now)
                .status(finalStatus) // Sử dụng trạng thái đã qua bộ lọc kiểm tra muộn
                .note(attendanceNote)
                .build();

        StaffAttendance saved = attendanceRepository.save(attendance);

        // Đút câu log tương ứng (Đúng giờ / Muộn vừa / Muộn nặng) vào bảng logs để hiển thị lên Timeline
        staffLogService.addLog(staff.getStaffId(), logMessage);

        return saved;
    }

    @Override
    @Transactional
    public StaffAttendance checkOut(String username) {
        Staff staff = getStaffByUsername(username);
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        StaffAttendance attendance = attendanceRepository.findByStaffStaffIdAndAttendanceDateAndCheckOutTimeIsNull(staff.getStaffId(), today)
                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy ca làm việc nào đang chờ bạn Check-out!"));

        attendance.setCheckOutTime(now);

        long minutes = Duration.between(attendance.getCheckInTime(), now).toMinutes();
        BigDecimal hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        attendance.setTotalHours(hours);

        String shiftDisplay = attendance.getWorkShift().getShiftName();
        double hoursDouble = hours.doubleValue();
        String congGhiNhan = "0.0";

        if (hoursDouble >= 7.5) {
            congGhiNhan = "1.0 công (Cả ngày)";
        } else if (hoursDouble >= 3.5) {
            congGhiNhan = "0.5 công (Nửa ngày)";
        } else {
            congGhiNhan = "0.25 công (Làm thiếu giờ)";
        }

        String logMessage;

        if (now.isBefore(attendance.getWorkShift().getEndTime())) {
            attendance.setStatus(AttendanceStatus.EARLY);
            attendance.setNote("Về sớm - Ghi nhận: " + congGhiNhan);
            logMessage = "⚠️ Nhân viên [" + staff.getFullName() + "] đã VỀ SỚM trước khi [" + shiftDisplay + "] kết thúc vào lúc "
                    + now.format(formatter) + " (Thực làm: " + hours + " giờ. Tính: " + congGhiNhan + ")";
        } else {
            attendance.setNote("Hoàn thành - Ghi nhận: " + congGhiNhan);
            logMessage = "🚪 Nhân viên [" + staff.getFullName() + "] đã Check-out [" + shiftDisplay + "] lúc "
                    + now.format(formatter) + " (Thực làm: " + hours + " giờ. Tính: " + congGhiNhan + ")";
        }

        StaffAttendance saved = attendanceRepository.save(attendance);
        staffLogService.addLog(staff.getStaffId(), logMessage);

        return saved;
    }

    @Override
    public List<StaffAttendanceHistoryDTO> getAttendanceHistory(String username) {
        Staff staff = getStaffByUsername(username);

        // Kích hoạt quét bù ngầm như bình thường
        try { this.syncAbsentRecordsUpToYesterday(staff.getStaffId()); } catch (Exception e) {}

        List<StaffAttendance> entities = attendanceRepository.findByStaffStaffIdOrderByAttendanceDateDesc(staff.getStaffId());
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        // 🔥 Map sang DTO sạch sẽ để tránh lỗi Lazy Loading/Vòng lặp JSON
        return entities.stream().map(att -> {
            String checkInStr = att.getCheckInTime() != null ? att.getCheckInTime().format(timeFormatter) : null;
            String checkOutStr = att.getCheckOutTime() != null ? att.getCheckOutTime().format(timeFormatter) : null;
            String hoursStr = att.getTotalHours() != null ? att.getTotalHours().toString() : "0";
            String shiftNameStr = att.getWorkShift() != null ? att.getWorkShift().getShiftName() : "";

            return StaffAttendanceHistoryDTO.builder()
                    .attendanceDate(att.getAttendanceDate().toString())
                    .checkInTime(checkInStr).checkOutTime(checkOutStr).totalHours(hoursStr)
                    .status(att.getStatus().toString()).shiftName(shiftNameStr)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public AttendanceSummaryDTO getAdminWidgetsSummary() {
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        long totalStaff = staffRepository.count();
        long presentToday = attendanceRepository.countPresentStaffToday(today);
        long lateThisMonth = attendanceRepository.countTotalLateInMonth(firstDayOfMonth, today);
        long absentToday = totalStaff - presentToday;
        if (absentToday < 0) absentToday = 0;

        return AttendanceSummaryDTO.builder()
                .totalStaff(totalStaff).presentToday(presentToday).lateThisMonth(lateThisMonth).absentToday(absentToday)
                .build();
    }

    @Override
    public List<StaffAttendanceDashboardDTO> getAllStaffAttendanceDashboard() {
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);

        List<Staff> allStaff = staffRepository.findAll();
        List<StaffAttendance> todayAttendances = attendanceRepository.findByAttendanceDate(today);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        return allStaff.stream().map(staff -> {
            Optional<StaffAttendance> staffTodayRecord = todayAttendances.stream()
                    .filter(a -> a.getStaff().getStaffId().equals(staff.getStaffId()))
                    .findFirst();

            String todayCheckIn = "--:--";
            String todayStatus = "CHUA_CHECKIN";

            if (staffTodayRecord.isPresent()) {
                StaffAttendance att = staffTodayRecord.get();
                todayCheckIn = att.getCheckInTime() != null ? att.getCheckInTime().format(timeFormatter) : "--:--";
                todayStatus = att.getStatus().toString();
            }

            long totalLate = attendanceRepository.countLateByStaffInMonth(staff.getStaffId(), firstDayOfMonth, today);
            long totalAbsent = attendanceRepository.countAbsentByStaffInMonth(staff.getStaffId(), firstDayOfMonth, today);

            return StaffAttendanceDashboardDTO.builder()
                    .staffId(staff.getStaffId()).todayCheckIn(todayCheckIn).todayStatus(todayStatus)
                    .totalLateInMonth(totalLate).totalAbsentInMonth(totalAbsent)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void autoCheckAbsentForFinishedShifts() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        List<WorkShift> allShifts = workShiftRepository.findAll();
        List<Staff> allStaff = staffRepository.findAll();

        for (WorkShift shift : allShifts) {
            if (now.isAfter(shift.getEndTime())) {
                String shiftDisplay = shift.getShiftName();

                for (Staff staff : allStaff) {
                    Optional<StaffAttendance> attendanceOpt = attendanceRepository
                            .findByStaffStaffIdAndAttendanceDateAndWorkShiftShiftId(staff.getStaffId(), today, shift.getShiftId());

                    if (attendanceOpt.isEmpty()) {
                        StaffAttendance absentRecord = StaffAttendance.builder()
                                .staff(staff)
                                .workShift(shift)
                                .attendanceDate(today)
                                .checkInTime(null)
                                .checkOutTime(null)
                                .status(AttendanceStatus.ABSENT)
                                .totalHours(BigDecimal.ZERO)
                                .note("Vắng mặt - Ghi nhận nghỉ: 0.5 công")
                                .build();

                        attendanceRepository.save(absentRecord);

                        staffLogService.addLog(staff.getStaffId(), "🚨 [HỆ THỐNG AUTO] Nhân viên [" + staff.getFullName() + "] vắng mặt không lý do tại [" + shiftDisplay + "]. Ghi nhận nghỉ: 0.5 công.");
                        System.out.println("🚨 [Cron Job] Đã tự động chốt VẮNG MẶT cho NV: " + staff.getFullName() + " tại ca: " + shift.getShiftName());
                    }
                }
            }
        }
    }

    @Override
    public List<StaffAttendanceHistoryDTO> getAttendanceHistoryByStaffId(Integer staffId) {
        // 🔥 ĐIỂM KÍCH HOẠT 3: Quét bù trước khi xuất lịch sử chi tiết cho Admin xem chéo
        try { this.syncAbsentRecordsUpToYesterday(staffId); } catch (Exception e) {}

        List<StaffAttendance> entities = attendanceRepository.findByStaffStaffIdOrderByAttendanceDateDesc(staffId);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        return entities.stream().map(att -> {
            String checkInStr = att.getCheckInTime() != null ? att.getCheckInTime().format(timeFormatter) : null;
            String checkOutStr = att.getCheckOutTime() != null ? att.getCheckOutTime().format(timeFormatter) : null;
            String hoursStr = att.getTotalHours() != null ? att.getTotalHours().toString() : "0";
            String shiftNameStr = att.getWorkShift() != null ? att.getWorkShift().getShiftName() : "";

            return StaffAttendanceHistoryDTO.builder()
                    .attendanceDate(att.getAttendanceDate().toString())
                    .checkInTime(checkInStr).checkOutTime(checkOutStr).totalHours(hoursStr)
                    .status(att.getStatus().toString()).shiftName(shiftNameStr)
                    .build();
        }).collect(Collectors.toList());
    }
}