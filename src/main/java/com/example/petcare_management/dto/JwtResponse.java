package com.example.petcare_management.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JwtResponse {
    private String token;

    @Builder.Default
    private String type = "Bearer";

    private Integer accountId;
    private String username;
    private String email;
    private String role;
}