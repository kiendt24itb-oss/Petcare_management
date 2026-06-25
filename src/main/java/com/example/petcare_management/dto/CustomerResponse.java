package com.example.petcare_management.dto;

import com.example.petcare_management.entity.enums.AccountStatus;
import com.example.petcare_management.entity.enums.Gender;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponse {
    private Integer customerId;
    private String customerCode;
    private String username;
    private String fullName;
    private Integer age;
    private Gender gender;
    private String email;
    private String phone;
    private String address;
    private String avatar;
    private Integer violationCount;
    private AccountStatus accountStatus; // 🌟 THÊM MỚI: Lấy status từ Account sang (ACTIVE / LOCKED)
    private LocalDateTime createdAt;

    // 🌟 THÊM MỚI: Hàm trả về chế độ duyệt cho Front-End bốc xài ăn tiền ngay
    public String getApprovalMode() {
        if (this.accountStatus == AccountStatus.LOCKED) {
            return "LOCKED"; // Tài khoản bị khóa
        }
        return (this.violationCount != null && this.violationCount >= 3) ? "MANUAL" : "AUTO"; // Quá 3 lần hủy lịch thì duyệt tay, ngược lại tự động duyệt
    }
}