package com.example.petcare_management.controller;

import com.example.petcare_management.dto.BookingLogResponse;
import com.example.petcare_management.service.BookingLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@CrossOrigin("*") // Thông quan CORS cho Front-End gọi thoải mái
public class BookingLogController {

    private final BookingLogService bookingLogService;

    /**
     * 📊 API LẤY NHẬT KÝ BIẾN ĐỘNG ĐỂ VẼ TIMELINE REAL-TIME
     * GET http://localhost:8080/api/bookings/{bookingId}/logs
     * * @param bookingId ID của lịch hẹn cần xem dòng thời gian
     * @return Danh sách các mốc thời gian và nội dung log xếp từ cũ đến mới
     */
    @GetMapping("/{bookingId}/logs")
    public ResponseEntity<List<BookingLogResponse>> getBookingTimeline(
            @PathVariable("bookingId") Integer bookingId) {

        // Gọi Service bốc đống nhật ký dồn dập trong ngày ra
        List<BookingLogResponse> timelineLogs = bookingLogService.getLogsByBookingId(bookingId);

        return ResponseEntity.ok(timelineLogs);
    }
}