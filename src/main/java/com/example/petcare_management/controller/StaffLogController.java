package com.example.petcare_management.controller;

import com.example.petcare_management.dto.StaffLogResponse;
import com.example.petcare_management.service.StaffLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
public class StaffLogController {

    @Autowired
    private StaffLogService staffLogService;

    /**
     * API LẤY DANH SÁCH NHẬT KÝ HOẠT ĐỘNG CỦA NHÂN VIÊN TRONG NGÀY HÔM NAY
     * URL: GET http://localhost:8080/api/v1/dashboard/staff-logs-today
     * Quyền truy cập: ADMIN, STAFF (Theo cấu hình SecurityConfig)
     */
    @GetMapping("/staff-logs-today")
    public ResponseEntity<List<StaffLogResponse>> getTodayStaffLogs() {
        List<StaffLogResponse> logs = staffLogService.getTodayLogs();
        return ResponseEntity.ok(logs);
    }
}