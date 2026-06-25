package com.example.petcare_management.dto;

import com.example.petcare_management.entity.enums.AttendanceStatus;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceResponse {
    private Integer attendanceId;
    private String staffCode;
    private String staffName;
    private LocalDate attendanceDate;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private AttendanceStatus status; // PRESENT, LATE, ABSENT
    private String note;
}