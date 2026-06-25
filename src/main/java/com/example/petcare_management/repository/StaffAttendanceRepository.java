package com.example.petcare_management.repository;

import com.example.petcare_management.entity.StaffAttendance;
import com.example.petcare_management.entity.enums.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StaffAttendanceRepository extends JpaRepository<StaffAttendance, Integer> {

    // === 🕒 Các hàm cũ của bạn (Giữ nguyên) ===
    Optional<StaffAttendance> findByStaffStaffIdAndAttendanceDateAndWorkShiftShiftId(Integer staffId, LocalDate date, Integer shiftId);
    Optional<StaffAttendance> findByStaffStaffIdAndAttendanceDateAndCheckOutTimeIsNull(Integer staffId, LocalDate date);
    List<StaffAttendance> findByStaffStaffIdOrderByAttendanceDateDesc(Integer staffId);

    // === 📊 CÁC HÀM BỔ SUNG CHO ADMIN DASHBOARD ===

    /**
     * 1. Đếm số nhân viên độc bản ĐÃ CÓ MẶT hôm nay
     * (Chỉ cần có bản ghi bất kể ca sáng hay ca chiều)
     */
    @Query("SELECT COUNT(DISTINCT a.staff.staffId) FROM StaffAttendance a WHERE a.attendanceDate = :date")
    Long countPresentStaffToday(@Param("date") LocalDate date);

    /**
     * 2. Đếm tổng số lượt ĐI MUỘN của toàn bộ tiệm trong tháng hiện tại
     */
    @Query("SELECT COUNT(a) FROM StaffAttendance a WHERE a.status = 'LATE' " +
            "AND a.attendanceDate BETWEEN :startDate AND :endDate")
    Long countTotalLateInMonth(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 3. Lấy danh sách chấm công của TẤT CẢ nhân viên trong ngày hôm nay
     * (Để Admin biết hôm nay ai đi giờ nào, trạng thái gì)
     */
    List<StaffAttendance> findByAttendanceDate(LocalDate date);

    /**
     * 4. Đếm số ngày đi muộn (LATE) của MỘT NHÂN VIÊN cụ thể trong tháng
     */
    @Query("SELECT COUNT(a) FROM StaffAttendance a WHERE a.staff.staffId = :staffId " +
            "AND a.status = 'LATE' AND a.attendanceDate BETWEEN :startDate AND :endDate")
    Long countLateByStaffInMonth(@Param("staffId") Integer staffId,
                                 @Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate);

    /**
     * 5. Đếm số ngày vắng mặt (ABSENT) của MỘT NHÂN VIÊN cụ thể trong tháng
     */
    @Query("SELECT COUNT(a) FROM StaffAttendance a WHERE a.staff.staffId = :staffId " +
            "AND a.status = 'ABSENT' AND a.attendanceDate BETWEEN :startDate AND :endDate")
    Long countAbsentByStaffInMonth(@Param("staffId") Integer staffId,
                                   @Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate);

    // 🔥 CHÈN THÊM HÀM NÀY: Kiểm tra xem nhân viên này đã có bản ghi điểm danh ở ca này, ngày này chưa
    boolean existsByStaffStaffIdAndAttendanceDateAndWorkShiftShiftId(Integer staffId, LocalDate date, Integer shiftId);
}