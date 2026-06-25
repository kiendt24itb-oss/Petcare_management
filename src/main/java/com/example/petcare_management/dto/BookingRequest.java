package com.example.petcare_management.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingRequest {

    private LocalDate bookingDate;
    private LocalTime bookingTime;
    private String note;

    // 🌟 THÊM TRƯỜNG NÀY: Để nhận dữ liệu thanh toán (CASH / TRANSFER) từ Front-End gửi lên
    private String paymentMethod;

    private List<BookingItemDTO> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BookingItemDTO {
        private Integer petId;     // Khớp với Integer trong JpaRepository<Pet, Integer>
        private Integer serviceId;   // Khớp với Integer luôn ní nha
        private Integer staffId;   // 🌟 THÊM DÒNG NÀY: Để hứng ID nhân viên do khách chọn từ Front-End bắn lên nka!
    }
}