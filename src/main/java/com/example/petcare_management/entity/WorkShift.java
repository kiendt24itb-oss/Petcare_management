package com.example.petcare_management.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;

@Entity
@Table(name = "work_shifts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkShift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shift_id")
    private Integer shiftId;

    @Column(name = "shift_name", nullable = false)
    private String shiftName; // CA_NGAY, CA_TOI

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime; // 08:00:00 hoặc 16:00:00

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime; // 16:00:00 hoặc 22:00:00
}