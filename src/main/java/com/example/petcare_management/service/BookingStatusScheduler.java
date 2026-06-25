package com.example.petcare_management.service;

import com.example.petcare_management.entity.Booking;
import com.example.petcare_management.entity.BookingDetail;
import com.example.petcare_management.entity.BookingLog;
import com.example.petcare_management.entity.ServiceEntity;
import com.example.petcare_management.entity.enums.BookingStatus;
import com.example.petcare_management.repository.BookingRepository;
import com.example.petcare_management.repository.BookingLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingStatusScheduler {

    private final BookingRepository bookingRepository;
    private final BookingLogRepository bookingLogRepository;

    /**
     * 🕒 TỰ ĐỘNG QUÉT VÀ CHUYỂN TRẠNG THÁI (CHẠY NGẦM 1 PHÚT MỘT LẦN)
     */
    @Scheduled(cron = "0 */1 * * * *") // Kích hoạt mỗi khi kim phút nhảy
    @Transactional
    public void autoUpdateBookingStatuses() {
        LocalDate today = LocalDate.now();
        LocalTime nowTime = LocalTime.now();

        // ---------------------------------------------------------------------
        // 🔄 LUỒNG 1: CHUYỂN TỪ "WAITING" -> "PROCESSING" (ĐẾN GIỜ LÀM VIỆC)
        // ---------------------------------------------------------------------
        List<Booking> waitingBookings = bookingRepository.findByBookingDateAndStatus(today, BookingStatus.WAITING);

        for (Booking bk : waitingBookings) {
            if (nowTime.isAfter(bk.getBookingTime()) || nowTime.equals(bk.getBookingTime())) {
                bk.setStatus(BookingStatus.PROCESSING);
                bookingRepository.save(bk);

                BookingLog timelineLog = BookingLog.builder()
                        .booking(bk)
                        .logTime(nowTime)
                        .logText("✂️ Bé đã đến giờ hẹn lịch. Hệ thống tự động chuyển trạng thái sang [Đang tiến hành làm dịch vụ].")
                        .build();
                bookingLogRepository.save(timelineLog);

                BookingStatusScheduler.log.info("🎉 Đã tự động kích hoạt ca làm việc cho đơn: {}", bk.getBookingCode());
            }
        }

        // ---------------------------------------------------------------------
        // 🔄 LUỒNG 2: CHUYỂN TỪ "PROCESSING" -> "COMPLETED" (TỰ ĐỘNG ĐÓNG CA)
        // ---------------------------------------------------------------------
        List<Booking> processingBookings = bookingRepository.findByBookingDateAndStatus(today, BookingStatus.PROCESSING);

        for (Booking bk : processingBookings) {
            int totalDurationMinutes = 0;

            List<BookingDetail> detailsList = bk.getBookingDetails();
            for (BookingDetail detail : detailsList) {
                ServiceEntity dịchVụ = detail.getService();
                if (dịchVụ != null && dịchVụ.getDurationMinutes() != null) {
                    totalDurationMinutes += dịchVụ.getDurationMinutes();
                }
            }

            if (totalDurationMinutes == 0) {
                totalDurationMinutes = 60; // Dự phòng 60 phút
            }

            // 🌟 LOGIC TÍNH TOÁN THỜI GIAN HOÀN THÀNH DỰ KIẾN:
            LocalTime completionTime;

            if (bk.getActualStartTime() != null) {
                // Nếu có actualStartTime -> Nhân viên đã bấm "Vào ca sớm" -> Lấy giờ thực tế bấm nút + số phút dịch vụ
                completionTime = bk.getActualStartTime().toLocalTime().plusMinutes(totalDurationMinutes);
            } else {
                // Nếu không có actualStartTime -> Hệ thống tự động vào ca đúng giờ -> Lấy giờ đặt gốc + số phút dịch vụ
                completionTime = bk.getBookingTime().plusMinutes(totalDurationMinutes);
            }

            // Nếu giờ hiện tại đã đến hoặc vượt quá giờ hoàn thành dự kiến -> Auto đóng đơn
            if (nowTime.isAfter(completionTime) || nowTime.equals(completionTime)) {
                bk.setStatus(BookingStatus.COMPLETED);
                bookingRepository.save(bk);

                BookingLog timelineLog = BookingLog.builder()
                        .booking(bk)
                        .logTime(nowTime)
                        .logText("📑 Toàn bộ dịch vụ đăng ký đã hoàn tất (Thời gian thực hiện: " + totalDurationMinutes + " phút). Bé đã sẵn sàng về nhà với chủ!")
                        .build();
                bookingLogRepository.save(timelineLog);

                BookingStatusScheduler.log.info("✅ Đã tự động đóng ca thành công cho đơn: {}", bk.getBookingCode());
            }
        }
    }
}