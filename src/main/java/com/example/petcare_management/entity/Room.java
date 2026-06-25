package com.example.petcare_management.entity;

import com.example.petcare_management.entity.enums.RoomStatus;
import com.example.petcare_management.entity.enums.RoomType;
import com.example.petcare_management.entity.enums.ServiceCategory;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Integer roomId;

    @Column(name = "room_code", nullable = false, unique = true, length = 10)
    private String roomCode; // R-A001, R-A002...

    @Column(name = "room_name", nullable = false, length = 100)
    private String roomName;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_category", nullable = false)
    private ServiceCategory roomCategory; // HEALTH, SPA, HOTEL

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type")
    private RoomType roomType; // STANDARD, DELUXE, CLINIC_ROOM...

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RoomStatus status; // AVAILABLE, BUSY, MAINTENANCE

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}