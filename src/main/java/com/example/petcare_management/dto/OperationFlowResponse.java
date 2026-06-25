package com.example.petcare_management.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperationFlowResponse {
    private String bookingCode;      // Mã đơn (Ví dụ: BK001)
    private String petAndOwnerName;  // Tên Thú cưng / Chủ (Ví dụ: "Kiki / Nguyễn Văn A")
    private String serviceName;      // Dịch vụ chỉ định (Ví dụ: Khám tổng quát)
    private String status;           // Trạng thái (Ví dụ: WAITING, PROCESSING)
    private String staffInCharge;    // Nhân viên phụ trách
}