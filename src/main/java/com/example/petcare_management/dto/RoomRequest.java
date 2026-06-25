package com.example.petcare_management.dto;

import com.example.petcare_management.entity.enums.RoomStatus;
import com.example.petcare_management.entity.enums.RoomType;
import com.example.petcare_management.entity.enums.ServiceCategory;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomRequest {
    private String roomName;
    private ServiceCategory roomCategory; // HEALTH, SPA, HOTEL
    private RoomType roomType;            // CLINIC_ROOM, SPA_TABLE...
    private RoomStatus status;            // AVAILABLE, BUSY...
    private String note;
}