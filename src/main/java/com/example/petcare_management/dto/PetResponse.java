package com.example.petcare_management.dto;

import com.example.petcare_management.entity.enums.Gender;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PetResponse {
    private Integer petId;
    private String petCode;
    private Integer customerId; // 🌟 THÊM TRƯỜNG NÀY: Để Front-End map-matching chính xác chủ sở hữu
    private String petName;
    private String species;
    private String breed;
    private Integer age;
    private Double weight;
    private Gender gender;
    private String avatar;
    private String healthNote;
    private LocalDateTime createdAt;
}