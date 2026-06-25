package com.example.petcare_management.repository;

import com.example.petcare_management.entity.Booking;
import com.example.petcare_management.entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Integer> {

    // 🔮 Bốc mã lịch hẹn lớn nhất hiện tại để phục vụ CodeGenerator sinh mã (Ví dụ: BK-005)
    @Query(value = "SELECT b.booking_code FROM bookings b WHERE b.booking_code LIKE 'BK-%' ORDER BY b.booking_code DESC LIMIT 1", nativeQuery = true)
    String findLatestBookingCode();

    // 🔎 Tìm toàn bộ lịch đặt của một khách hàng dựa vào username tài khoản (Sắp xếp tăng dần theo lịch hẹn)
    List<Booking> findByCustomer_Account_UsernameOrderByBookingDateAscBookingTimeAsc(String username);

    // 🌟 Phục vụ Luồng 1 của Cron Job: Quét tự động theo Ngày và Trạng thái WAITING
    List<Booking> findByBookingDateAndStatus(LocalDate bookingDate, BookingStatus status);

    // 🌟 Phục vụ Luồng 2 của Cron Job: Quét đơn theo trạng thái để xử lý logic (PROCESSING, PENDING_APPROVAL...)
    List<Booking> findByStatus(BookingStatus status);

    @Query("SELECT DISTINCT b FROM Booking b JOIN b.bookingDetails d WHERE d.staff.account.username = :username ORDER BY b.bookingDate ASC, b.bookingTime ASC")
    List<Booking> findByStaffUsername(@Param("username") String username);

    // 📊 1. Tính tổng doanh thu của một tháng cụ thể (Hiển thị con số tháng hiện tại)
    @Query("SELECT COALESCE(SUM(b.totalPrice), 0) FROM Booking b " +
            "WHERE b.status = com.example.petcare_management.entity.enums.BookingStatus.COMPLETED " +
            "AND FUNCTION('MONTH', b.bookingDate) = :month " +
            "AND FUNCTION('YEAR', b.bookingDate) = :year")
    BigDecimal calculateMonthlyRevenue(@Param("month") int month, @Param("year") int year);

    // 📈 2. Lấy doanh thu các đơn COMPLETED trong khoảng từ ngày đến ngày (Phục vụ gom nhóm 6 tháng gần nhất)
    @Query("SELECT FUNCTION('YEAR', b.bookingDate) AS y, FUNCTION('MONTH', b.bookingDate) AS m, SUM(b.totalPrice) " +
            "FROM Booking b " +
            "WHERE b.status = com.example.petcare_management.entity.enums.BookingStatus.COMPLETED " +
            "AND b.bookingDate >= :startDate " +
            "GROUP BY FUNCTION('YEAR', b.bookingDate), FUNCTION('MONTH', b.bookingDate) " +
            "ORDER BY y ASC, m ASC")
    List<Object[]> getRevenueByPeriod(@Param("startDate") LocalDate startDate);

    // 📅 3. Đếm tổng số đơn hôm nay (Gom hết các đơn đang vận hành, bỏ qua đơn HỦY và TỪ CHỐI)
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.bookingDate = :date " +
            "AND b.status NOT IN (com.example.petcare_management.entity.enums.BookingStatus.CANCELLED, " +
            "com.example.petcare_management.entity.enums.BookingStatus.REJECTED)")
    long countActiveBookingsByDate(@Param("date") LocalDate date);
}