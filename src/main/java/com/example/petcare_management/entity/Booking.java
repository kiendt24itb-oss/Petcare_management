package com.example.petcare_management.entity;

import com.example.petcare_management.entity.enums.BookingStatus;
import com.example.petcare_management.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    private Integer bookingId;

    @Column(name = "booking_code", nullable = false, unique = true, length = 10)
    private String bookingCode; // Mã tự sinh thông minh (BK-A001, BK-A002...)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "total_price", nullable = false)
    private BigDecimal totalPrice;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Column(name = "booking_time", nullable = false)
    private LocalTime bookingTime;

    @Column(name = "actual_start_time")
    private LocalDateTime actualStartTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private PaymentStatus paymentStatus;

    @Column(name = "payment_method", length = 50)
    @Builder.Default
    private String paymentMethod = "CASH";

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private BookingStatus status; // Đảm bảo BookingStatus có: PENDING_APPROVAL, WAITING, PROCESSING, COMPLETED, CANCELLED, REJECTED

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<BookingDetail> bookingDetails = new ArrayList<>();

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<BookingLog> bookingLogs = new ArrayList<>();
}