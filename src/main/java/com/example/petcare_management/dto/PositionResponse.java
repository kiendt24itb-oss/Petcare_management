package com.example.petcare_management.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PositionResponse {
    private Integer positionId;
    private String positionCode;
    private String positionName;
    private String description;
    private BigDecimal baseAllowance;
}