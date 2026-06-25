package com.example.petcare_management.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaffLogRequest {
    private Integer staffId;
    private String logText;
}