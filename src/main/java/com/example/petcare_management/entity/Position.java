package com.example.petcare_management.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "positions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "position_id")
    private Integer positionId;

    @Column(name = "position_code", nullable = false, unique = true, length = 20)
    private String positionCode; // Mã chức vụ (BS, KTV, LT...)

    @Column(name = "position_name", nullable = false, length = 100)
    private String positionName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "base_allowance")
    private BigDecimal baseAllowance; // Phụ cấp cơ bản
}