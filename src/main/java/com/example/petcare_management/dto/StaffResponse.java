package com.example.petcare_management.dto;

import com.example.petcare_management.entity.enums.Gender;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor // 🌟 Thằng này sẽ tự tạo Constructor theo đúng thứ tự từ trên xuống dưới này
@Builder
public class StaffResponse {
    // 1. Nhóm 8 trường đầu tiên của Staff (Bỏ cccd ra)
    private Integer staffId;
    private String staffCode;
    private String fullName;
    private Gender gender;
    private String email;
    private String phone;
    private String avatar;
    private String address;

    // 2. Nhóm các trường liên kết từ bảng Position, Salary và Account
    private String positionName;
    private BigDecimal salary;
    private BigDecimal bonus;
    private BigDecimal totalSalary;
    private String username;
    private LocalDateTime hireDate;

    // 3. Nhóm 2 trường mới ném xuống cuối cùng cho khớp khít câu Query
    private String cccd;
    private Integer positionId;
}