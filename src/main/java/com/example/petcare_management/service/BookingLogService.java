package com.example.petcare_management.service;

import com.example.petcare_management.dto.BookingLogResponse;
import java.util.List;

public interface BookingLogService {
    // Luồng cũ: Lấy log lẻ của 1 đơn hàng khi xem chi tiết
    List<BookingLogResponse> getLogsByBookingId(Integer bookingId);

    // 📊 LUỒNG MỚI: Lấy toàn bộ log vận hành của ngày hôm nay đổ lên Timeline Dashboard Admin
    List<BookingLogResponse> getDailyLogsForDashboard();
}