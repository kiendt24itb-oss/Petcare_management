package com.example.petcare_management.service;

import com.example.petcare_management.dto.StaffLogResponse;
import java.util.List;

public interface StaffLogService {
    // Hàm ghi log nhân viên
    void addLog(Integer staffId, String text);

    // Hàm lấy log của ngày hôm nay
    List<StaffLogResponse> getTodayLogs();
}