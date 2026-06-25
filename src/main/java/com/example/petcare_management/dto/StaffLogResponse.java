package com.example.petcare_management.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffLogResponse {
    private Integer logId;
    private Integer staffId;
    private String staffCode;    // Hiện mã NV dạng NV001 cho chuyên nghiệp
    private String staffName;    // Hiện tên NV để Admin dễ đọc log
    private LocalDateTime logTime;
    private String logText;
}