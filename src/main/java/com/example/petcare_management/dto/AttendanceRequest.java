package com.example.petcare_management.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRequest {
    private Integer staffId; // Nhân viên nào điểm danh
    private String note;     // Lý do đi trễ hoặc dặn dò ca làm
}