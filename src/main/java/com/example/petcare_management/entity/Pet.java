package com.example.petcare_management.entity;

import com.example.petcare_management.entity.enums.Gender;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "pets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pet_id")
    private Integer petId;

    @Column(name = "pet_code", nullable = false, unique = true, length = 10)
    private String petCode; // Mã tự sinh (TC-A001...)

    // Nhiều thú cưng thuộc về 1 Khách hàng
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "pet_name", nullable = false, length = 250)
    private String petName;

    @Column(name = "species", length = 100)
    private String species; // Loài (Dog, Cat...)

    @Column(name = "breed", length = 100)
    private String breed; // Giống cụ thể

    @Column(name = "age")
    private Integer age;

    @Column(name = "weight")
    private Double weight;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    @Column(name = "avatar", length = 500)
    private String avatar;

    @Column(name = "health_note", columnDefinition = "TEXT")
    private String healthNote;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}