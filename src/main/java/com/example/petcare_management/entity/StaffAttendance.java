package com.example.petcare_management.entity;

import com.example.petcare_management.entity.enums.AttendanceStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
// 🔥 ĐÃ NÂNG CẤP: Khai báo khóa Unique hỗn hợp gồm Nhân viên + Ngày + Ca làm việc
@Table(
        name = "staff_attendance",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "unique_staff_date_shift",
                        columnNames = {"staff_id", "attendance_date", "shift_id"}
                )
        }
)
public class StaffAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_id")
    private Integer attendanceId;

    // 🐾 Liên kết với Nhân viên (Chặn lỗi vòng lặp JSON bằng JsonIgnoreProperties)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Staff staff;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "check_in_time", nullable = true)
    private LocalTime checkInTime;

    @Column(name = "check_out_time")
    private LocalTime checkOutTime;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private AttendanceStatus status;

    @Column(name = "total_hours")
    private BigDecimal totalHours;

    @Column(name = "note")
    private String note;

    // ⏱️ Liên kết với Ca làm việc (Đồng bộ tên cột mapped đúng chuẩn database)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private WorkShift workShift;
}