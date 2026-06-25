package com.example.petcare_management.service;

import com.example.petcare_management.dto.BookingLogResponse;
import com.example.petcare_management.repository.BookingLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime; // 🌟 SỬA: Import LocalTime thay vì LocalDateTime
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingLogServiceImpl implements BookingLogService {

    private final BookingLogRepository bookingLogRepository;

    @Override
    @Transactional(readOnly = true)
    public List<BookingLogResponse> getLogsByBookingId(Integer bookingId) {
        return bookingLogRepository.findByBooking_BookingIdOrderByLogTimeAsc(bookingId)
                .stream()
                .map(log -> BookingLogResponse.builder()
                        .logId(log.getLogId())
                        .logTime(log.getLogTime())
                        .logText(log.getLogText())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingLogResponse> getDailyLogsForDashboard() {
        // 🚀 Gọi hàm Repo mới chỉnh sửa, DB tự quét đúng ngày hôm nay của nó
        return bookingLogRepository.findLogsToday()
                .stream()
                .map(log -> BookingLogResponse.builder()
                        .logId(log.getLogId())
                        .logTime(log.getLogTime()) // Lấy trực tiếp logTime không trống trong DB
                        .logText(log.getLogText())
                        .build())
                .collect(Collectors.toList());
    }
}