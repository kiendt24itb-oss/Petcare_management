package com.example.petcare_management.dto;

import java.util.List;
import lombok.Data;

@Data
public class CustomerProfileResponse {
    private String fullName;
    private String customerCode;
    private List<PetDTO> pets;

    @Data
    public static class PetDTO {
        private Integer petId;    // 🌟 CHỖ NÀY: Sửa từ Long thành Integer cho khớp với Entity Pet
        private String petName;
        private String species;
    }
}