package com.example.petcare_management.dto;

import com.example.petcare_management.entity.enums.Gender;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PetRequest {
    private String petName;
    private String species;
    private String breed;
    private Integer age;
    private Double weight;
    private Gender gender;
    private String avatar;
    private String healthNote;
}