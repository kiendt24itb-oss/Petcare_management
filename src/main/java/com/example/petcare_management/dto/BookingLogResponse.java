package com.example.petcare_management.dto;

import lombok.*;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingLogResponse {

    private Integer logId;
    private LocalTime logTime; // Giờ xảy ra biến động (Ví dụ: 11:32:00)
    private String logText;    // Nội dung text hiển thị (Ví dụ: "Hệ thống tự động chuyển sang Đang làm")
}