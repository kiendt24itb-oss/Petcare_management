package com.example.petcare_management.dto;

import com.example.petcare_management.entity.enums.Gender;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerRequest {
    private String fullName;
    private Integer age;
    private Gender gender;
    private String email; // 🌟 Chỉ cần 1 trường này, tầng Service sẽ tự set mail này cho cả Account và Customer
    private String phone;
    private String address;
    private String avatar;
}