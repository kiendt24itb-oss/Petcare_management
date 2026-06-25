package com.example.petcare_management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "booking_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Integer logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "log_time", nullable = false)
    private LocalTime logTime; // Giờ phút xảy ra biến động để vẽ Timeline (ví dụ: 11:32)

    @Column(name = "log_text", nullable = false, columnDefinition = "TEXT")
    private String logText; // Nội dung tiếng Việt bắn lên giao diện

    // Thêm cột này để backend lọc nhật ký của "Ngày hôm nay"
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}