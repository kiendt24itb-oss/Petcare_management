package com.example.petcare_management.service;

import com.example.petcare_management.dto.PositionRequest;
import com.example.petcare_management.dto.PositionResponse;
import java.util.List;

public interface PositionService {
    PositionResponse createPosition(PositionRequest request);
    PositionResponse updatePosition(Integer positionId, PositionRequest request);
    List<PositionResponse> getAllPositions();
    void deletePosition(Integer positionId);
}