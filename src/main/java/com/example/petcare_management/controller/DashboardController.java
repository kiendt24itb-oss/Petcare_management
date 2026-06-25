package com.example.petcare_management.controller;

import com.example.petcare_management.dto.DashboardResponse;
import com.example.petcare_management.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Thông quan CORS cho Front-End gọi thoải mái
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 📊 [GET] /api/v1/dashboard/operation-summary
     * Lấy toàn bộ dữ liệu thống kê, luồng điều phối và nhật ký vận hành hỗn hợp trong ngày hôm nay từ Service
     */
    @GetMapping("/operation-summary")
    public ResponseEntity<DashboardResponse> getOperationSummary() {
        // Gọi thẳng Service lo liệu từ A-Z (bao gồm cả mảng operationLogs đã được xử lý)
        DashboardResponse response = dashboardService.getDashboardData();
        return ResponseEntity.ok(response);
    }
}