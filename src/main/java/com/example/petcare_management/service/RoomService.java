package com.example.petcare_management.service;

import com.example.petcare_management.dto.RoomRequest;
import com.example.petcare_management.dto.RoomResponse;
import com.example.petcare_management.entity.enums.ServiceCategory;
import java.util.List;

public interface RoomService {
    RoomResponse createRoom(RoomRequest request);
    RoomResponse updateRoom(Integer roomId, RoomRequest request);
    List<RoomResponse> getAllRooms();
    List<RoomResponse> getAvailableRoomsByCategory(ServiceCategory category);

    // =========================================================================
    // 🌟 3 HÀM MỚI ĐỂ ĐỒNG BỘ LOGIC GÁN PHÒNG / ĐỔI TRẠNG THÁI GIỮA CÁC DỊCH VỤ
    // =========================================================================

    /**
     * Chuyển trạng thái phòng sang BOOKED khi có bất kỳ dịch vụ nào chọn gán phòng
     */
    RoomResponse assignRoomToService(Integer roomId);

    /**
     * Chuyển trạng thái phòng sang BUSY khi khách đến check-in / bắt đầu làm dịch vụ
     */
    RoomResponse checkInRoom(Integer roomId);

    /**
     * Chuyển trạng thái phòng về AVAILABLE khi hoàn thành / thanh toán / giải phóng phòng
     */
    RoomResponse releaseRoom(Integer roomId);
}