package com.example.petcare_management.dto;

import com.example.petcare_management.entity.enums.Gender; // Import cái Enum chung vào đây ní ơi
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffRequest {
    private String fullName;
    private Gender gender; // 🌟 THÊM EM NÓ VÀO ĐÂY NHA NI
    private String email;
    private String cccd;
    private String phone;
    private String avatar;
    private String address;
    private Integer positionId;
    private BigDecimal salary;
    private BigDecimal bonus;
    private String username;
    private String password;
}