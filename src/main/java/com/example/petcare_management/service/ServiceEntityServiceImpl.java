package com.example.petcare_management.service;

import com.example.petcare_management.dto.ServiceRequest;
import com.example.petcare_management.dto.ServiceResponse;
import com.example.petcare_management.entity.Room;
import com.example.petcare_management.entity.ServiceEntity;
import com.example.petcare_management.entity.enums.RoomStatus;
import com.example.petcare_management.entity.enums.ServiceCategory;
import com.example.petcare_management.repository.RoomRepository;
import com.example.petcare_management.repository.ServiceRepository;
import com.example.petcare_management.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceEntityServiceImpl implements ServiceEntityService {

    private final ServiceRepository serviceRepository;
    private final RoomRepository roomRepository;

    @Override
    @Transactional
    public ServiceResponse createService(ServiceRequest request) {
        String nextCode = generateNextServiceCode(request.getCategory());

        Room assignedRoom = null;
        if (request.getRoomCode() != null && !request.getRoomCode().trim().isEmpty()) {
            assignedRoom = roomRepository.findByRoomCode(request.getRoomCode())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng có mã: " + request.getRoomCode()));

            // 🛑 ĐỒNG BỘ MỚI: Chặn nếu phòng đang trong trạng thái bảo trì
            if (assignedRoom.getStatus() == RoomStatus.MAINTENANCE) {
                throw new IllegalStateException("Thất bại: Phòng " + request.getRoomCode() + " đang bảo trì, không thể gán cho dịch vụ mới!");
            }

            // Kiểm tra trạng thái khả dụng thông thường
            if (assignedRoom.getStatus() != RoomStatus.AVAILABLE) {
                throw new IllegalStateException("Thất bại: Phòng " + request.getRoomCode() + " đã được gán hoặc đang bận!");
            }
            assignedRoom.setStatus(RoomStatus.BOOKED);
            roomRepository.save(assignedRoom);
        }

        ServiceEntity serviceEntity = ServiceEntity.builder()
                .serviceCode(nextCode)
                .serviceName(request.getServiceName())
                .category(request.getCategory())
                .price(request.getPrice())
                .durationMinutes(request.getDurationMinutes())
                .room(assignedRoom)
                .description(request.getDescription())
                .status(true)
                .build();

        ServiceEntity saved = serviceRepository.save(serviceEntity);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ServiceResponse updateService(Integer serviceId, ServiceRequest request) {
        ServiceEntity serviceEntity = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ nào có ID: " + serviceId));

        // Giải phóng phòng cũ về AVAILABLE (Chỉ giải phóng nếu phòng cũ không phải đang bị khóa bảo trì đột xuất)
        if (serviceEntity.getRoom() != null) {
            Room oldRoom = serviceEntity.getRoom();
            if (oldRoom.getStatus() != RoomStatus.MAINTENANCE) {
                oldRoom.setStatus(RoomStatus.AVAILABLE);
                roomRepository.save(oldRoom);
            }
        }

        // Tìm và liên kết phòng mới từ request
        Room assignedRoom = null;
        if (request.getRoomCode() != null && !request.getRoomCode().trim().isEmpty()) {
            assignedRoom = roomRepository.findByRoomCode(request.getRoomCode())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng phục vụ mới: " + request.getRoomCode()));

            // 🛑 ĐỒNG BỘ MỚI: Chặn nếu phòng mới chọn đang trong quá trình sửa chữa/bảo trì
            if (assignedRoom.getStatus() == RoomStatus.MAINTENANCE) {
                throw new IllegalStateException("Thất bại: Phòng mới chọn (" + request.getRoomCode() + ") đang trong quá trình bảo trì!");
            }

            // Kiểm tra nếu phòng mới bị trùng lịch dịch vụ khác
            if (assignedRoom.getStatus() != RoomStatus.AVAILABLE) {
                throw new IllegalStateException("Thất bại: Phòng mới chọn đã bị dịch vụ khác đặt mất!");
            }
            assignedRoom.setStatus(RoomStatus.BOOKED);
            roomRepository.save(assignedRoom);
        }

        serviceEntity.setServiceName(request.getServiceName());
        serviceEntity.setCategory(request.getCategory());
        serviceEntity.setPrice(request.getPrice());
        serviceEntity.setDurationMinutes(request.getDurationMinutes());
        serviceEntity.setRoom(assignedRoom);
        serviceEntity.setDescription(request.getDescription());

        if (request.getStatus() != null) {
            serviceEntity.setStatus(request.getStatus());
        }

        ServiceEntity updated = serviceRepository.save(serviceEntity);
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceResponse> getAllServices() {
        return serviceRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceResponse> getActiveServicesByCategory(ServiceCategory category) {
        return serviceRepository.findByCategoryAndStatusTrue(category).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void toggleServiceStatus(Integer serviceId) {
        ServiceEntity serviceEntity = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ cần đổi trạng thái!"));

        serviceEntity.setStatus(!serviceEntity.getStatus());
        serviceRepository.save(serviceEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceResponse> searchServices(ServiceCategory category, String keyword) {
        String trimmedKeyword = (keyword == null) ? "" : keyword.trim();
        return serviceRepository.searchServices(category, trimmedKeyword).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private String generateNextServiceCode(ServiceCategory category) {
        String prefix;
        switch (category) {
            case HEALTH -> prefix = "DV-H";
            case SPA -> prefix = "DV-S";
            case HOTEL -> prefix = "DV-O";
            default -> prefix = "DV-";
        }
        String latestCode = serviceRepository.findMaxServiceCodeByCategory(category);
        return CodeGenerator.generateNextCode(latestCode, prefix);
    }

    private ServiceResponse mapToResponse(ServiceEntity entity) {
        return ServiceResponse.builder()
                .serviceId(entity.getServiceId())
                .serviceCode(entity.getServiceCode())
                .serviceName(entity.getServiceName())
                .category(entity.getCategory())
                .price(entity.getPrice())
                .durationMinutes(entity.getDurationMinutes())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .roomCode(entity.getRoom() != null ? entity.getRoom().getRoomCode() : "Chưa gán")
                .build();
    }
}