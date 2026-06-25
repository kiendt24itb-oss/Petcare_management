package com.example.petcare_management.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {

    // 1. Bốn ô thống kê phía trên cùng
    private long totalTicketsToday;       // Tổng ca tiếp nhận (Hôm nay)
    private long waitingCount;            // Đang Chờ Khám / Spa
    private long processingCount;         // Đang Thực Hiện (Processing)
    private double hotelCapacityRate;     // Công suất Phòng Hotel (%)
    private long availableRooms;          // Sức chứa hiện tại (Số phòng trống)

    // 2. Bảng luồng điều phối ở giữa
    private List<OperationFlowResponse> operationFlows;

    // 3. Nhật ký biến động ở bên phải (Dùng chính DTO bạn vừa viết)
    private List<BookingLogResponse> operationLogs;
}