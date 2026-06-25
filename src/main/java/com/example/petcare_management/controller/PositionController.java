package com.example.petcare_management.controller;

import com.example.petcare_management.dto.PositionRequest;
import com.example.petcare_management.dto.PositionResponse;
import com.example.petcare_management.service.PositionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/positions")
@RequiredArgsConstructor
@CrossOrigin("*")
public class PositionController {

    private final PositionService positionService;

    @PostMapping("/create")
    public ResponseEntity<PositionResponse> createPosition(@RequestBody PositionRequest request) {
        return ResponseEntity.ok(positionService.createPosition(request));
    }

    @PutMapping("/update/{positionId}")
    public ResponseEntity<PositionResponse> updatePosition(
            @PathVariable Integer positionId,
            @RequestBody PositionRequest request) {
        return ResponseEntity.ok(positionService.updatePosition(positionId, request));
    }

    @GetMapping("/all")
    public ResponseEntity<List<PositionResponse>> getAllPositions() {
        return ResponseEntity.ok(positionService.getAllPositions());
    }

    @DeleteMapping("/delete/{positionId}")
    public ResponseEntity<String> deletePosition(@PathVariable Integer positionId) {
        try {
            positionService.deletePosition(positionId);
            return ResponseEntity.ok("Xóa chức vụ thành công sạch sẽ!");
        } catch (RuntimeException e) {
            // Trả về thông điệp lỗi chặn xóa từ Service (vướng lính) để FE hiển thị Toast
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}