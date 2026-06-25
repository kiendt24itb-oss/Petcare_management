package com.example.petcare_management.entity;

import com.example.petcare_management.entity.enums.Gender;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "account") // Loại trừ account để tránh lỗi StackOverflowError khi gọi toString()
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Integer customerId;

    @Column(name = "customer_code", nullable = false, unique = true, length = 10)
    private String customerCode; // Logic sinh mã dạng "KH-0001" xử lý ở tầng Service chuẩn luôn ní!

    // Mối quan hệ 1-1 với Account
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY) // Thêm FetchType.LAZY để tối ưu hiệu năng
    @JoinColumn(name = "account_id", referencedColumnName = "account_id", nullable = false, unique = true)
    private Account account;

    @Column(name = "full_name", nullable = false, length = 250)
    private String fullName;

    @Column(name = "age")
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    @Column(name = "email", length = 250, unique = true)
    private String email;

    @Column(name = "phone", length = 15)
    private String phone;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "avatar", length = 500)
    private String avatar;

    @Column(name = "violation_count")
    @Builder.Default
    private Integer violationCount = 0; // Số lần bom lịch, mặc định bằng 0

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}