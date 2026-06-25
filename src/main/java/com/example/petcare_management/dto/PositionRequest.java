package com.example.petcare_management.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PositionRequest {
    private String positionCode; // Ví dụ: BS, KTV, LT
    private String positionName; // Ví dụ: Bác sĩ thú y
    private String description;
    private BigDecimal baseAllowance;
}