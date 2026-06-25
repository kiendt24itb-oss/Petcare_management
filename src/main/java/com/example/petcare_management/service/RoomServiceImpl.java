package com.example.petcare_management.service;

import com.example.petcare_management.dto.RoomRequest;
import com.example.petcare_management.dto.RoomResponse;
import com.example.petcare_management.entity.Room;
import com.example.petcare_management.entity.enums.RoomStatus;
import com.example.petcare_management.entity.enums.RoomType;
import com.example.petcare_management.entity.enums.ServiceCategory;
import com.example.petcare_management.repository.RoomRepository;
import com.example.petcare_management.repository.ServiceRepository; // 💥 Import chuẩn repo dịch vụ vào ní ơi
import com.example.petcare_management.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final ServiceRepository serviceRepository; // 💥 Tiêm trực tiếp vào để dùng lệnh dọn phòng bảo trì

    @Override
    @Transactional
    public RoomResponse createRoom(RoomRequest request) {
        RoomType determinedType = request.getRoomType();
        if (request.getRoomCategory() == ServiceCategory.HEALTH) {
            determinedType = RoomType.CLINIC_ROOM;
        } else if (request.getRoomCategory() == ServiceCategory.SPA) {
            determinedType = RoomType.SPA_TABLE;
        }

        String latestCode = roomRepository.findLatestRoomCode();

        Room room = Room.builder()
                .roomCode(CodeGenerator.generateNextCode(latestCode, "R"))
                .roomName(request.getRoomName())
                .roomCategory(request.getRoomCategory())
                .roomType(determinedType)
                .status(RoomStatus.AVAILABLE)
                .note(request.getNote())
                .build();

        return mapToResponse(roomRepository.save(room));
    }

    @Override
    @Transactional
    public RoomResponse updateRoom(Integer roomId, RoomRequest request) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng hoặc vị trí vận hành!"));

        // =========================================================================
        // 🔄 LOGIC ĐỒNG BỘ: XỬ LÝ KHI BẤM NÚT "KHÓA BẢO TRÌ"
        // =========================================================================
        if (request.getStatus() == RoomStatus.MAINTENANCE) {

            // Nếu phòng đang được gán dịch vụ (BOOKED) hoặc đang chạy dở ca (BUSY)
            if (room.getStatus() == RoomStatus.BOOKED || room.getStatus() == RoomStatus.BUSY) {

                // 🧹 Kích hoạt lệnh update DB ép toàn bộ Dịch vụ đang bám vào phòng này về NULL
                serviceRepository.detachRoomFromServices(room.getRoomId());
            }

        } else {
            // 🛡️ LOGIC BẢO VỆ CŨ: Chỉ chặn khi người dùng update thông tin thông thường (không phải đi bảo trì)
            if (room.getStatus() == RoomStatus.BOOKED || room.getStatus() == RoomStatus.BUSY) {
                if (room.getRoomCategory() != request.getRoomCategory()) {
                    throw new IllegalStateException("Không thể đổi Khối dịch vụ khi phòng đang có lịch đặt hoặc đang có khách!");
                }
            }
        }

        // Cập nhật thông tin bình thường
        room.setRoomName(request.getRoomName());
        room.setRoomCategory(request.getRoomCategory());

        if (request.getRoomCategory() == ServiceCategory.HOTEL) {
            room.setRoomType(request.getRoomType());
        } else {
            room.setRoomType(request.getRoomCategory() == ServiceCategory.HEALTH ?
                    RoomType.CLINIC_ROOM : RoomType.SPA_TABLE);
        }

        if (request.getStatus() != null) {
            room.setStatus(request.getStatus());
        }
        room.setNote(request.getNote());

        return mapToResponse(roomRepository.save(room));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getAllRooms() {
        return roomRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getAvailableRoomsByCategory(ServiceCategory category) {
        return roomRepository.findByRoomCategoryAndStatus(category, RoomStatus.AVAILABLE).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RoomResponse assignRoomToService(Integer roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng để gán dịch vụ!"));

        // 🛡️ CHẶN LUÔN NẾU PHÒNG ĐANG BẢO TRÌ
        if (room.getStatus() == RoomStatus.MAINTENANCE) {
            throw new IllegalStateException("Thất bại: Phòng này đang bảo trì, không thể gán dịch vụ!");
        }

        if (room.getStatus() != RoomStatus.AVAILABLE) {
            throw new IllegalStateException("Thất bại: Phòng này đã được gán hoặc đang bận!");
        }

        room.setStatus(RoomStatus.BOOKED);
        return mapToResponse(roomRepository.save(room));
    }

    @Override
    @Transactional
    public RoomResponse checkInRoom(Integer roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng để tiến hành check-in!"));

        if (room.getStatus() == RoomStatus.MAINTENANCE) {
            throw new IllegalStateException("Thất bại: Phòng đang bảo trì!");
        }

        room.setStatus(RoomStatus.BUSY);
        return mapToResponse(roomRepository.save(room));
    }

    @Override
    @Transactional
    public RoomResponse releaseRoom(Integer roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng để giải phóng!"));

        room.setStatus(RoomStatus.AVAILABLE);
        return mapToResponse(roomRepository.save(room));
    }

    private RoomResponse mapToResponse(Room room) {
        return RoomResponse.builder()
                .roomId(room.getRoomId())
                .roomCode(room.getRoomCode())
                .roomName(room.getRoomName())
                .roomCategory(room.getRoomCategory())
                .roomType(room.getRoomType())
                .status(room.getStatus())
                .note(room.getNote())
                .build();
    }
}