package com.example.petcare_management.service;

import com.example.petcare_management.dto.PositionRequest;
import com.example.petcare_management.dto.PositionResponse;
import com.example.petcare_management.entity.Position;
import com.example.petcare_management.repository.PositionRepository;
import com.example.petcare_management.repository.StaffRepository; // 🌟 QUAN TRỌNG: Phải import thêm ông này vào
import com.example.petcare_management.service.PositionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PositionServiceImpl implements PositionService {

    private final PositionRepository positionRepository;
    private final StaffRepository staffRepository; // 🌟 ĐÃ NHÚNG: Để kiểm tra ràng buộc trước khi xóa ghế

    @Override
    @Transactional
    public PositionResponse createPosition(PositionRequest request) {
        Position position = Position.builder()
                .positionCode(request.getPositionCode().toUpperCase())
                .positionName(request.getPositionName())
                .description(request.getDescription())
                .baseAllowance(request.getBaseAllowance())
                .build();
        return mapToResponse(positionRepository.save(position));
    }

    @Override
    @Transactional
    public PositionResponse updatePosition(Integer positionId, PositionRequest request) {
        Position position = positionRepository.findById(positionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chức vụ!"));
        position.setPositionName(request.getPositionName());
        position.setDescription(request.getDescription());
        position.setBaseAllowance(request.getBaseAllowance());
        return mapToResponse(positionRepository.save(position));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PositionResponse> getAllPositions() {
        return positionRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deletePosition(Integer positionId) {
        // 1. Kiểm tra xem Id chức vụ này có tồn tại trong hệ thống không
        Position position = positionRepository.findById(positionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chức vụ để xóa!"));

        // 2. Chặn đứng lỗi sập DB: Kiểm tra xem có lính nào đang ngồi ở cái ghế chức vụ này không
        boolean hasStaff = !staffRepository.findByPosition_PositionCode(position.getPositionCode()).isEmpty();

        if (hasStaff) {
            throw new RuntimeException("Không thể xóa! Hiện tại đang có nhân viên thuộc chức vụ: " + position.getPositionName());
        }

        // 3. Mọi thứ an toàn thì tiến hành cho bay màu
        positionRepository.deleteById(positionId);
    }

    private PositionResponse mapToResponse(Position position) {
        return PositionResponse.builder()
                .positionId(position.getPositionId())
                .positionCode(position.getPositionCode())
                .positionName(position.getPositionName())
                .description(position.getDescription())
                .baseAllowance(position.getBaseAllowance())
                .build();
    }
}