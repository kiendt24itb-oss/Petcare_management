package com.example.petcare_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Overview {
    private BigDecimal currentMonthRevenue; // Doanh thu tháng này
    private long todayBookingsCount;        // 🌟 THÊM DÒNG NÀY: Số đơn đặt lịch hôm nay

    // Thống kê 6 tháng gần nhất phục vụ vẽ Chart
    private List<MonthlyRevenueDTO> sixMonthsRevenue;

    @Data
    @AllArgsConstructor
    public static class MonthlyRevenueDTO {
        private String label;
        private BigDecimal revenue;
    }
}