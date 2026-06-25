package com.example.petcare_management.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "staff_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder // Dùng Builder để sau này viết log nhanh, ví dụ: StaffLog.builder().text("...").build()
public class StaffLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Integer logId;

    // Nhiều dòng log thuộc về 1 Nhân viên
    @ManyToOne(fetch = FetchType.LAZY) // Dùng LAZY để tối ưu hiệu năng khi select
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @Column(name = "log_time")
    private LocalDateTime logTime = LocalDateTime.now();

    @Column(name = "log_text", nullable = false, columnDefinition = "TEXT")
    private String logText;
}