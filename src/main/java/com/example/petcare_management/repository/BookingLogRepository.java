package com.example.petcare_management.repository;

import com.example.petcare_management.entity.BookingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingLogRepository extends JpaRepository<BookingLog, Integer> {

    // 📊 Lấy toàn bộ lịch sử biến động của 1 đơn hàng cụ thể, sắp xếp theo thứ tự thời gian tăng dần
    List<BookingLog> findByBooking_BookingIdOrderByLogTimeAsc(Integer bookingId);

    // 🌟 SỬA LẠI DÒNG NÀY: Dùng hàm CURRENT_DATE của DB để quét chuẩn ngày hôm nay
    @Query("SELECT bl FROM BookingLog bl WHERE DATE(bl.createdAt) = CURRENT_DATE ORDER BY bl.createdAt DESC")
    List<BookingLog> findLogsToday();
}