package com.example.petcare_management.dto;

import com.example.petcare_management.entity.enums.BookingStatus;
import com.example.petcare_management.entity.enums.PaymentStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {

    private Integer bookingId;
    private String bookingCode;
    private String customerName;
    private BigDecimal totalPrice;
    private LocalDate bookingDate;
    private LocalTime bookingTime;
    // 🌟 THÊM TRƯỜNG NÀY: Trả về thời gian vào ca thực tế cho FE hiển thị hoặc tính toán nếu cần
    private java.time.LocalDateTime actualStartTime;
    private PaymentStatus paymentStatus;
    private BookingStatus status;

    // 🌟 THÊM TRƯỜNG NÀY: Trả về phương thức thanh toán (CASH / TRANSFER) cho Front-End bốc ra
    private String paymentMethod;

    private String note;
    private List<DetailResponse> details;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DetailResponse {
        private Integer detailId;
        private String petName;
        private String serviceName;
        private BigDecimal price;
        private String staffName;
        private String roomName;

        // 🌟 THÊM 4 TRƯỜNG CHÍ MẠNG NÀY: Gửi ID thật về để cứu vớt lỗi trống trơn Selector ở Front-End!
        private Integer petId;
        private Integer serviceId;
        private Integer staffId;
        private String categoryType; // Chứa chữ "SPA", "HEALTH" hoặc "HOTEL"
    }
}