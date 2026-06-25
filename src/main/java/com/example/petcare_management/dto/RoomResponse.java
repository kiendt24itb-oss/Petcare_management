package com.example.petcare_management.dto;

import com.example.petcare_management.entity.enums.RoomStatus;
import com.example.petcare_management.entity.enums.RoomType;
import com.example.petcare_management.entity.enums.ServiceCategory;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomResponse {
    private Integer roomId;
    private String roomCode; // R-A001...
    private String roomName;
    private ServiceCategory roomCategory;
    private RoomType roomType;
    private RoomStatus status;
    private String note;

    // 🌟 BỔ SUNG: Getter ảo trả về tên Khối (Category) tiếng Việt
    public String getCategoryVn() {
        if (this.roomCategory == null) return "Chưa xác định";
        switch (this.roomCategory) {
            case HOTEL: return "Khách Sạn";
            case SPA: return "SPA";
            case HEALTH: return "Thú Y";
            default: return this.roomCategory.name();
        }
    }

    // 🌟 BỔ SUNG: Getter ảo trả về tên Kiểu (Type) tiếng Việt
    public String getRoomTypeVn() {
        if (this.roomType == null) return "Chưa phân loại";
        switch (this.roomType) {
            case STANDARD: return "Phòng Thường";
            case DELUXE: return "Căn Hộ Rộng";
            case VIP: return "Biệt Thự Luxury";
            case SPA_TABLE: return "Bàn Cắt Tỉa / Tạo Kiểu";
            case SPA_TUB: return "Bồn Tắm / Sấy Spa";
            case CLINIC_ROOM: return "Phòng Khám Đa Khoa";
            case SURGERY_ROOM: return "Phẫu Thuật Vô Trùng";
            default: return this.roomType.name();
        }
    }

    // 🌟 BỔ SUNG: Getter ảo trả về Trạng thái (Status) tiếng Việt thân thiện
    public String getStatusVn() {
        if (this.status == null) return "Trạng thái";
        switch (this.status) {
            case AVAILABLE: return "🟢 Còn Trống";
            case BUSY: return "🔴 Đang Có Khách Ở";
            case BOOKED: return "🟠 Đã Được Gán";
            case MAINTENANCE: return "🛠️ Bảo Trì";
            default: return this.status.name();
        }
    }
}