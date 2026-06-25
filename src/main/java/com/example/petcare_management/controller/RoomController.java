package com.example.petcare_management.controller;

import com.example.petcare_management.dto.RoomRequest;
import com.example.petcare_management.dto.RoomResponse;
import com.example.petcare_management.entity.enums.ServiceCategory;
import com.example.petcare_management.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@CrossOrigin("*")
public class RoomController {

    private final RoomService roomService;

    // 📥 1. Khởi tạo không gian mới (Mã phòng tự nhảy số tuần tự)
    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(@RequestBody RoomRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.createRoom(request));
    }

    // 📝 2. Cập nhật chi tiết thông tin phòng hoặc đổi trạng thái nhanh (AVAILABLE, BUSY, MAINTENANCE)
    // 🛡️ ĐÃ VÁ LỖI: Chỉ định rõ ràng định danh "roomId" bên trong @PathVariable để tránh lỗi Reflection compiler
    @PutMapping("/{roomId}")
    public ResponseEntity<RoomResponse> updateRoom(
            @PathVariable("roomId") Integer roomId,
            @RequestBody RoomRequest request) {
        return ResponseEntity.ok(roomService.updateRoom(roomId, request));
    }

    // 📋 3. Lấy toàn bộ danh sách phòng
    @GetMapping("/all")
    public ResponseEntity<List<RoomResponse>> getAllRooms() {
        return ResponseEntity.ok(roomService.getAllRooms());
    }

    // 🔍 4. Lấy danh sách phòng ĐANG TRỐNG theo khối (Phục vụ xếp lịch ca WAITING)
    @GetMapping("/available")
    public ResponseEntity<List<RoomResponse>> getAvailableRooms(@RequestParam ServiceCategory category) {
        return ResponseEntity.ok(roomService.getAvailableRoomsByCategory(category));
    }
}